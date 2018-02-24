/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 * 
 * 2016 - modified many parts by Jochen Peters
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package click.dummer.Gertie;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import java.util.UUID;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */
public class UartService extends Service {

    private final static String TAG = UartService.class.getSimpleName();
    private byte[] outputBytes;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private SharedPreferences mPrefs;

    public final static String ACTION_GATT_CONNECTED =
            "click.dummer.UartNotify.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "click.dummer.UartNotify.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "click.dummer.UartNotify.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "click.dummer.UartNotify.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "click.dummer.UartNotify.EXTRA_DATA";
    public final static String DEVICE_DOES_NOT_SUPPORT_UART =
            "click.dummer.UartNotify.DEVICE_DOES_NOT_SUPPORT_UART";

    public UUID            CCCD;
    public UUID RX_SERVICE_UUID;
    public UUID    RX_CHAR_UUID;
    public UUID    TX_CHAR_UUID;

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                broadcastUpdate(intentAction);
                showMessage("Connected to GATT server.");
                // Attempts to discover services after successful connection.
                showMessage("Attempting to start service discovery: " + mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                showMessage("Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                showMessage("mBluetoothGatt = " + mBluetoothGatt );
            	
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                showMessage("onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);

        // This is handling for the notification on TX Character of NUS service
        if (TX_CHAR_UUID.equals(characteristic.getUuid())) {
            intent.putExtra(EXTRA_DATA, characteristic.getValue());
        } else {
        	
        }
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    public class LocalBinder extends Binder {
        UartService getService() {
            return UartService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int deviceType = Integer.parseInt(mPrefs.getString("device_type", "0"));

        switch (deviceType) {
            case 0:
                // Bluefruit
                CCCD            = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
                RX_SERVICE_UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e");
                RX_CHAR_UUID    = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e");
                TX_CHAR_UUID    = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e");
                break;
            case 1:
                // HM10 (CC2541)
                CCCD            = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
                RX_SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
                RX_CHAR_UUID    = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
                TX_CHAR_UUID    = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
                break;
            case 2:
                // nRF51822-04AT
                CCCD            = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
                RX_SERVICE_UUID = UUID.fromString("00001234-0000-1000-8000-00805f9b34fb");
                RX_CHAR_UUID    = UUID.fromString("00001235-0000-1000-8000-00805f9b34fb");
                TX_CHAR_UUID    = UUID.fromString("00001236-0000-1000-8000-00805f9b34fb");
                break;
            default:
                CCCD            = UUID.fromString(
                        mPrefs.getString("ble_cccd",   "00002902-0000-1000-8000-00805f9b34fb"));
                RX_SERVICE_UUID = UUID.fromString(
                        mPrefs.getString("ble_srv",    "6e400001-b5a3-f393-e0a9-e50e24dcca9e"));
                RX_CHAR_UUID    = UUID.fromString(
                        mPrefs.getString("ble_rxuuid", "6e400002-b5a3-f393-e0a9-e50e24dcca9e"));
                TX_CHAR_UUID    = UUID.fromString(
                        mPrefs.getString("ble_txuuid", "6e400003-b5a3-f393-e0a9-e50e24dcca9e"));
                break;
        }

        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    public boolean initialize() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                showMessage(getString(R.string.noInitBT));
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            showMessage(getString(R.string.getNoBT));
            return false;
        }

        return true;
    }

    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            showMessage(getString(R.string.strangeBTaddr));
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            showMessage(getString(R.string.useExistingBT));
            if (mBluetoothGatt.connect()) {
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            showMessage(getString(R.string.unableConBT));
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);

        showMessage(getString(R.string.tryNewBTcon));
        mBluetoothDeviceAddress = address;
        return true;
    }

    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            showMessage(getString(R.string.BTnotInit));
            return;
        }
        mBluetoothGatt.disconnect();
    }

    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        showMessage(getString(R.string.BTclosed));
        mBluetoothDeviceAddress = null;
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }

    public void enableTXNotification() {
    	BluetoothGattService RxService = mBluetoothGatt.getService(RX_SERVICE_UUID);
    	if (RxService == null) {
            showMessage(getString(R.string.RXmissing));
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }
    	BluetoothGattCharacteristic TxChar = RxService.getCharacteristic(TX_CHAR_UUID);
        if (TxChar == null) {
            showMessage(getString(R.string.TXmissing));
            broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(TxChar,true);
        
        BluetoothGattDescriptor descriptor = TxChar.getDescriptor(CCCD);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor);
    	
    }
    
    public void writeRXCharacteristic(byte[] val) {
        new AsyncTask<byte[], Void, Void>() {
            @Override
            protected Void doInBackground(byte[]... bytes) {
                byte[] value = bytes[0];

                int BYTE_LIMIT = Integer.parseInt(mPrefs.getString("packet_limit", "8"));
                int SPLIT_MS = Integer.parseInt(mPrefs.getString("packet_pause", "60"));

                int packs = (int) Math.ceil(
                        (float) value.length / (float) BYTE_LIMIT
                );
                int finish = BYTE_LIMIT;
                int offset;
                for (int i=0; i<packs; i++) {

                    offset = i * BYTE_LIMIT;
                    if ((offset+BYTE_LIMIT) >= value.length) {
                        finish = value.length - offset;
                    }
                    outputBytes = new byte[finish];
                    System.arraycopy(value, offset, outputBytes, 0, finish);

                    BluetoothGattService RxService = mBluetoothGatt.getService(RX_SERVICE_UUID);
                    Log.d(TAG, "mBluetoothGatt null ("+(i+1)+"/"+packs+") " + mBluetoothGatt);
                    if (RxService == null) {
                        showMessage(getString(R.string.RXmissing));
                        broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
                        return null;
                    }
                    BluetoothGattCharacteristic RxChar = RxService.getCharacteristic(RX_CHAR_UUID);
                    if (RxChar == null) {
                        showMessage(getString(R.string.RXcharaMissing));
                        broadcastUpdate(DEVICE_DOES_NOT_SUPPORT_UART);
                        return null;
                    }
                    RxChar.setValue(outputBytes);

                    mBluetoothGatt.writeCharacteristic(RxChar);

                    if (i == packs-1) showMessage(getString(R.string.all_) + packs + getString(R.string._areSend));
                    try {
                        Thread.sleep(SPLIT_MS);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                return null;
            }
        }.execute(val);
    }

    private void showMessage(String msg) {
        Log.d(TAG, msg);
        if (mPrefs.getBoolean("toasty", false) == true) {
            try {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
            }
        }
    }
}
