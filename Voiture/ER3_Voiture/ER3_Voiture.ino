/*
 Name:		ER3_Voiture.ino
 Created:	04/12/2019 13:26:48
 Author:	kelian
*/
#include "Algorithm.h"


Moteur mMoteur;
Bluetooth mBluetooth;

char cParam;
byte byValue1, byValue2;

typedef enum{FALLBACK,RUNNING}TState;

TState State, State_old;

void setup() {
    
    //Debug
    Serial.begin(DEBUG_SPEED);
    Serial.println("READY");
	State = FALLBACK;
	State_old = RUNNING;
	delay(25);
}

void loop() {
	//Input Refresh
	bool bReceived = mBluetooth.Receive(cParam, byValue1, byValue2);
	bool bRepli = mBluetooth.Alive();


	//State+Output Update
	switch (State) {
	case FALLBACK : 
		if (bRepli) {
			State = RUNNING;
			Serial.println("RUN");
		}
		break;

	case RUNNING:
		if (bRepli==0) {
			State = FALLBACK;
			Serial.println("BACK");
		}
		if (bReceived) {
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
		}
		break;
	}	
	if (State != State_old) {
		State_old = State;
		switch (State)
		{
		case FALLBACK:
			mMoteur.Right(FORWARD, 0);
			mMoteur.Left(FORWARD, 0);
			break;
		}
	}

}

