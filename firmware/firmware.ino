#include <TFT.h>
#include <SPI.h>
#include <MsTimer2.h>

#define LED   13
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

int wasOn=-1;

inline void ticking() {
  if (wasOn >= 0) wasOn++;
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

void setup() {
  Serial.begin(SERIAL_SPEED);
  pinMode(LEFT1, OUTPUT);
  pinMode(LEFT2, OUTPUT);
  pinMode(RIGHT1, OUTPUT);
  pinMode(RIGHT2, OUTPUT);
  randomSeed(analogRead(A7));
  
  TFTscreen.begin();
  TFTscreen.setTextSize(7);
  renew();
  good();
  
  MsTimer2::set(100, ticking);
  MsTimer2::start();
}

void loop() {
  if (random(30000) == 10) blinky();
  
  if (Serial.available()) {
    char inChar = (char) Serial.read();
    if (inChar == '0') {
      digitalWrite(LED, LOW);      
    }
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
    }

    if (inChar == '1') {
      digitalWrite(LED, HIGH);      
    }
    if (inChar == 'l') {
      digitalWrite(LEFT1,  LOW);
      digitalWrite(LEFT2,  HIGH);
      digitalWrite(RIGHT1, HIGH);
      digitalWrite(RIGHT2, LOW);
      wasOn=0;
    }
    if (inChar == 'r') {
      digitalWrite(LEFT1,  HIGH);
      digitalWrite(LEFT2,  LOW);
      digitalWrite(RIGHT1, LOW);
      digitalWrite(RIGHT2, HIGH);
      wasOn=0;
    }
    if (inChar == 'b') {
      digitalWrite(LEFT1,  LOW);
      digitalWrite(LEFT2,  HIGH);
      digitalWrite(RIGHT1, LOW);
      digitalWrite(RIGHT2, HIGH);
      wasOn=0;
    }
    if (inChar == 'f') {
      digitalWrite(LEFT1,  HIGH);
      digitalWrite(LEFT2,  LOW);
      digitalWrite(RIGHT1, HIGH);
      digitalWrite(RIGHT2, LOW);
      wasOn=0;
    }
  }

  if (wasOn > 10) {
    wasOn=-1;
    digitalWrite(LEFT1,  LOW);
    digitalWrite(LEFT2,  LOW);
    digitalWrite(RIGHT1, LOW);
    digitalWrite(RIGHT2, LOW);
  }
}
