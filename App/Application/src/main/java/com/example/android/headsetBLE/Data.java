package com.example.android.headsetBLE;

import android.app.Activity;
import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import java.util.List;

public class Data extends Activity {
    public TextView textView;
    DatabaseManager m_DataBaseManager;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_data);
        textView = (TextView) findViewById(R.id.dataBase_value);
        m_DataBaseManager = new DatabaseManager(this);
        List<RobotData> action = m_DataBaseManager.readTop100();
        textView.append(action.toString());
        m_DataBaseManager.close();

    }



}
