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
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

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

    private TextView mConnectionState;
    private TextView mDataField;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
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

    // canned data variables to compute blink
    short[] raw_data = {0};
    private int raw_data_index= 0;
    // UI components fo headset
    private Button headsetButton;
    private Button startButton;
    private Button stopButton;
    private TextView attValue;
    private TextView medValue;
    private TextView stateText;
    private TextView sqText;
    private ImageView blinkImage;
    private NskAlgoSdk nskAlgoSdk;
    private int bLastOutputInterval = 1;

    private SeekBar barProgress;
    private SeekBar barProgress2;
    String sBar1 = "0";
    String sBar2 = "0";
    char cMovement = 'A';
    char cMovement2 = 'A';


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
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
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
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                //Echo back received data, with something inserted
               /* final byte[] rxBytes = bluetoothGattCharacteristicHM_10.getValue();
                final byte[] txTest = {0102}; //BAse 8 to send
                final byte[] insertSomething = {0107};
                final byte[] cR = {'\n'};
                byte[] txBytes = new byte[insertSomething.length + rxBytes.length + cR.length];
                System.arraycopy(insertSomething, 0, txBytes, 0, insertSomething.length);
                System.arraycopy(txTest, 0, txBytes, insertSomething.length, rxBytes.length);
                System.arraycopy(cR, 0, txBytes, insertSomething.length + rxBytes.length, cR.length);
               */
               if(bluetoothGattCharacteristicHM_10 != null){
                    bluetoothGattCharacteristicHM_10.setValue("Z ");
                    mBluetoothLeService.writeCharacteristic(bluetoothGattCharacteristicHM_10);
                    mBluetoothLeService.setCharacteristicNotification(bluetoothGattCharacteristicHM_10,true);
                }
            }

        }
    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        String mDeviceName;
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mDataField = (TextView) findViewById(R.id.data_value);

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        nskAlgoSdk = new NskAlgoSdk();

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

        headsetButton = this.findViewById(R.id.headsetButton);
        startButton = this.findViewById(R.id.startButton);
        stopButton = this.findViewById(R.id.stopButton);

        attValue = this.findViewById(R.id.attText);
        medValue = this.findViewById(R.id.medText);

        blinkImage = this.findViewById(R.id.blinkImage);

        stateText = this.findViewById(R.id.stateText);
        sqText = this.findViewById(R.id.sqText);

        barProgress = (SeekBar) findViewById(R.id.seekBar);
        barProgress.setMax(510);
        barProgress.setProgress(255);
        this.barProgress.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
         //   int iProgress = 0;
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(i<156){
                    sBar1 = Integer.toString(255-i);
                    cMovement2 = 'B';
                }else if (i<240){
                    cMovement2 = 'B';
                    sBar1 = "0"+ Integer.toString(255-i);
                }else if (i<270){
                    sBar1 = "000";
                }else if (i<354){
                    cMovement2 = 'A';
                    sBar1 = "0"+Integer.toString(i-255);
                }else{
                    cMovement2 = 'A';
                    sBar1 = Integer.toString(i-255);
                }

                DataSend();



            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                barProgress.setProgress(255);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

                    barProgress.setProgress(255);
                    sBar1 = "000";
                    DataSend();
            }
        });

        barProgress2 = (SeekBar) findViewById(R.id.seekBar2);
        barProgress2.setMax(510);
        barProgress2.setProgress(255);
        this.barProgress2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            //   int iProgress = 0;
            //When Progress value changed.
            @Override
            public void onProgressChanged(SeekBar seekBar2, int i, boolean b) {

                if(i<156){
                    sBar2 = Integer.toString(255-i);
                    cMovement = 'B';
                }else if (i<240){
                    cMovement = 'B';
                    sBar2 = "0"+ Integer.toString(255-i);
                }else if (i<270){
                    sBar2 = "000";
                }else if (i<354){
                    cMovement = 'A';
                    sBar2 = "0"+Integer.toString(i-255);
                }else{
                    cMovement = 'A';
                    sBar2 = Integer.toString(i-255);
                }

                DataSend();

               /* try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                }*/


            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar2) {
                barProgress2.setProgress(255);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar2) {
                barProgress2.setProgress(255);
                sBar2 = "000";
                DataSend();
            }
        });

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
            }
        });

        nskAlgoSdk.setOnEyeBlinkDetectionListener(new NskAlgoSdk.OnEyeBlinkDetectionListener() {
            @Override
            public void onEyeBlinkDetect(int strength) {
                Log.d(TAG, "NskAlgoEyeBlinkDetectionListener: Eye blink detected: " + strength);
                if(bluetoothGattCharacteristicHM_10 != null){
                    // send 'a' to BLE
                    barProgress.setProgress(255);
                    barProgress2.setProgress(255);
                    sBar1 = "0";
                    sBar2 = "0";
                   DataSend();
                }
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        blinkImage.setImageResource(R.mipmap.led_on);
                        Timer timer = new Timer();

                        timer.schedule(new TimerTask() {
                            public void run() {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        blinkImage.setImageResource(R.mipmap.led_off);
                                    }
                                });
                            }
                        }, 0);
                    }
                });
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
    }

    @Override
    protected void onDestroy() {
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
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
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

    private void displayData(String data) {
        if (data != null) {
            mDataField.setText(data);
        }
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

    public static String Datetime()
    {
        Calendar c = Calendar.getInstance();

        String sDate = "[" + c.get(Calendar.YEAR) + "/"
                + (c.get(Calendar.MONTH)+1)
                + "/" + c.get(Calendar.DAY_OF_MONTH)
                + " " + c.get(Calendar.HOUR_OF_DAY)
                + ":" + String.format("%02d", c.get(Calendar.MINUTE))
                + ":" + String.format("%02d", c.get(Calendar.SECOND)) + "]";
        return sDate;
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
    public void DataSend(){
        char cMovementFinale = '0';
        switch(cMovement){
            case 'A':
                if(cMovement2 == 'A'){
                    cMovementFinale = 'A';
                }else{
                    cMovementFinale = 'C';
                }
                break;

            case 'B':
                if(cMovement2== 'A'){
                    cMovementFinale = 'D';
                }else{
                    cMovementFinale = 'B';
                }
                break;
        }

        bluetoothGattCharacteristicHM_10.setValue(cMovementFinale + sBar2 + sBar1 + " "); //Testing the send by
        mBluetoothLeService.writeCharacteristic(bluetoothGattCharacteristicHM_10);
        mBluetoothLeService.setCharacteristicNotification(bluetoothGattCharacteristicHM_10, true);
    }
/*

    public void startProgress(View view) {
        // Do something long
        // a runnable is declared and instantiated
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
              int  iBool = 0;
              int iBcl = 0;
                do { // this could be replaced by a do{} while();

                    try {
                        Thread.sleep(1); // thread sleeps during 1s, in order other threads can do something
                    } catch (InterruptedException e) {
                        e.printStackTrace(); // recover error code
                    }
					 /*
					  * This is an old school manner to exchange with UI thread
					  *

					 seekBar.post(new Runnable() { // This sends a message to UI, it contains a "runnable" executed by UI thread
						@Override
						public void run() {
							text.setText("Updating");
							progress.setProgress(value);
						}
					});
					*/
                    /*
                     * this another old school manner is available with any object of the UI :
                     *  txtResult.post(...
                     */
                    // see "timer" example to understand Runnable class
                    /*if(iBool == 0) {
                        runOnUiThread(new Runnable() { // This sends a message to UI, it contains a "runnable" executed by UI thread
                            @Override
                            public void run() {

                            }
                        });
                    }
                    // when run terminates, the thread is killed
                } while (iBcl != 10000);
                iBcl = 0;
            }
        };
        // This create a new thread and execute runnable defined above.
        // runnable.run() is possible but inside the UI Thread -> to avoid if real-time processing is needed in the runnable
        new Thread(runnable).start();
    }*/
}



