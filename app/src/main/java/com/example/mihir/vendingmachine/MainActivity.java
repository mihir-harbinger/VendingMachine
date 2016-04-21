package com.example.mihir.vendingmachine;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.TextView;
import android.graphics.Color;

public class MainActivity extends AppCompatActivity  {

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //ui actions
        chkGreentea     = (CheckBox) findViewById(R.id.checkBox1);
        chkBlackCoffee  = (CheckBox) findViewById(R.id.checkBox2);
        chkLemonade     = (CheckBox) findViewById(R.id.checkBox3);
        chkHotWater     = (CheckBox) findViewById(R.id.checkBox4);

        //ui outlets
        txtBluetoothStatus = (TextView) findViewById(R.id.textView1);
        txtConnectionStatus = (TextView) findViewById(R.id.textView2);
        txtAdvertisementStatus = (TextView) findViewById(R.id.textView3);

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
                // Bluetooth is not enable :)
            }
            else{
                txtBluetoothStatus.setTextColor(Color.parseColor("#cccccc"));
                txtBluetoothStatus.setText("Bluetooth OFF");
            }
        }
    }

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
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        setDeviceStatus("Turning OFF...", false);
                        break;
                    case BluetoothAdapter.STATE_ON:
                        setDeviceStatus("Bluetooth ON", true);
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        setDeviceStatus("Turning ON...", true);
                        break;
                }
            }
        }
    };

    private void startAdvertising() {
        if (mAdvertiser == null){
            mAdvertiser = new bleAdvertiser(this, mBluetoothManager);
        }
        mAdvertiser.startAdvertising();
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
    public void setAdvertisementStatus(final String message, final boolean colorCode){
        runOnUiThread(new Runnable(){
            public void run(){
                if(colorCode){
                    txtAdvertisementStatus.setTextColor(Color.parseColor("#ff4081"));
                }
                else{
                    txtAdvertisementStatus.setTextColor(Color.parseColor("#cccccc"));
                }
                txtConnectionStatus.setText(message);
            }
        });
    }
}