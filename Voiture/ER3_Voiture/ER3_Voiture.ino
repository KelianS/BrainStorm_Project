/*
 Name:		ER3_Voiture.ino
 Created:	04/12/2019 13:26:48
 Author:	kelian
*/
#include "Algorithm.h"

Algorithm mAlgorithm;

void setup() {

}

void loop() {
	
	mAlgorithm.Input_Refresh();
	mAlgorithm.State_Update();
	mAlgorithm.Output_Update();
	   
	if((mAlgorithm.State == mAlgorithm.TState::AUTONOMOUS) || (mAlgorithm.State == mAlgorithm.TState::RECEIVING_IN_AUTO)){
		mAlgorithm.Async_Auto();		
	}
}

