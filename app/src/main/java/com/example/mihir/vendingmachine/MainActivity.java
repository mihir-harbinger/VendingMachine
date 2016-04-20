package com.example.mihir.vendingmachine;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity  {

    private CheckBox chkGreentea;
    private CheckBox chkBlackCoffee;
    private CheckBox chkLemonade;
    private CheckBox chkHotWater;
    private TextView txtBluetoothStatus;
    private TextView txtConnectionStatus;

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
    }
}