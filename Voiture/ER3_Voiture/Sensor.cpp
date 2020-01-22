#include <Arduino.h>
#include "Sensor.h"
/************************
IR Sensor :
Left : A2   --> PF5 / ADC5
Mid : A4	--> PF1 / ADC1
Right : A3	--> PF4 / ADC4

LED L port 13 :
D13 --> PC7
************************/

Sensor::Sensor() {
	
	DDRF &= ~((1 << PF1) | (1 << PF4) | (1 << PF5)); //IR Sensor as input
	DDRC |= (1 << PC7);

	//Innitialise les registres pour la conversion A / N
	/*
	ADCSRA = B10000100;//prescaler/16
	ADCSRB = B00000000;//free running*/
}

int Sensor::Read(int iADC) {
/*************************
Name : ConvAN8
Author : SERMET Kelian
Date : 19/02/19

Retourne le resultat de la conversion effectué sur le CAN
Retourne  : uint8_t [0;255] => Valeur retour du CAN
	BLOQUANT 
*************************/

	int iNumRet;
	/*
	ADMUX = (B01000001); //ADLAR = 0 = right ajust | 4 derniers bits pour ADC0...8 (0000...1000)  (B0001*iADC)
	ADCSRA = ADCSRA | (1 << 7); //Demarre CAN (ON/OFF)
	ADCSRA = ADCSRA | (1 << 6); //Demarre la conversion

	do {

	} while ((ADCSRA&(1<<6)) == (1<<6));
	iNumRet = (int)ADCL;*/
	
	iNumRet = analogRead(iADC);  //To change but with register description it doesn't work and block
	return iNumRet;
}