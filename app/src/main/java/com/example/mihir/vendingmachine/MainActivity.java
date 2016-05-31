package com.example.mihir.vendingmachine;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
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
    private TextView txtBluetoothStatus;
    private TextView txtConnectionStatus;
    private TextView txtAdvertisementStatus;
    private TextView txtPrintMessage;
    private TextView txtStatus;
    private TextView txtDeviceName;
    private Button btnScan;
    private Button btnPlaceOrder;
    private Button btnToggleMachine;
    private Button btnResetActivity;
    private TabHost tabHost;
    private LinearLayout llMenu;

    //handler
    private Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //bluetooth lib
        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);

        //ui actions
        btnScan         = (Button) findViewById(R.id.button_scan);
        btnPlaceOrder   = (Button) findViewById(R.id.button_placeOrder);
        btnToggleMachine= (Button) findViewById(R.id.button_advertise);
        btnResetActivity = (Button) findViewById(R.id.button_cancel);

        //ui outlets
        txtBluetoothStatus = (TextView) findViewById(R.id.textView1);
        txtConnectionStatus = (TextView) findViewById(R.id.textView2);
        txtAdvertisementStatus = (TextView) findViewById(R.id.textView3);
        txtPrintMessage = (TextView) findViewById(R.id.textView5);
        txtStatus = (TextView) findViewById(R.id.textView6);
        txtDeviceName = (TextView) findViewById(R.id.textView);

        //menu layout
        llMenu = (LinearLayout) findViewById(R.id.menuLayout);

        //onClick listeners
        findViewById(R.id.button_scan).setOnClickListener(this);
        findViewById(R.id.button_advertise).setOnClickListener(this);
        findViewById(R.id.button_placeOrder).setOnClickListener(this);
        findViewById(R.id.button_cancel).setOnClickListener(this);

        //tab view
        tabHost = (TabHost) findViewById(R.id.tabHost);
        tabHost.setup();

        //add tabs and set default tab
        tabHost.addTab(tabHost.newTabSpec("CONSUMER").setIndicator("CONSUMER").setContent(R.id.linearLayout4));
        tabHost.addTab(tabHost.newTabSpec("MACHINE").setIndicator("MACHINE").setContent(R.id.linearLayout5));
        tabHost.setCurrentTab(0);

        handler = new Handler();

        //set device name
        txtDeviceName.setText("Hello, " + Build.MODEL);

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
                Toast.makeText(MainActivity.this, "Please turn Bluetooth ON", Toast.LENGTH_LONG).show();
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
                break;
            case R.id.button_placeOrder:
                if (mBleScanner != null) {
                    mBleScanner.sendMessage("REQ");
                    Log.i("REQUEST", "REQ");
                }
                break;
            case R.id.button_cancel:
                Intent intent = getIntent();
                finish();
                startActivity(intent);
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
        setConnectionStatus("Scanning", true);
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
    public void serveIncomingRequest(final String str){
        runOnUiThread(new Runnable() {
            public void run() {
                txtPrintMessage.setText(str);
            }
        });
    }

    public void printResponseFromMachine(final String str){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                txtStatus.setText(str);
            }
        });
    }

    public void addItemToMenu(final String item){
        Log.i("FOUND", item);

        final CheckBox cb = new CheckBox(MainActivity.this);
        cb.setText(item.substring(5));
        cb.setTextSize(18);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 50, 0, 50);

        final LinearLayout.LayoutParams lp_copy = lp;

        final View v = new View(this);

        runOnUiThread(new Runnable() {
            public void run() {
                llMenu.addView(cb, lp_copy);
                btnPlaceOrder.setVisibility(v.VISIBLE);
                btnResetActivity.setVisibility(v.VISIBLE);
            }
        });
    }

    public void isRequestAccepted(final String msg){

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                handler.post(new Runnable() { // This thread runs in the UI
                    @Override
                    public void run() {
                        new AlertDialog.Builder(MainActivity.this)
                                .setTitle("Incoming Request")
                                .setMessage("Do I have enough sugar, water, milk powder?")
                                .setPositiveButton("YES", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        // continue with delete
                                        mAdvertiser.sendMessage("Dispensing Coffee...");
                                        mAdvertiser.sendMessageWithDelay("Order Served!");
                                    }
                                })
                                .setNegativeButton("NO", new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        mAdvertiser.sendMessage("Machine out of order :(");
                                    }
                                })
                                .show();
                    }
                });
            }
        };
        new Thread(runnable).start();
    }
}