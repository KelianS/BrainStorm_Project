#pragma once
#define FORWARD 1 
#define REVERSE 0 


class Moteur
{
public :
	Moteur();
	void Right(bool bF_R, byte byValue);
	void Left(bool bF_R, byte byValue);
	 
};

