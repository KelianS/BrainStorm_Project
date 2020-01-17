#pragma once

#define BT_SPEED 57600
#define ANSWER_TIME 150


class Bluetooth
{
public :

	bool bHasAnswer;
	String stBuffer;
	unsigned long ulLastReceived = 0;

	Bluetooth();

	bool Receive(char& cParam, byte& byVal, byte& byVal2);

	bool Alive(void);

};

