/****************************
Author : Kelian SERMET
Last Modif : 17/01/2020

Bluetooth frame:
<char Param><char val1.1><char val1.2><char val1.1><char val1.3><char val2.1><char val2.2><char val2.3><" ">
Ex : 'A255255 ' 

Param definition :
'A' => Val1 = Motor1 Value / Val2 = Motor2 Value //FORWARD
'B' => Val1 = Motor1 Value / Val2 = Motor2 Value //REVERSE
'C' => Val1 = Motor1 Value FORWARD / Val2 = Motor2 Value REVERSE
'D' => Val1 = Motor1 Value REVERSE / Val2 = Motor2 Value FORWARD
'E' => ZEN MODE : 'E255255 '=activation  | 'E000000 '=desactivation
'F' => SENSOR debug : - <F><bool Sensor1><Sensor2><Sensor3><Sensor4><Sensor5><Sensor6><' '>		
					  - Sensor definition : '1' = IR_LEFT
											'2' = IR_MID
											'3' = IR_RIGHT
					  - EX : 'F111000 ' := SENSOR LEFT/MID/RIGHT Activated
'Y' => Alive bit sended from the car to know if the app is always connected. ex: 'Y' (no data sent here)
*****************************/

#include <Arduino.h>
#include "Bluetooth.h"


Bluetooth::Bluetooth() {
	Serial1.begin(BT_SPEED);
	stBuffer = "";
}


bool Bluetooth::Receive(char& cParam, byte& byVal1, byte& byVal2) {
	bool bRet = false;

	if(Serial1.available() > 0)
	{
		ulLastReceived = millis();
		bHasAnswer = true;

		String stValue = "        ";
		stValue = Serial1.readStringUntil(' '); //End comms with a space
		cParam = stValue.substring(0, 1).charAt(0);
		byVal1 = stValue.substring(1, 4).toInt();
		byVal2 = stValue.substring(4).toInt();

		//Debug 
		//Serial.println("> " + stValue);
		//Serial.print(cParam); Serial.print("-");  Serial.print(byVal1); Serial.print("-"); Serial.println(byVal2);
		if (cParam != 'Z') {
			bRet = true;
		}
	}

	return bRet;

}


bool Bluetooth::Alive(void) {
	/*********************************************************************************
	Send a 'Y' every $ANSWER_TIME (in millisecond), return true if the phone answer with a "Z " in the next $ANSWER_TIME.
	**********************************************************************************/
	bool bRetAlive = true;
	static unsigned long ulTimeOld = millis();

	//Send 'Y'
	if ((millis()-ulTimeOld) > ANSWER_TIME) {
		if (stBuffer!="") {//We have something to send (like the sensor info)
			//Serial.print(stBuffer);
			Serial1.print(stBuffer);
		}
		else {//send alive bit if nothing else to send
			Serial1.print("Y");
		}
		ulTimeOld = millis();
	}

	//If last reception occurs at more than 'ANSWER_TIME' = disconnected
	if ((millis()-ulLastReceived) > (ANSWER_TIME*2)) {
		//Serial.println(millis() - (ulLastReceived));
		bHasAnswer = false;
	}

	//Return 0 if mobile hasn't answered
	if (bHasAnswer == false) { 
		bRetAlive = false;
	}

	return bRetAlive;
}