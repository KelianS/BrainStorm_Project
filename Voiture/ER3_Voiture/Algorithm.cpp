#include <Arduino.h>
#include "Algorithm.h"


Algorithm::Algorithm() {
	/***************************************************************
	Initialize every component needed for the algorithm of the car
	****************************************************************/
	State = FALLBACK;
	State_old = RUNNING;
	StAuto = NO_SENS;
	StAuto_old = THREESENS;

	ulRefreshAuto = 0;
	ulLastRefreshAuto = 0;
	
	//Debug
	Serial.begin(DEBUG_SPEED);
	Serial.println("Brain_Storm_V2.0");
}

void Algorithm::Input_Refresh() {
	/***************************************************************
	Refresh Inputs for the global State of the car
	****************************************************************/
	bReceived = mBluetooth.Receive(cParam, byValue1, byValue2);
	bReply = mBluetooth.Alive();
	bIR_Left = (analogRead(IR_LEFT) < 50);
	bIR_Mid = (analogRead(IR_MIDDLE) < 50);
	bIR_Right = (analogRead(IR_RIGHT) < 50);
	mBluetooth.stBuffer = SensorUpdate();
}

void Algorithm::State_Update() {
	/***************************************************************
	Update the current global State of the car 
	+Serial_debug : Connected/disconnected/Collision/Autonomous Mode
	****************************************************************/
	switch (State) {
	case FALLBACK:
		if (bReply) {
			State = RUNNING;
			Serial.println("Connected");
			digitalWrite(LED_TOP, 255);
		}
		break;

	case RUNNING:
		if (!bReply) {
			State = FALLBACK;
			Serial.println("disconnected");
			digitalWrite(LED_TOP, 0);
		}
		else if (bIR_Left || bIR_Mid || bIR_Right) {
			State = COLLISION;
			mMoteur.Right(FORWARD, 0);
			mMoteur.Left(FORWARD, 0);
			Serial.println("Collision");
		}
		else if (bReceived) {
			State = RECEIVING;
		}
		else if (cParam == 'E' && byValue1 == 255 && byValue2 == 255) {
			State = AUTONOMOUS;
			Serial.println("Autonomous");
			digitalWrite(LED_TOP, 255);
		}
		break;

	case COLLISION:
		if (!bReply) {
			State = FALLBACK;
			Serial.println("disconnected");
			digitalWrite(LED_TOP, 0);
		}
		else if (!bIR_Left && !bIR_Mid && !bIR_Right) {
			State = RUNNING;
		}
		else if (bReceived) {
			State = RECEIVING_IN_COLLISION;
		}
		break;

	case AUTONOMOUS:
		if (!bReply) {
			State = FALLBACK;
			Serial.println("disconnected");
			digitalWrite(LED_TOP, 0);
		}
		else if (cParam == 'E' && byValue1 == 0 && byValue2 == 0) {
			State = RUNNING;
		}
		break;

	case RECEIVING_IN_AUTO:
		State = AUTONOMOUS;
		break;

	case RECEIVING_IN_COLLISION:
		State = COLLISION;
		break;

	case RECEIVING:
		State = RUNNING;
		break;
	}
}

void Algorithm::Output_Update() {
	/***************************************************************
	Update the Outputs of the car depending on the global state
	****************************************************************/
	if (State != State_old) {
		State_old = State;
		switch (State)
		{
		case FALLBACK:
			mMoteur.Right(FORWARD, 0);
			mMoteur.Left(FORWARD, 0);
			break;
		case RUNNING:
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

		case COLLISION:
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

		case AUTONOMOUS:
			ulRefreshAuto = millis(); //launch auto mode async
			break;
		}

	}

}

void Algorithm::Async_Auto() {
	if (millis() + 25 > ulRefreshAuto) {//refresh OUTPUTS every 25ms
		ulRefreshAuto = millis();

		//Combinatory  / State_Auto update
		if (bIR_Left && bIR_Mid && bIR_Right) {
			StAuto = THREESENS;
			mBluetooth.stBuffer = "F111000 ";
		}
		else if (bIR_Left && bIR_Mid) {
			StAuto = TWOSENS_L;
			mBluetooth.stBuffer = "F110000 ";
		}
		else if (bIR_Mid && bIR_Right) {
			StAuto = TWOSENS_R;
			mBluetooth.stBuffer = "F011000 ";
		}
		else if (bIR_Mid) {
			StAuto = MID;
			mBluetooth.stBuffer = "F010000 ";
		}
		else if (bIR_Left) {
			StAuto = LEFT;
			mBluetooth.stBuffer = "F100000 ";
		}
		else if (bIR_Right) {
			StAuto = RIGHT;
			mBluetooth.stBuffer = "F100000 ";
		}
		else if((ulLastRefreshAuto + 500) < millis()){
			StAuto = NO_SENS;
			mBluetooth.stBuffer = "";
		}

		//Outputs update
		if (StAuto != StAuto_old) {
			StAuto_old = StAuto;
			switch (StAuto) {
			case NO_SENS: //Forward
				mMoteur.Right(FORWARD, 110);
				mMoteur.Left(FORWARD, 110);
				//Serial.print("-");
				break;

			case LEFT:
				mMoteur.Right(REVERSE, 128);
				mMoteur.Left(FORWARD, 50);
				Serial.println("L");
				ulLastRefreshAuto = millis();//take time of the last hit
				break;
			case RIGHT:
				mMoteur.Right(FORWARD, 50);
				mMoteur.Left(REVERSE, 128);
				Serial.println("R");
				ulLastRefreshAuto = millis();
				break;
			case MID:
				mMoteur.Right(REVERSE, 150);
				mMoteur.Left(FORWARD, 80);
				Serial.println("M");
				ulLastRefreshAuto = millis();
				break;
			case TWOSENS_L :
				mMoteur.Right(REVERSE, 150);
				mMoteur.Left(FORWARD, 20);
				ulLastRefreshAuto = millis();
				break;
			case TWOSENS_R:
				mMoteur.Right(FORWARD, 20);
				mMoteur.Left(REVERSE, 150);
				ulLastRefreshAuto = millis();
				break;
			case THREESENS:
				mMoteur.Right(REVERSE, 150);
				mMoteur.Left(REVERSE, 20);
				ulLastRefreshAuto = millis();
				break;
			}
		}
	}
}


String Algorithm::SensorUpdate() {
	String stRet = "";
	//Combinatory  / update sensor info to send over BT
	if (bIR_Left && bIR_Mid && bIR_Right) {
		stRet = "F111000 ";
	}
	else if (bIR_Left && bIR_Mid) {
		stRet = "F110000 ";
	}
	else if (bIR_Mid && bIR_Right) {
		stRet = "F011000 ";
	}
	else if (bIR_Mid) {
		stRet = "F010000 ";
	}
	else if (bIR_Left) {
		stRet = "F100000 ";
	}
	else if (bIR_Right) {
		stRet = "F100000 ";
	}
	return stRet;

}