package com.example.android.headsetBLE;

import android.widget.SeekBar;

public class RobotData {
    private int iDValue;
    private String stValue;


    public RobotData(int uniDValue, String unstValue){
        this.setiDValue(uniDValue);
        this.setStValue(unstValue);
    }

    public int getiDValue() {

        return iDValue;
    }

    public void setiDValue(int iDValue) {
        this.iDValue = iDValue;
    }

    public String getStValue() {
        return stValue;
    }

    public void setStValue(String stValue) {
        this.stValue = stValue;
    }

    @Override
    public String toString(){
        return(iDValue+":"+stValue);
    }
}
