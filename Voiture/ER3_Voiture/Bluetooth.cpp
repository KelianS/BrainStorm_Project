#include <Arduino.h>
#include "Bluetooth.h"


Bluetooth::Bluetooth() {
	Serial1.begin(BT_SPEED);
}


bool Bluetooth::Receive(char& cParam, byte& byVal1, byte& byVal2) {
	bool bRet = false;

	if(Serial1.available() > 0)
	{
			String stValue = Serial1.readStringUntil(' '); //End comms with a space

			if (stValue.compareTo("Z")==0) { //If we received 'Z ' -> It's the Alive bit
				bHasAnswer = true;
				ulLastReceived = millis();
				//Debug
				Serial.println("<-->");

			}
			else {
				cParam = stValue.substring(0, 1).charAt(0);
				byVal1 = stValue.substring(1, 4).toInt();
				byVal2 = stValue.substring(4).toInt();

				//Debug 
				Serial.println("> " + stValue);
				Serial.print(cParam); Serial.print("-");  Serial.print(byVal1); Serial.print("-"); Serial.println(byVal2);

				bRet = true;
			}
	}

	return bRet;

}


bool Bluetooth::Alive(void) {
	/*********************************************************************************
	Send a 'Y' every 100ms, return true if the phone answer with a "Z " in the next 20ms.
	**********************************************************************************/
	bool bRetAlive = true;
	static unsigned long ulTimeOld = millis();

	//Send 'Y'
	if (millis() > (ulTimeOld + ANSWER_TIME)) {
		Serial1.print('A');
		Serial.println("__");
		ulTimeOld = millis();
	}
	if (millis() > ulLastReceived + (ANSWER_TIME*(3./2))) {//Not connected
		bHasAnswer = false;
	}

	//Return 0 if mobile hasn't answered
	if (bHasAnswer == false) { 
		bRetAlive = false;
	}

	return bRetAlive;
}