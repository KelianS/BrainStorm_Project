#pragma once
#include <Arduino.h>
#include "Moteur.h"
#include "Bluetooth.h"
#include "Sensor.h"

#define DEBUG_SPEED 115200


class Algorithm
{
public: 
	Moteur mMoteur;
	Bluetooth mBluetooth;
	Sensor mSensor;

	typedef enum { FALLBACK, RUNNING, RECEIVING, COLLISION, RECEIVING_IN_COLLISION, AUTONOMOUS, RECEIVING_IN_AUTO }TState;
	typedef enum { LEFT, RIGHT, MID, TWOSENS_L, TWOSENS_R, THREESENS, NO_SENS }TStAuto;
	TState State, State_old;
	TStAuto StAuto, StAuto_old;

	//BT
	bool bReceived;
	bool bReply;
	char cParam;
	byte byValue1, byValue2;

	//infrared Sensor
	bool bIR_Left;
	bool bIR_Mid;
	bool bIR_Right;

	//Auto Mode
	unsigned long ulRefreshAuto;
	unsigned long ulLastRefreshAuto;

	Algorithm();


	void Input_Refresh();
	void State_Update();
	void Output_Update();
	void Async_Auto();
	String SensorUpdate();

};

