#pragma once

#define BT_SPEED 57600
#define ANSWER_TIME 100

class Bluetooth
{
private : 
	bool bHasAnswer;
	unsigned long ulLastReceived = 0;

public :

	Bluetooth();

	bool Receive(char& cParam, byte& byVal, byte& byVal2);

	bool Alive(void);

};

