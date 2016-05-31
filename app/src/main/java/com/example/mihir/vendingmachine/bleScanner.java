package com.example.mihir.vendingmachine;

/**
 * Created by mihir on 21/4/16.
 */

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Handler;
import android.os.ParcelUuid;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class bleScanner {

    static final long SCAN_TIMEOUT = 5000l;

    private BluetoothManager mBluetoothManager;
    private HashMap<String, BluetoothDevice> mDiscoveredDevices;
    private BluetoothLeScanner mScanner;
    private Context mContext;
    private BluetoothGatt mGatt;
    private BluetoothGattCharacteristic mCharacteristic;
    private BluetoothGattDescriptor descriptor;
    private MainActivity mActivity;

    public bleScanner(Context context, BluetoothManager bluetoothManager) {
        mDiscoveredDevices = new HashMap<>();
        mBluetoothManager = bluetoothManager;
        mContext = context;
        mActivity = (MainActivity)context;
    }

    public void startScanning(){
        if (mBluetoothManager.getAdapter().isEnabled()) {
            mDiscoveredDevices.clear();
            ScanFilter.Builder filterBuilder = new ScanFilter.Builder();
            filterBuilder.setServiceUuid(new ParcelUuid(UUID.fromString(Constants.VM_SERVICE_UUID)));
            ScanFilter filter = filterBuilder.build();
            List<ScanFilter> filters = new ArrayList<ScanFilter>();
            filters.add(filter);
            ScanSettings.Builder settingsBuilder = new ScanSettings.Builder();
            ScanSettings settings = settingsBuilder.build();
            mScanner = mBluetoothManager.getAdapter().getBluetoothLeScanner();
            mScanner.startScan(filters, settings, mScanCallback);
            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mDiscoveredDevices.clear();
                    mScanner.stopScan(mScanCallback);
                    if(!mActivity.getConnectionStatus()){
                        mActivity.setConnectionStatus("Scanning Stopped", false);
                    }
                }
            }, SCAN_TIMEOUT);
        }
        else {
            Log.e("bleScanner", "Bluetooth is disabled!");
        }
    }

    private void connectToGattServer(BluetoothDevice device){
        device.connectGatt(mContext, false, new BluetoothGattCallback() {
            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                mGatt = gatt;
                //refreshDeviceCache(mGatt);
                for (int i = 0; i < gatt.getServices().size(); i++) {
                    BluetoothGattService service = gatt.getServices().get(i);
                    Log.e("bleScanner", "Service discovered: " + service.getUuid());

//                    if(service.getUuid().equals(UUID.fromString("00001800-0000-1000-8000-00805f9b34fb"))){
//                        Log.i("found", "found");
//                    }

                    if (service.getUuid().equals(UUID.fromString(Constants.VM_SERVICE_UUID))) {
                        for (int j = 0; j < service.getCharacteristics().size(); j++) {
                            mCharacteristic = service.getCharacteristics().get(j);
                            Log.e("bleScanner", "Characteristic discovered: " + mCharacteristic.getUuid());
                            mActivity.setConnectionStatus("Connected", true);
                            mGatt.setCharacteristicNotification(mCharacteristic, true);
//                            for (BluetoothGattDescriptor descriptor:mCharacteristic.getDescriptors()){
//                                Log.e("SCANNER DESC", "BluetoothGattDescriptor: "+descriptor.getUuid().toString());
//                            }

                            /*** for ios **/
                            descriptor = mCharacteristic.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
                            //descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                            mGatt.writeDescriptor(descriptor);
                            /*** for ios **/

                            sendMessage("HANDSHAKE");
                        }
                    }
                }
            }

            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                mGatt = gatt;
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.e("bleScanner", "Connected from Gatt Server");
                    gatt.discoverServices();
                }
                else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    Log.e("bleScanner", "Disconnected from Gatt Server");
                    mActivity.setConnectionStatus("Disconnected", false);
                }
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt,
                                             BluetoothGattCharacteristic characteristic, int status) {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    byte[] data = characteristic.getValue();
                    System.out.println("reading");
                    System.out.println(new String(data));
                }
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
                Log.e("bleScanner", "Characteristic changed value: " + characteristic.getStringValue(0));
                String msg = characteristic.getStringValue(0);
                if(msg.contains("ITEM")){
                    mActivity.addItemToMenu(msg);
                }
                else{
                    mActivity.printResponseFromMachine(msg);
                }
            }
        });
    }

    public void sendMessage(String msg) {
        if (mCharacteristic != null) {
            try{
                mCharacteristic.setValue(URLEncoder.encode(msg, "utf-8"));
                mGatt.writeCharacteristic(mCharacteristic);
            }
            catch(UnsupportedEncodingException ex){
                ex.printStackTrace();
            }
        }
        else{
            Log.e("SCANNER", "NO CHARACTERISTIC FOUND");
        }
    }

    public void destroy() {
        if (mGatt != null) {
            mGatt.disconnect();
            mGatt.close();
        }
    }

    private void refreshDeviceCache(BluetoothGatt gatt){
        try {
            BluetoothGatt localBluetoothGatt = gatt;
            Method localMethod = localBluetoothGatt.getClass().getMethod("refresh", new Class[0]);
            if (localMethod != null) {
                localMethod.invoke(localBluetoothGatt, new Object[0]);
            }
        }
        catch (Exception localException) {
            Log.e("bleScanner", localException.getMessage());
        }
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            if (result.getDevice() != null) {
                mScanner.stopScan(this);
                if (!mDiscoveredDevices.containsKey(result.getDevice().getAddress())) {
                    mDiscoveredDevices.put(result.getDevice().getAddress(), result.getDevice());
                    Log.e("bleScanner", "Discovered device: " + result.getDevice());
                    connectToGattServer(result.getDevice());
                }
            }
        }
    };
}