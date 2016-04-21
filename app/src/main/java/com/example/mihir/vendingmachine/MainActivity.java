package com.example.mihir.vendingmachine;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TabHost;
import android.widget.TextView;
import android.graphics.Color;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements OnClickListener {

    //objects
    private BluetoothManager mBluetoothManager;
    private bleAdvertiser mAdvertiser;
    private bleScanner mBleScanner;

    //ui components
    private CheckBox chkGreentea;
    private CheckBox chkBlackCoffee;
    private CheckBox chkLemonade;
    private CheckBox chkHotWater;
    private TextView txtBluetoothStatus;
    private TextView txtConnectionStatus;
    private TextView txtAdvertisementStatus;
    private Button btnPlaceOrder;
    private Button btnToggleMachine;
    private TabHost tabHost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //bluetooth lib
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        //ui actions
        chkGreentea     = (CheckBox) findViewById(R.id.checkBox1);
        chkBlackCoffee  = (CheckBox) findViewById(R.id.checkBox2);
        chkLemonade     = (CheckBox) findViewById(R.id.checkBox3);
        chkHotWater     = (CheckBox) findViewById(R.id.checkBox4);
        btnPlaceOrder   = (Button) findViewById(R.id.button_scan);
        btnToggleMachine= (Button) findViewById(R.id.button_advertise);

        //ui outlets
        txtBluetoothStatus = (TextView) findViewById(R.id.textView1);
        txtConnectionStatus = (TextView) findViewById(R.id.textView2);
        txtAdvertisementStatus = (TextView) findViewById(R.id.textView3);

        //onClick listeners
        findViewById(R.id.button_scan).setOnClickListener(this);
        findViewById(R.id.button_advertise).setOnClickListener(this);

        //tab view
        tabHost = (TabHost) findViewById(R.id.tabHost);
        tabHost.setup();

        //add tabs and set default tab
        tabHost.addTab(tabHost.newTabSpec("CONSUMER").setIndicator("CONSUMER").setContent(R.id.linearLayout4));
        tabHost.addTab(tabHost.newTabSpec("MACHINE").setIndicator("MACHINE").setContent(R.id.linearLayout5));
        tabHost.setCurrentTab(0);


        //check bluetooth status at startup
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mReceiver, filter);

        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            txtConnectionStatus.setText("Error!");
            // Device does not support Bluetooth
        } else {
            if (mBluetoothAdapter.isEnabled()) {
                txtBluetoothStatus.setTextColor(Color.parseColor("#00ddff"));
                txtBluetoothStatus.setText("Bluetooth ON");
                startScanning();
                // Bluetooth is not enable :)
            }
            else{
                txtBluetoothStatus.setTextColor(Color.parseColor("#cccccc"));
                txtBluetoothStatus.setText("Bluetooth OFF");
            }
        }
    }

    //asynchronous response from device regarding bluetooth status
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        setDeviceStatus("Bluetooth OFF", false);
                        setAdvertisementStatus("Vending Machine OFF", false);
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        setDeviceStatus("Turning OFF...", false);
                        break;
                    case BluetoothAdapter.STATE_ON:
                        setDeviceStatus("Bluetooth ON", true);
                        startScanning();
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        setDeviceStatus("Turning ON...", true);
                        break;
                }
            }
        }
    };

    //clean up asynchronous receiver
    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    //
    @Override
    protected void onPause() {
        super.onPause();
        if (mAdvertiser != null) {
            mAdvertiser.destroy();
        }
        if (mBleScanner != null) {
            mBleScanner.destroy();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button_advertise:
                if(txtAdvertisementStatus.getText().toString().equals("Vending Machine ON")){
                    stopAdvertising();
                    btnToggleMachine.setText("Turn ON");
                    break;
                }
                if(txtAdvertisementStatus.getText().toString().equals("Vending Machine OFF")){
                    if(txtBluetoothStatus.getText() == "Bluetooth OFF") {
                        Toast.makeText(MainActivity.this, "Bluetooth is turned OFF", Toast.LENGTH_LONG).show();
                        break;
                    }
                    startAdvertising();
                    btnToggleMachine.setText("Turn OFF");
                }
                break;
            case R.id.button_scan:
                if(txtBluetoothStatus.getText() == "Bluetooth OFF") {
                    Toast.makeText(MainActivity.this, "Bluetooth is turned OFF", Toast.LENGTH_LONG).show();
                    break;
                }
                startScanning();
                setConnectionStatus("Scanning", true);
                break;
//            case R.id.button_send:
//                if (mEditTextMsg.getText() != null) {
//                    if (mBleScanner != null) {
//                        mBleScanner.sendMessage(mEditTextMsg.getText().toString());
//                    } else if (mAdvertiser != null) {
//                        mAdvertiser.sendMessage(mEditTextMsg.getText().toString());
//                    }
//                    mEditTextMsg.setText("");
//                }
//                break;
//            case R.id.responseIndicator_1:
//                //Toast.makeText(this, "Call request sent", Toast.LENGTH_SHORT).show();
//                if (mBleScanner != null) {
//                    mBleScanner.sendMessage("CALL");
//                } else if (mAdvertiser != null) {
//                    mAdvertiser.sendMessage("CALL");
//                }
//                break;
//            case R.id.responseIndicator_2:
//                if (mBleScanner != null) {
//                    mBleScanner.sendMessage("TEST");
//                } else if (mAdvertiser != null) {
//                    mAdvertiser.sendMessage("TEST");
//                }
//                break;
        }
    }

    private void startAdvertising() {
        if (mAdvertiser == null){
            mAdvertiser = new bleAdvertiser(this, mBluetoothManager);
        }
        mAdvertiser.startAdvertising();
    }
    private void stopAdvertising() {
        if (mAdvertiser != null){
            mAdvertiser.stopAdvertising();
        }
    }

    private void startScanning() {
        if (mBleScanner == null){
            mBleScanner = new bleScanner(this, mBluetoothManager);
        }
        mBleScanner.startScanning();
    }

    public void setDeviceStatus(final String message, final boolean colorCode) {
        runOnUiThread(new Runnable() {
            public void run() {

                if (colorCode) {
                    txtBluetoothStatus.setTextColor(Color.parseColor("#00ddff"));
                } else {
                    txtBluetoothStatus.setTextColor(Color.parseColor("#cccccc"));
                }
                txtBluetoothStatus.setText(message);
            }
        });
    }
    public void setConnectionStatus(final String message, final boolean colorCode) {
        runOnUiThread(new Runnable() {
            public void run() {
                if (colorCode) {
                    txtConnectionStatus.setTextColor(Color.parseColor("#99cc00"));
                } else {
                    txtConnectionStatus.setTextColor(Color.parseColor("#cccccc"));
                }
                txtConnectionStatus.setText(message);
            }
        });
    }
    public boolean getConnectionStatus(){
        if(txtConnectionStatus.getText().toString().equals("Connected")){
            return true;
        }
        return false;
    }
    public void setAdvertisementStatus(final String message, final boolean colorCode){
        runOnUiThread(new Runnable(){
            public void run(){
                if(colorCode){
                    txtAdvertisementStatus.setTextColor(Color.parseColor("#ff4081"));
                }
                else{
                    txtAdvertisementStatus.setTextColor(Color.parseColor("#cccccc"));
                    btnToggleMachine.setText("Turn ON");
                }
                txtAdvertisementStatus.setText(message);
            }
        });
    }
}