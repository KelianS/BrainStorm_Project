/*
 Name:		ER3_Voiture.ino
 Created:	04/12/2019 13:26:48
 Author:	kelian
*/
#include "Algorithm.h"


#define IR_RIGHT  3
#define IR_MIDDLE 4
#define IR_LEFT   2

Moteur mMoteur;
Bluetooth mBluetooth;

char cParam;
byte byValue1, byValue2;

typedef enum{FALLBACK,RUNNING,RECEIVING,COLLISION,RECEIVING_IN_COLLISION}TState;

TState State, State_old;

void setup() {
    
    //Debug
    Serial.begin(DEBUG_SPEED);
    Serial.println("READY");
	State = FALLBACK;
	State_old = RUNNING;
	delay(25);


	pinMode(IR_LEFT, INPUT);
	pinMode(IR_MIDDLE, INPUT);
	pinMode(IR_RIGHT, INPUT);
}

void loop() {
	//Input Refresh
	bool bReceived = mBluetooth.Receive(cParam, byValue1, byValue2);
	bool bReply = mBluetooth.Alive();


	//State+Output Update
	switch (State) {
	case FALLBACK : 
		if (bReply) {
			State = RUNNING;
			Serial.println("connected");
		}
		break;

	case RUNNING:
		if (!bReply) {
			State = FALLBACK;
		}
		if (bReceived) {
			State = RECEIVING;
		}
		if ((analogRead(IR_LEFT) < 30) || (analogRead(IR_MIDDLE) < 30) || (analogRead(IR_RIGHT) < 30)) {
			State = COLLISION;
			mMoteur.Right(FORWARD, 0);
			mMoteur.Left(FORWARD, 0);
			Serial.println("Collision");
		}
		break;

	case COLLISION:
		if (!bReply) {
			State = FALLBACK;
		}
		if ((analogRead(IR_LEFT) > 30) && (analogRead(IR_MIDDLE) > 30) && (analogRead(IR_RIGHT) > 30)) {
			State = RUNNING;
		}
		if (bReceived) {
			State = RECEIVING_IN_COLLISION;
		}
		break;

	case RECEIVING_IN_COLLISION :
		State = COLLISION;
		break;

	case RECEIVING:
		State = RUNNING;
		break;
	}	

	if (State != State_old) {
		State_old = State;
		switch (State)
		{
		case FALLBACK:
			mMoteur.Right(FORWARD, 0);
			mMoteur.Left(FORWARD, 0);
			Serial.println("disconnected");
			break;
		case RECEIVING:
			switch (cParam) {
			case 'A': //Both Motors -> Forward
				mMoteur.Right(FORWARD, byValue1);
				mMoteur.Left(FORWARD, byValue2);
				break;
			case 'B':  //Both Motors -> Reverse
				mMoteur.Right(REVERSE, byValue1);
				mMoteur.Left(REVERSE, byValue2);
				break;
			case 'C':  //EXTRA :
				mMoteur.Right(FORWARD, byValue1); // RIGHT MOTOR -> Forward
				mMoteur.Left(REVERSE, byValue2); // LEFT -> Reverse
				break;
			case 'D':  //EXTRA
				mMoteur.Right(REVERSE, byValue1); // RIGHT -> Reverse 
				mMoteur.Left(FORWARD, byValue2); // LEFT MOTOR -> Forward
				break;
			}
		break;

		case RECEIVING_IN_COLLISION :
			//impossible to go Forward
			switch (cParam) {
			case 'A':  //Both Motors -> Forward
				break;
			case 'B':  //Both Motors -> Reverse
				mMoteur.Right(REVERSE, byValue1);
				mMoteur.Left(REVERSE, byValue2);
				break;
			case 'C':  //EXTRA :
				mMoteur.Left(REVERSE, byValue2); // LEFT -> Reverse
				break;
			case 'D':  //EXTRA
				mMoteur.Right(REVERSE, byValue1); // RIGHT -> Reverse 
				break;
			}
			break;
		}

	}

}

