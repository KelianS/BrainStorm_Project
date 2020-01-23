/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.headsetBLE;
import android.bluetooth.BluetoothClass;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Looper;
import android.view.View;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import android.os.Message;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.neurosky.AlgoSdk.NskAlgoDataType;
import com.neurosky.AlgoSdk.NskAlgoSdk;
import com.neurosky.AlgoSdk.NskAlgoSignalQuality;
import com.neurosky.AlgoSdk.NskAlgoState;
import com.neurosky.AlgoSdk.NskAlgoType;
import com.neurosky.connection.ConnectionStates;
import com.neurosky.connection.DataType.MindDataType;
import com.neurosky.connection.TgStreamHandler;
import com.neurosky.connection.TgStreamReader;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.TimeUnit;


/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity {
    private final static String TAG = "BTLE";
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";
    public String mDeviceName;

    private TextView mConnectionState;
    private String mDeviceAddress;
    public BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<>();
    private boolean mConnected = false;

    private BluetoothGattCharacteristic mNotifyCharacteristic;
    private BluetoothGattCharacteristic bluetoothGattCharacteristicHM_10;

    // variable for headset
    private TgStreamReader tgStreamReader;
    private BluetoothAdapter mBluetoothAdapter;

    // internal variables
    private boolean bInited = false;
    private boolean bRunning = false;
    private int iValueL;//Seekbar values
    private int iValueR;
    private boolean bstart = false; //BT Thread running
    private int iTest = 0;
    private boolean bConnected = false;
    private int iMedActivate = 0;//for Zen Mode
    private boolean bZenModeActivate = false;
    private boolean bFocusActive =false; //for Focus mode
    private int iFocusActivate = 0;//for focus Mode
    boolean bSensorLeft = false;
    boolean bSensorMid = false;
    boolean bSensorRight = false;
    boolean bSensorLeftold = true;
    boolean bSensorMidold = true;
    boolean bSensorRightold = true;

    // canned data variables to compute blink
    short[] raw_data = {0};
    private int raw_data_index= 0;

    // UI components for headset
    private Button headsetButton;
    private Button startButton;
    private Button stopButton;
    private TextView attValue;
    private TextView medValue;
    private TextView stateText;
    private TextView sqText;
    private ImageView blinkImage;
    private NskAlgoSdk nskAlgoSdk;

    //UI components for threads
    private SeekBar seekG;
    private SeekBar seekD;

    //UI components ZEN / ATTENTION MODE
    private Button bZenButton;
    private Button buFocus;
    private Button buReverse;

    //UI components for sensor
    private ImageView blinkLeft;
    private ImageView blinkMid;
    private ImageView blinkRight;

    //DataBase
    private DatabaseManager m_DatabaseManager;

    private boolean bReverse = false;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
                Begin();
                final Button m_DataBase_show = findViewById(R.id.DataBase_show);
                m_DataBase_show.setEnabled(true);
                if (iTest == 0){
                    m_DataBase_show.setClickable(true);
                }
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                Stop();
                //clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());

                if (mGattCharacteristics != null) {
                    final BluetoothGattCharacteristic characteristic =
                            mGattCharacteristics.get(3).get(0);
                    final int charaProp = characteristic.getProperties();
                    if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                        // If there is an active notification on a characteristic, clear
                        // it first so it doesn't update the data field on the user interface.
                        if (mNotifyCharacteristic != null) {
                            mBluetoothLeService.setCharacteristicNotification(
                                    mNotifyCharacteristic, false);
                            mNotifyCharacteristic = null;
                        }
                        mBluetoothLeService.readCharacteristic(characteristic);
                    }
                    if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0) {
                        mNotifyCharacteristic = characteristic;
                        mBluetoothLeService.setCharacteristicNotification(
                                characteristic, true);
                    }

                }

            /***************** Listener Received Data from the Car here ******************/

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                String sReceived = intent.getStringExtra(BluetoothLeService.EXTRA_DATA);

                if(sReceived != null) {
                     //Log.i("R_DATA", sReceived);


                    //If we receive a data (any data) we reply for the alive bit
                    if (bluetoothGattCharacteristicHM_10 != null) {
                        bluetoothGattCharacteristicHM_10.setValue("Z ");
                        mBluetoothLeService.writeCharacteristic(bluetoothGattCharacteristicHM_10);
                    }

                    bSensorLeft = false;
                    bSensorMid = false;
                    bSensorRight = false;


                    if (sReceived.charAt(0) == 'F') {//=Received sensor info
                        if (sReceived.charAt(1) == '1') {
                            bSensorLeft = true;
                           // Log.i("R_DATA", "LEFT");
                        }
                        if (sReceived.charAt(2) == '1') {
                            bSensorMid = true;
                           // Log.i("R_DATA", "MID");
                        }
                        if (sReceived.charAt(3) == '1') {
                            bSensorRight = true;
                          //  Log.i("R_DATA", "RIGHT");
                        }

                    }
                }
            }
            /*****************************************************************************/

        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent intent = getIntent();

        //BT Init
        setContentView(R.layout.gatt_services_characteristics);
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);
        nskAlgoSdk = new NskAlgoSdk();

        // Sets up UI references
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        //DataBase Init
        final Button m_DataBase_show = findViewById(R.id.DataBase_show);
        m_DataBase_show.setEnabled(false);
        m_DatabaseManager = new DatabaseManager(this);

        //Reverse button Init
        buReverse = findViewById(R.id.reverse);

        //Graphical object association
        headsetButton = this.findViewById(R.id.headsetButton);
        startButton = this.findViewById(R.id.startButton);
        stopButton = this.findViewById(R.id.stopButton);
        seekG = this.findViewById(R.id.SeekBarL);
        seekD = this.findViewById(R.id.SeekBarR);
        attValue = this.findViewById(R.id.attText);
        medValue = this.findViewById(R.id.medText);
        blinkImage = this.findViewById(R.id.blinkImage);
        stateText = this.findViewById(R.id.stateText);
        sqText = this.findViewById(R.id.sqText);
        bZenButton = findViewById(R.id.ZenButton);
        buFocus = findViewById(R.id.FocusButton);
        blinkLeft = this.findViewById(R.id.blinkLeft);
        blinkMid = this.findViewById(R.id.blinkMid);
        blinkRight = this.findViewById(R.id.blinkRight);

        bZenButton.setBackgroundColor(0xBB808080);//Change the color of the background for the zen mode button
        bZenButton.setTextColor(Color.BLACK); //Change the color of the text for the zen mode button
        buFocus.setBackgroundColor(0xBB808080);//Change the color of the background for the focus mode button
        buFocus.setTextColor(Color.BLACK); //Change the color of the text for the focus mode button

        seekD.setMax(510);
        seekG.setMax(510);
        /********* Return both Seekbar to 0 when release **********/
        this.seekG.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override
            public void onProgressChanged(SeekBar seekG, int i, boolean b) {
                //
            }

            @Override
            public void onStopTrackingTouch (SeekBar seekG) {
                seekG.setProgress(255);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekG) {
              //
            }
        });
        this.seekD.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){

            @Override
            public void onProgressChanged(SeekBar seekD, int i, boolean b) {
                //
            }

            @Override
            public void onStopTrackingTouch (SeekBar seekD){
                    seekD.setProgress(255);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekD) {
                //
            }
        });
        /**********************************************************/


        /*********************** BT SetUp *************************/
        try {
            // (1) Make sure that the device supports Bluetooth and Bluetooth is on
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
                Toast.makeText(
                        this,getApplicationContext().getString(R.string.error_no_bluetooth)
                        ,
                        Toast.LENGTH_LONG).show();
                //finish();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.i(TAG, "error:" + e.getMessage());
            return;
        }

        headsetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                raw_data = new short[512];
                raw_data_index = 0;


                startButton.setEnabled(false);

                // Example of constructor public TgStreamReader(BluetoothAdapter ba, TgStreamHandler tgStreamHandler)
                tgStreamReader = new TgStreamReader(mBluetoothAdapter,callback);

                if(tgStreamReader != null && tgStreamReader.isBTConnected()){

                    // Prepare for connecting
                    tgStreamReader.stop();
                    tgStreamReader.close();
                }

                // (4) Demo of  using coneplace connectAndStart(),
                //                // please call start() when the nect() and start() to rstate is changed to STATE_CONNECTED
                tgStreamReader.connect();
            }
        });

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!bRunning) {
                    NskAlgoSdk.NskAlgoStart(false);
                } else {
                    NskAlgoSdk.NskAlgoPause();
                }
            }
        });

        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                NskAlgoSdk.NskAlgoStop();
            }
        });

        nskAlgoSdk.setOnSignalQualityListener(new NskAlgoSdk.OnSignalQualityListener() {
            @Override
            public void onSignalQuality(int level) {
                //Log.d(TAG, "NskAlgoSignalQualityListener: level: " + level);
                final int fLevel = level;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // change UI elements here
                        String sqStr = NskAlgoSignalQuality.values()[fLevel].toString();
                        sqText.setText(sqStr);
                    }
                });
            }
        });

        nskAlgoSdk.setOnStateChangeListener(new NskAlgoSdk.OnStateChangeListener() {
            @Override
            public void onStateChange(int state, int reason) {
                String stateStr = "";
                for (NskAlgoState s : NskAlgoState.values()) {
                    if (s.value == state) {
                        stateStr = s.toString();
                    }
                }
                String reasonStr = "";
                for (NskAlgoState r : NskAlgoState.values()) {
                    if (r.value == reason) {
                        reasonStr = r.toString();
                    }
                }
                Log.d(TAG, "NskAlgoSdkStateChangeListener: state: " + stateStr + ", reason: " + reasonStr);
                String StateStr = stateStr;
                if (!reasonStr.equals("")) {
                    StateStr = StateStr + " | " + reasonStr;
                }
                final String finalStateStr = StateStr;
                final int finalState = state;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // change UI elements here
                        stateText.setText(finalStateStr);

                        if (finalState == NskAlgoState.NSK_ALGO_STATE_RUNNING.value || finalState == NskAlgoState.NSK_ALGO_STATE_COLLECTING_BASELINE_DATA.value) {
                            bRunning = true;
                            startButton.setText(getApplicationContext().getString(R.string.label_pause));
                            startButton.setEnabled(true);
                            stopButton.setEnabled(true);
                        } else if (finalState == NskAlgoState.NSK_ALGO_STATE_STOP.value) {
                            bRunning = false;
                            raw_data = null;
                            raw_data_index = 0;
                            startButton.setText(getApplicationContext().getString(R.string.label_started));
                            startButton.setEnabled(false);
                            stopButton.setEnabled(false);
                            headsetButton.setEnabled(true);
                            attValue.setText("--");
                            medValue.setText("--");
                            stateText.setText("--");
                            sqText.setText("--");
                            if (tgStreamReader != null && tgStreamReader.isBTConnected()) {
                                // Prepare for connecting
                                tgStreamReader.stop();
                                tgStreamReader.close();
                            }
                            //System.gc();
                        } else if (finalState == NskAlgoState.NSK_ALGO_STATE_PAUSE.value) {
                            bRunning = false;
                            startButton.setText(getApplicationContext().getString(R.string.label_started));
                            startButton.setEnabled(true);
                            stopButton.setEnabled(true);
                        } else if (finalState == NskAlgoState.NSK_ALGO_STATE_ANALYSING_BULK_DATA.value) {
                            bRunning = true;
                            startButton.setText(getApplicationContext().getString(R.string.label_started));
                            startButton.setEnabled(false);
                            stopButton.setEnabled(true);
                        } else if (finalState == NskAlgoState.NSK_ALGO_STATE_INITED.value || finalState == NskAlgoState.NSK_ALGO_STATE_UNINTIED.value) {
                            bRunning = false;
                            startButton.setText(getApplicationContext().getString(R.string.label_started));
                            startButton.setEnabled(true);
                            stopButton.setEnabled(false);
                        }
                    }
                });
            }
        });

        nskAlgoSdk.setOnSignalQualityListener(new NskAlgoSdk.OnSignalQualityListener() {
            @Override
            public void onSignalQuality(final int level) {
                if (bRunning) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // change UI elements here
                            String sqStr = NskAlgoSignalQuality.values()[level].toString();
                            sqText.setText(sqStr);
                        }
                    });
                }
            }
        });

        nskAlgoSdk.setOnAttAlgoIndexListener(new NskAlgoSdk.OnAttAlgoIndexListener() {
            @Override
            public void onAttAlgoIndex(int value) {

                Log.d(TAG, "NskAlgoAttAlgoIndexListener: Attention:" + value);
                final String finalAttStr = "[" + value + "]";
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // change UI elements here
                        attValue.setText(finalAttStr);

                    }
                });

                int iAtt = value;
                if(iAtt>=50 && iFocusActivate == 0 && bFocusActive == true){
                    iFocusActivate = 1;
                    buFocus.setBackgroundColor(0xbb10ff10);//green

                }else if(iAtt< 30 || bFocusActive == false){
                    iFocusActivate = 0;
                    if(bFocusActive == true){
                        buFocus.setBackgroundColor(0xBBFFCC33);//yellow
                    }
                }

            }
        });

        nskAlgoSdk.setOnMedAlgoIndexListener(new NskAlgoSdk.OnMedAlgoIndexListener() {
            @Override
            public void onMedAlgoIndex(int value) {
                Log.d(TAG, "NskAlgoMedAlgoIndexListener: Meditation:" + value);
                final String finalMedStr = "[" + value + "]";
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // change UI elements here
                        medValue.setText(finalMedStr);
                    }
                });
                int iMed = value;
                if(iMed>=50 && iMedActivate == 0 && bZenModeActivate == true){
                    iMedActivate = 1;
                    bZenButton.setBackgroundColor(0xbb10ff10);//green

                }else if(iMed< 30 || bZenModeActivate == false){
                    iMedActivate = 0;
                    if(bZenModeActivate == true){
                        bZenButton.setBackgroundColor(0xBBFFCC33);//yellow
                    }
                }

            }
        });

        /**********************Eye Blink Here ****************************/
        nskAlgoSdk.setOnEyeBlinkDetectionListener(new NskAlgoSdk.OnEyeBlinkDetectionListener() {
            @Override
            public void onEyeBlinkDetect(int strength) {
                Log.d(TAG, "NskAlgoEyeBlinkDetectionListener: Eye blink detected: " + strength);
               /* if(bluetoothGattCharacteristicHM_10 != null){
                    // send 'a' to BLE
                    bluetoothGattCharacteristicHM_10.setValue("a\n");
                    mBluetoothLeService.writeCharacteristic(bluetoothGattCharacteristicHM_10);
                }*/

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        blinkImage.setImageResource(R.mipmap.led_on);
                        Timer timer = new Timer();
                        Blink();
                        timer.schedule(new TimerTask() {
                            public void run() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        blinkImage.setImageResource(R.mipmap.led_off);
                                    }
                                });
                            }
                        }, 100);
                    }
                });
            }
        });
    }
/*
Function : Begin
Utility : Start Tread100ms
Made By : Kélian Sermet - Gaspard Misery
			- Quentin Noé - Benjamain Bouaziz
Input : Global boolean variable
Output : none
*/

    void Begin(){ //receive seekbar from main activity

        bstart = true; //set to true the running

        //start the Thread.
        new Thread(new BluetoothSending()).start();
        new Thread(new UpdateSensorUI()).start();
    }

    /*
Function : Stop
Utility : Stop Tread100ms
Made By : Kélian Sermet - Gaspard Misery
			- Quentin Noé - Benjamain Bouaziz
Input : Global boolean variable
Output : none
*/
    void Stop(){
        /*******Pause Thread to send**********/
        bstart = false;

        /******** Send Command to stop motors before disconnect *******************/
        if(bluetoothGattCharacteristicHM_10 != null) {
            bluetoothGattCharacteristicHM_10.setValue("A000000 ");
            mBluetoothLeService.writeCharacteristic(bluetoothGattCharacteristicHM_10);
        }
    }

/*
Function : OnClickDataBaseButton
Utility : Start database activity (Only clickable when first data has been send)
Made By : Kélian Sermet - Gaspard Misery
			- Quentin Noé - Benjamain Bouaziz
Input : none
Output : none
*/

    public void onClickDataBaseButton(View view){
        //mBluetoothLeService = null;
        Intent myIntentDataBase = new Intent(DeviceControlActivity.this,Data.class);
        myIntentDataBase.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, mDeviceName);
        myIntentDataBase.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, mDeviceAddress);
        startActivity(myIntentDataBase);
        Log.d("DATABASE","RUN");
    }
    /*
    Function : OnClickFocus
    Utility : Start focus mode
    Made By : Kélian Sermet - Gaspard Misery
                - Quentin Noé - Benjamain Bouaziz
    Input : Global boolean variable
    Output : none
    */
    public void onClickFocus(View view){

        if (bFocusActive == false) {
            if(bConnected == true) {
                bFocusActive = true;
                buFocus.setBackgroundColor(0xBBFFCC33);//yellow
            }
            else{
                Toast.makeText(this,"Please connect the Brain wave before use Zen or Concentration mode",Toast.LENGTH_LONG).show();
            }
        }else{
            bFocusActive = false;
            buFocus.setBackgroundColor(0xBB808080);//default grey
        }

    }
    /*
    Function : OnClickZen
    Utility : Start zen mode
    Made By : Kélian Sermet - Gaspard Misery
                - Quentin Noé - Benjamain Bouaziz
    Input : Global boolean variable
    Output : none
    */
    public void OnClickZen(View view){

            if (bZenModeActivate == false) {
                if(bConnected == true) {
                    bZenModeActivate = true;
                    bZenButton.setBackgroundColor(0xBBFFCC33);//yellow
                }
                else{
                    Toast.makeText(this,"Please connect the Brain wave before use Zen or Concentration mode",Toast.LENGTH_LONG).show();
                }
            }else{
                bZenModeActivate = false;
                bZenButton.setBackgroundColor(0xBB808080);//default grey
            }

    }
     String sVal2;
    /*
Thread  : UpdateSensorUI
Utility : Change color of led corespunding to sensor
Made By : Kélian Sermet - Gaspard Misery
			- Quentin Noé - Benjamain Bouaziz
Input : Sensor Value Global
Output : none
*/
    class UpdateSensorUI implements Runnable {
        @Override
        public void run() {
            while (bstart) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                            if (bSensorLeftold != bSensorLeft) {
                                bSensorLeftold = bSensorLeft;
                                if (bSensorLeft) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            blinkLeft.setImageResource(R.mipmap.led_on1);
                                        }
                                    });
                                } else {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            blinkLeft.setImageResource(R.mipmap.led_off1);
                                        }
                                    });
                                }


                            }
                            if (bSensorMidold != bSensorMid) {
                                bSensorMidold = bSensorMid;
                                if (bSensorMid) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            blinkMid.setImageResource(R.mipmap.led_on2);
                                        }
                                    });
                                } else {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            blinkMid.setImageResource(R.mipmap.led_off2);
                                        }
                                    });
                                }

                            }
                            if (bSensorRightold != bSensorRight) {

                                bSensorRightold = bSensorRight;
                                if (bSensorRight) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            blinkRight.setImageResource(R.mipmap.led_on3);
                                        }
                                    });
                                } else {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            blinkRight.setImageResource(R.mipmap.led_off3);
                                        }
                                    });
                                }

                            }



            }
        }
    }


   /*
Thread  : BluetoothSending
Utility : Check seekBar value and send it to bluetooth (every 50ms)
Made By : Kélian Sermet - Gaspard Misery
			- Quentin Noé - Benjamain Bouaziz
Input : Seekbar (from UI)
Output : none
*/

    class BluetoothSending implements Runnable {
        @Override
        public void run() {
            while (bstart) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                /*********** Get SeekBar Values ******/
                iValueL = seekG.getProgress()-255;
                iValueR = seekD.getProgress()-255;
                if(iFocusActivate==0) {

                    if (iValueL > 190) {
                        iValueL = 190;
                    }
                    else if(iValueL<-190){
                        iValueL = -190;
                    }

                    if(iValueR>190){
                        iValueR = 190;
                    }
                    else if(iValueR<-190){
                        iValueR=-190;
                    }
                }
               // Log.i("R_DATA", Integer.toString(iValueR));

                /******* SeekBar Right Algo : ********/
                String sBarR;
                char cMovementR;
                if (iValueR < -10) {//Reverse
                    cMovementR = 'B';
                    if (-iValueR < 100) {
                        sBarR = "0" + Integer.toString(-iValueR);
                    } else {
                        sBarR = Integer.toString(-iValueR);
                    }
                } else if (iValueR < 10) {//Dead Zone
                    sBarR = "000";
                    cMovementR = 'A';
                } else {
                    cMovementR = 'A';
                    if (iValueR < 100) {
                        sBarR = "0" + Integer.toString(iValueR);
                    } else {
                        sBarR = Integer.toString(iValueR);
                    }
                }

                /******* SeekBar Left Algo : ********/
                String sBarL;
                char cMovementL;
                if (iValueL < -10) {//Reverse
                    cMovementL = 'B';
                    if (-iValueL < 100) {
                        sBarL = "0" + Integer.toString(-iValueL);
                    } else {
                        sBarL = Integer.toString(-iValueL);
                    }
                } else if (iValueL < 10) {//Dead Zone
                    cMovementL = 'A';
                    sBarL = "000";
                } else {
                    cMovementL = 'A';
                    if (iValueL < 100) {
                        sBarL = "0" + Integer.toString(iValueL);
                    } else {
                        sBarL = Integer.toString(iValueL);
                    }
                }

                /*********** Final Movement Algo **************/
                char cMovementFinale = 'A';
                switch (cMovementR) {
                    case 'A'://Right Forward
                        if (cMovementL == 'A') {//Left Forward
                            cMovementFinale = 'A';//both forward
                        } else {
                            cMovementFinale = 'C';
                        }
                        break;

                    case 'B'://Right Reverse
                        if (cMovementL == 'A') { //left forward
                            cMovementFinale = 'D';
                        } else {
                            cMovementFinale = 'B';
                        }
                        break;
                }

                switch(cMovementFinale){
                    case 'A':
                    sVal2 = "B";
                        break;
                    case 'B':
                        sVal2 = "A";
                        break;
                    case 'C':
                        sVal2 = "D";
                        break;
                    case 'D':
                        sVal2 = "C";
                        break;
                }

                sVal2 = sVal2+sBarR+sBarL+ " ";
                /****************** Bluetooth Sending + Updating DataBase while sending ******************/
                DateFormat df = new SimpleDateFormat("yyyy.MM.dd G 'at' HH:mm:ss z");
                String date = df.format(Calendar.getInstance().getTime());
                String sVal = "R : " + Integer.toString(iValueR) + "  L : " + Integer.toString(iValueL) + "\n" + date + "\n\n";

                if(bReverseActivate == false){
                    m_DatabaseManager.insertScore(sVal, sVal2);
                }
                if (bluetoothGattCharacteristicHM_10 != null) {

                    if(bReverseActivate == true){
                        sVal2 = m_DatabaseManager.GetLastData();
                        bluetoothGattCharacteristicHM_10.setValue(sVal2);
                    }
                    else if(iMedActivate == 1) {
                        bluetoothGattCharacteristicHM_10.setValue("E255255 ");
                    }
                    else{
                        bluetoothGattCharacteristicHM_10.setValue(cMovementFinale + sBarR + sBarL + " ");
                    }
                    mBluetoothLeService.writeCharacteristic(bluetoothGattCharacteristicHM_10);
                   // mBluetoothLeService.setCharacteristicNotification(bluetoothGattCharacteristicHM_10, true);
                }
            }
        }
    }



    @Override
    protected void onResume() {

        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
        Begin();

        Log.i("K : ", "Resume");

    }

    @Override
    protected void onPause() {
        Log.i("K : ", "Pause");
        mBluetoothLeService.disconnect();
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
        Stop();
    }

    @Override
    protected void onDestroy() {
        Log.i("K : ", "Destroy");
        Stop();
        mBluetoothLeService.disconnect();
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                Begin();
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                Stop();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void displayGattServices(List<BluetoothGattService> gattServices) {
        UUID UUID_HM_10 =  UUID.fromString(SampleGattAttributes.HM_10);
        final String LIST_NAME = "NAME";
        final String LIST_UUID = "UUID";
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();

            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();
            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
                //Check if it is "HM_10"
                if(uuid.equals(SampleGattAttributes.HM_10)){
                    bluetoothGattCharacteristicHM_10 = gattService.getCharacteristic(UUID_HM_10);
                }
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private void SetAlgo() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // change UI elements here
                int algoTypes = 0;
                startButton.setEnabled(false);
                stopButton.setEnabled(false);
                attValue.setText("--");
                medValue.setText("--");
                stateText.setText("--");
                sqText.setText("");
                algoTypes += NskAlgoType.NSK_ALGO_TYPE_MED.value;
                algoTypes += NskAlgoType.NSK_ALGO_TYPE_ATT.value;
                algoTypes += NskAlgoType.NSK_ALGO_TYPE_BLINK.value;
                if (bInited) {
                    NskAlgoSdk.NskAlgoUninit();
                    bInited = false;
                }
                int ret = NskAlgoSdk.NskAlgoInit(algoTypes, getFilesDir().getAbsolutePath());
                if (ret == 0) {
                    bInited = true;
                }
                Log.d(TAG, "NSK_ALGO_Init(): " + ret);
            }
        });
    }
    private short [] readData(InputStream is, int size) {
        short[] data = new short[size];
        int lineCount = 0;
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        try {
            while (lineCount < size) {
                String line = reader.readLine();
                if (line == null || line.isEmpty()) {
                    Log.d(TAG, "lineCount=" + lineCount);
                    break;
                }
                data[lineCount] = Short.parseShort(line);
                lineCount++;
            }
            Log.d(TAG, "lineCount=" + lineCount);
        } catch (IOException e) {

        }
        return data;
    }

    @Override
    public void onBackPressed() {
        NskAlgoSdk.NskAlgoUninit();
        finish();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    private TgStreamHandler callback = new TgStreamHandler() {

        @Override
        public void onStatesChanged(int connectionStates) {
            Log.d(TAG, "connectionStates change to: " + connectionStates);
            switch (connectionStates) {
                case ConnectionStates.STATE_CONNECTING:
                    // Do something when connecting
                    showToast(getApplicationContext().getString(R.string.msg_connect), Toast.LENGTH_LONG);
                    break;
                case ConnectionStates.STATE_CONNECTED:
                    // Do something when connected
                    tgStreamReader.start();
                    SetAlgo();
                    showToast(getApplicationContext().getString(R.string.msg_connected),  Toast.LENGTH_LONG);
                    bConnected = true;
                    break;
                case ConnectionStates.STATE_WORKING:
                    // Do something when working

                    //(9) demo of recording raw data , stop() will call stopRecordRawData,
                    //or you can add a button to control it.
                    //You can change the save path by calling setRecordStreamFilePath(String filePath) before startRecordRawData
                    //tgStreamReader.startRecordRawData();

                    DeviceControlActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            Button startButton = findViewById(R.id.startButton);
                            startButton.setEnabled(true);
                        }

                    });

                    break;
                case ConnectionStates.STATE_GET_DATA_TIME_OUT:
                    // Do something when getting data timeout

                    //(9) demo of recording raw data, exception handling
                    //tgStreamReader.stopRecordRawData();

                    showToast(getApplicationContext().getString(R.string.msg_time_out), Toast.LENGTH_LONG);

                    if (tgStreamReader != null && tgStreamReader.isBTConnected()) {
                        tgStreamReader.stop();
                        tgStreamReader.close();
                    }

                    break;
                case ConnectionStates.STATE_STOPPED:
                    // Do something when stopped
                    // We have to call tgStreamReader.stop() and tgStreamReader.close() much more than
                    // tgStreamReader.connectAndstart(), because we have to prepare for that.

                    break;
                case ConnectionStates.STATE_DISCONNECTED:
                    // Do something when disconnected
                    bConnected = false; // For debbug
                    break;
                case ConnectionStates.STATE_ERROR:
                    // Do something when you get error message
                    break;
                case ConnectionStates.STATE_FAILED:
                    // Do something when you get failed message
                    // It always happens when open the BluetoothSocket error or timeout
                    // Maybe the device is not working normal.
                    // Maybe you have to try again
                    break;
            }
        }

        @Override
        public void onRecordFail(int flag) {
            // You can handle the record error message here
            Log.e(TAG,"onRecordFail: " +flag);

        }

        @Override
        public void onChecksumFail(byte[] payload, int length, int checksum) {
            // You can handle the bad packets here.
        }

        @Override
        public void onDataReceived(int datatype, int data, Object obj) {
            // You can handle the received data here
            // You can feed the raw data to algo sdk here if necessary.
            //Log.i(TAG,"onDataReceived");
            switch (datatype) {
                case MindDataType.CODE_ATTENTION:
                    short[] attValue = {(short) data};
                    NskAlgoSdk.NskAlgoDataStream(NskAlgoDataType.NSK_ALGO_DATA_TYPE_ATT.value, attValue, 1);
                    break;
                case MindDataType.CODE_MEDITATION:
                    short[] medValue = {(short) data};
                    NskAlgoSdk.NskAlgoDataStream(NskAlgoDataType.NSK_ALGO_DATA_TYPE_MED.value, medValue, 1);
                    break;
                case MindDataType.CODE_POOR_SIGNAL:
                    short[] pqValue = {(short) data};
                    NskAlgoSdk.NskAlgoDataStream(NskAlgoDataType.NSK_ALGO_DATA_TYPE_PQ.value, pqValue, 1);
                    break;
                case MindDataType.CODE_RAW:

                    raw_data[raw_data_index++] = (short)data;
                    if (raw_data_index == 512) {
                        NskAlgoSdk.NskAlgoDataStream(NskAlgoDataType.NSK_ALGO_DATA_TYPE_EEG.value, raw_data, raw_data_index);
                        raw_data_index = 0;
                    }
                    break;
                default:
                    break;
            }
        }

    };

    public void showToast(final String msg, final int timeStyle) {
        DeviceControlActivity.this.runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(getApplicationContext(), msg, timeStyle).show();
            }

        });
    }
    boolean bReverseActivate ;
    public void OnClickReverse(View view){
        buReverse.setBackgroundColor(0xBBFFCC33);
        if(bReverse == false){
            bReverse = true;


        }else{
            bReverse = false;
            buReverse.setBackgroundColor(Color.GRAY);
            bReverseActivate = false;
        }
    }
    int iBlink = 0;
    public void Blink(){
        if((iBlink == 0) && (bReverse == true)){
            iBlink = 1;
            buReverse.setBackgroundColor(Color.GREEN);
            bReverseActivate =true ;
        }else if (iBlink == 1 && bReverse == false ){
            iBlink = 0;
            Toast.makeText(
                    this,"You have do turn on the reverse mode."
                    ,
                    Toast.LENGTH_LONG).show();
        }
    }

}

