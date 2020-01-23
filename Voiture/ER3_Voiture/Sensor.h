#pragma once

#define IR_RIGHT	3
#define IR_MIDDLE	4
#define IR_LEFT		2

#define IR_ADC_L	5
#define IR_ADC_M	1
#define IR_ADC_R	4

#define LED_TOP   13


class Sensor
{

public:
	Sensor();

	int Read(int iADC);
};
