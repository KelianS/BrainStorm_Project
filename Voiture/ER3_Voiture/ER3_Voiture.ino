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

void setup() {
    
    //Debug
    Serial.begin(DEBUG_SPEED);
    Serial.println("READY");

}

void loop() {
	static bool b = false;
	if (mBluetooth.Alive()) {
		if (b == true) {
			Serial.println("Connected");
			b = false;
		}
		
		if (mBluetooth.Receive(cParam, byValue1, byValue2)) {//return 1 if comms is finished

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
	}
	else {
		if (b == false) {
			Serial.println("Disconnected");
			b = true;
		}
		mMoteur.Right(FORWARD, 0);
		mMoteur.Left(FORWARD,0);
	}
}

