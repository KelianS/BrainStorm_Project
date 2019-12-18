#include <Arduino.h>
#include "Moteur.h"
#include "Algorithm.h"

Moteur::Moteur() {
	//Moteur Droit
	DDRC |= (1 << 6);//PC6 Output -> PWM     <--OC3A
	DDRD |= (1 << 4);//PD4 Output -> Direction
	TCCR3A = B10000001;// Clear on compare on OC3A / Fast PWM 8 bits
	TCCR3B = B00001011;// Prescaler / 64  --> 980Hz
	TCNT3 = 0;
	OCR3A = 0;

	//Moteur Gauche
	DDRD |= (1 << 7);//PD7 Output -> PWM     <--OC4D
	DDRE |= (1 << 6);//PE6 Output -> Direction
	TCCR4B = B00000111;//Prescaler /64
	TCCR4C = B00001001;//Clear on Compare OC4D  /PWM4D enable
	TCCR4D = B00000000;//Fast PWM MODE
	TCNT4 = 0;
	OCR4D = 0;
}

void Moteur::Left(bool bF_R, byte byValue) {
	//droit
	/*analogWrite(PORT_VITESSE_M1, byValue);
	digitalWrite(PORT_DIRECTION_M1, bF_R);*/
	if (bF_R) { PORTD |= (1 << 4); }
		 else { PORTD &= ~(1 << 4); }

	TCNT3 = 0;
	OCR3A = byValue;
}

void Moteur::Right(bool bF_R, byte byValue) {
	//gauche
	/*analogWrite(PORT_VITESSE_M2, byValue);
	digitalWrite(PORT_DIRECTION_M2, bF_R);*/
	if (bF_R) { PORTE |= (1 << 6); }
		 else { PORTE &= ~(1 << 6); }

	TCNT4 = 0;
	OCR4D = byValue;
}