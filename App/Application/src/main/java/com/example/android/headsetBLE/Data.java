package com.example.android.headsetBLE;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import java.util.List;

public class Data extends Activity {
    public TextView textView;
    DatabaseManager m_DataBaseManager;
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    DeviceControlActivity m_DeviceControl;
    private String mDeviceName;
    private String mDeviceAddress;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_data);
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        textView = (TextView) findViewById(R.id.dataBase_value);
        m_DataBaseManager = new DatabaseManager(this);
        List<RobotData> action = m_DataBaseManager.readTop100();
        textView.append(action.toString());
        m_DataBaseManager.close();

    }

    public void onClickControlReturn(View view){
        final Intent intent = new Intent(this, DeviceControlActivity.class);
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, mDeviceName);
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS,mDeviceAddress );
        startActivity(intent);
    }

}
