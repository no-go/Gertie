#include <TFT.h>
#include <SPI.h>
#include <MsTimer2.h>

// JOKE LED LASER
#define LEDLASER  A3

// motor
#define LEFT1  2
#define LEFT2  3
#define RIGHT1 4
#define RIGHT2 5

#define SERIAL_SPEED  9600

#define BLACK           0x0000
#define GREY            0b0010000100000100
#define GREYBLUE        0b0010000100010000
#define YELLOW          0b0000011111111111
#define WHITE           0xFFFF

// create an instance of the library (cs,dc,reset)
TFT TFTscreen = TFT(A0, A2, A1);

#include <Wtv020sd16p.h>
int resetPin = 6;  // The pin number of the reset RST
int busyPin  = 7;  // The pin number of the  busy P06
int clockPin = 9;  // The pin number of the clock P04
int dataPin  = 8;  // The pin number of the  data P05
// P02 NEXT       k1
// P03 PREV       k2
// P07 play/stop  k3
Wtv020sd16p wtv020sd16p(resetPin,clockPin,dataPin,busyPin);

enum Sounds {
  // for a speaking clock
  ZERO,
  EIN,ZWEI,DREI,VIER,FUENF,SECHS,SIEBEN,ACHT,NEUN,ZEHN,
  ELEFEN,TWELFE,
  ZWANZIG,DREIZIG,VIERZIG,FUENFZIG,
  OCLOCK,UND,IT_IS,EINS,
  // robot movie voices
  ROBOTSOUND1,
  ROBOTSOUND2,
  ROBOTSOUND3,
  ROBOTSOUND4,
  ROBOTSOUND5,
  ROBOTSOUND6,
  ROBOTSOUND7,
  ROBOTSOUND8,
  ROBOTSOUND9
};

int wasOn=-1;
int laserTimer=-1;

inline void ticking() {
  if (wasOn >= 0) wasOn++;
  if (laserTimer >= 0) laserTimer++;
}

void clearM() {
  TFTscreen.fillRect(42, 36, 50, 60, YELLOW);
}

void blinky() {
  TFTscreen.fillCircle(100, 40,  6, YELLOW);
  TFTscreen.fillCircle(100, 84,  6, YELLOW);
  TFTscreen.fillRect(  100, 40, 3, 5, BLACK);
  TFTscreen.fillRect(  100, 84, 3, 5, BLACK);
  delay(80);
  TFTscreen.fillCircle(100, 40,  6, BLACK);
  TFTscreen.fillCircle(100, 84,  6, BLACK);
}

void doblink() {
  TFTscreen.fillCircle(100, 40,  6, YELLOW);
  clearM();
  TFTscreen.fillRect(  100, 40, 3, 5, BLACK);
  TFTscreen.stroke(0, 0, 0);
  TFTscreen.text("(", 42, 36);
}
void good() {
  clearM();
  TFTscreen.stroke(0, 0, 0);
  TFTscreen.text("(", 42, 36);
}
void bad() {
  clearM();
  TFTscreen.stroke(0, 0, 0);
  TFTscreen.text(")", 42, 36);
}
void ohh() {
  clearM();
  TFTscreen.stroke(0, 0, 0);
  TFTscreen.text("o", 42, 36);
}
void wrong() {
  clearM();
  TFTscreen.stroke(0, 0, 0);
  TFTscreen.text("E", 42, 36);
}
void hmm() {
  clearM();
  TFTscreen.stroke(0, 0, 0);
  TFTscreen.fillRect(42, 36, 5, 10, BLACK);
  TFTscreen.fillRect(45, 44, 5, 40, BLACK);
}
void unsure() {
  clearM();
  TFTscreen.stroke(0, 0, 0);
  TFTscreen.fillRect(42, 36, 5, 40, BLACK);
}
void renew() {
  TFTscreen.background(0, 0, 0);
  TFTscreen.fillCircle( 80, 62, 62, YELLOW);
  TFTscreen.fillCircle(100, 40,  6, BLACK);
  TFTscreen.fillCircle(100, 84,  6, BLACK);
}
void silent() {
  clearM();
  TFTscreen.stroke(0, 0, 0);
  TFTscreen.text("x", 42, 32);
}

inline void laser() {
  if (laserTimer < 30) return;
  
  if (laserTimer < 50) {
    digitalWrite(LEDLASER, laserTimer%4==0? LOW:HIGH);      
  } else if (laserTimer < 90) {
    digitalWrite(LEDLASER, laserTimer%2==0? HIGH:LOW);
  } else {
    digitalWrite(LEDLASER, HIGH);      
  }
}

void setup() {
  Serial.begin(SERIAL_SPEED);
  pinMode(LEDLASER, OUTPUT);
  pinMode(LEFT1,    OUTPUT);
  pinMode(LEFT2,    OUTPUT);
  pinMode(RIGHT1,   OUTPUT);
  pinMode(RIGHT2,   OUTPUT);
  randomSeed(analogRead(A7));
  
  TFTscreen.begin();
  TFTscreen.setTextSize(7);
  renew();
  good();
  wtv020sd16p.reset();
  delay(300);
  wtv020sd16p.asyncPlayVoice(ROBOTSOUND9);
  
  MsTimer2::set(100, ticking);
  MsTimer2::start();
}

void loop() {
  if (random(30000) == 10) blinky();
  laser();
  
  if (Serial.available()) {
    char inChar = (char) Serial.read();
    
    if (inChar == ')') {
      good();
    } else if (inChar == '(') {
      bad();
    } else if (inChar == ';') {
      doblink();
    } else if (inChar == 'o') {
      ohh();
    } else if (inChar == 'E') {
      wrong();
    } else if (inChar == '/') {
      hmm();
    } else if (inChar == 'i') {
      unsure();
    } else if (inChar == 'x') {
      silent();
    } else if (inChar == '0') {
      wtv020sd16p.asyncPlayVoice(ZERO);
    } else if (inChar == '1') {
      wtv020sd16p.asyncPlayVoice(ROBOTSOUND1);
    } else if (inChar == '2') {
      wtv020sd16p.asyncPlayVoice(ROBOTSOUND2);
    } else if (inChar == '3') {
      wtv020sd16p.asyncPlayVoice(ROBOTSOUND3);
      // laser on
      laserTimer=0;
      ohh();
    } else if (inChar == '4') {
      wtv020sd16p.asyncPlayVoice(ROBOTSOUND4);
    } else if (inChar == '5') {
      wtv020sd16p.asyncPlayVoice(ROBOTSOUND5);
    } else if (inChar == '6') {
      wtv020sd16p.asyncPlayVoice(ROBOTSOUND6);
    } else if (inChar == '7') {
      wtv020sd16p.asyncPlayVoice(ROBOTSOUND7);
    } else if (inChar == '8') {
      wtv020sd16p.asyncPlayVoice(ROBOTSOUND8);
    } else if (inChar == '9') {
      wtv020sd16p.asyncPlayVoice(ROBOTSOUND9);
    } else if (inChar == 'l') {
      digitalWrite(LEFT1,  LOW);
      digitalWrite(LEFT2,  HIGH);
      digitalWrite(RIGHT1, HIGH);
      digitalWrite(RIGHT2, LOW);
      wasOn=0;
    } else if (inChar == 'r') {
      digitalWrite(LEFT1,  HIGH);
      digitalWrite(LEFT2,  LOW);
      digitalWrite(RIGHT1, LOW);
      digitalWrite(RIGHT2, HIGH);
      wasOn=0;
    } else if (inChar == 'b') {
      digitalWrite(LEFT1,  LOW);
      digitalWrite(LEFT2,  HIGH);
      digitalWrite(RIGHT1, LOW);
      digitalWrite(RIGHT2, HIGH);
      wasOn=0;
    } else if (inChar == 'f') {
      digitalWrite(LEFT1,  HIGH);
      digitalWrite(LEFT2,  LOW);
      digitalWrite(RIGHT1, HIGH);
      digitalWrite(RIGHT2, LOW);
      wasOn=0;
    }
  }

  if (wasOn > 7) {
    wasOn=-1;
    digitalWrite(LEFT1,  LOW);
    digitalWrite(LEFT2,  LOW);
    digitalWrite(RIGHT1, LOW);
    digitalWrite(RIGHT2, LOW);
  }
  
  if (laserTimer > 150) {
    laserTimer=-1;
    good();
    digitalWrite(LEDLASER, LOW);
  }
}
