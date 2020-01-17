#include <Arduino.h>
#include "Sensor.h"

Sensor::Sensor() {
	pinMode(IR_LEFT, INPUT);
	pinMode(IR_MIDDLE, INPUT);
	pinMode(IR_RIGHT, INPUT);
	pinMode(LED_TOP, OUTPUT);
}

