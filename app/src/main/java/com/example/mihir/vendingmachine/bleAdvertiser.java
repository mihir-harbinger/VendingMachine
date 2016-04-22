package com.example.mihir.vendingmachine;

/**
 * Created by mihir on 21/4/16.
 */

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.UUID;

public class bleAdvertiser {

//    static final long ADVERTISE_TIMEOUT = 30000l;

    private BluetoothManager mBluetoothManager;
    private BluetoothGattServer mGattserver;
    private BluetoothDevice mConnectedDevice;
    private Context mContext;
    private MainActivity mActivity;
//    private BluetoothGattDescriptor descriptor;
//    private BluetoothGattCharacteristic mCharacteristic;


    public bleAdvertiser(Context context, BluetoothManager bluetoothManager) {
        mBluetoothManager = bluetoothManager;
        mContext = context;
        mActivity = (MainActivity)context;
    }

    public void startAdvertising() {
        if (mBluetoothManager.getAdapter().isEnabled()) {
            if (mBluetoothManager.getAdapter().isMultipleAdvertisementSupported()) {
                mGattserver = mBluetoothManager.openGattServer(mContext, new BluetoothGattServerCallback() {
                    @Override
                    public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
                        super.onConnectionStateChange(device, status, newState);
                        if (newState == BluetoothProfile.STATE_CONNECTED) {
                            //sendMessage("HELLO");
                            //mActivity.setConnectionStatus("Connected", true);
                            mConnectedDevice = device;
                        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                            //mActivity.setConnectionStatus("Disconnected", false);
                            mConnectedDevice = null;
                        }
                    }

                    @Override
                    public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
                        super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
                        Log.i("bleAdvertiser", "onCharacteristicReadRequest");
                    }

                    @Override
                    public void onServiceAdded(int status, BluetoothGattService service) {
                        super.onServiceAdded(status, service);
                        bleAdvertiser.this.onServiceAdded();
                    }

                    @Override
                    public void onNotificationSent(BluetoothDevice device, int status) {
                        super.onNotificationSent(device, status);
                        Log.i("bleAdvertiser", "onNotificationSent");
                    }

                    @Override
                    public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
                        super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);
                        if (characteristic.getUuid().equals(UUID.fromString(Constants.CHAT_CHARACTERISTIC_UUID))) {
                            String msg = "";
                            if (value != null) {
                                msg = new String(value);
                                mActivity.isRequestAccepted(msg);
                            }
                            Log.i("bleAdvertiser", "onCharacteristicWriteRequest: " + msg);
                            //mActivity.setMessageText(msg);
                        }
                    }
                });
                mGattserver.addService(ServiceFactory.generateService());
            } else {
                Log.i("bleAdvertiser", "Central mode not supported by the device!");
            }
        } else {
            mActivity.setConnectionStatus("Disconnected", false);
            Log.i("bleAdvertiser", "Bluetooth is disabled!");
        }
    }

    private void onServiceAdded(){
        final BluetoothLeAdvertiser bluetoothLeAdvertiser = mBluetoothManager.getAdapter().getBluetoothLeAdvertiser();
        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.setIncludeTxPowerLevel(false); //necessity to fit in 31 byte advertisement
        //dataBuilder.setManufacturerData(0, advertisingBytes);
        dataBuilder.addServiceUuid(new ParcelUuid(UUID.fromString(Constants.CHAT_SERVICE_UUID)));
        //dataBuilder.setServiceData(pUUID, new byte[]{});

        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED);
        settingsBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
        settingsBuilder.setConnectable(true);

        bluetoothLeAdvertiser.startAdvertising(settingsBuilder.build(), dataBuilder.build(), mAdvertiseCallback);
//        final Handler handler = new Handler(Looper.getMainLooper());
//        handler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                bluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
//                mActivity.setAdvertisementStatus("Vending Machine OFF", false);
//            }
//        }, ADVERTISE_TIMEOUT);
    }

    public void sendMessage(String msg) {
        if (mConnectedDevice != null) {
            BluetoothGattCharacteristic characteristic = ServiceFactory.generateService().getCharacteristic(UUID.fromString(Constants.CHAT_CHARACTERISTIC_UUID));
            characteristic.setValue(msg);
            Log.i("bleAdvertiser", "onCharacteristicWrite and notification");
            mGattserver.notifyCharacteristicChanged(mConnectedDevice, characteristic, false);
        }
    }

    public void sendMessageWithDelay(final String msg) {
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (mConnectedDevice != null) {
                    BluetoothGattCharacteristic characteristic = ServiceFactory.generateService().getCharacteristic(UUID.fromString(Constants.CHAT_CHARACTERISTIC_UUID));
                    characteristic.setValue(msg);
                    Log.i("bleAdvertiser", "onCharacteristicWrite and notification");
                    mGattserver.notifyCharacteristicChanged(mConnectedDevice, characteristic, false);
                }
            }
        }, 5000);

    }

    public void destroy() {
        if (mGattserver != null) {
            mGattserver.clearServices();
            mGattserver.close();
        }
    }

    public void stopAdvertising(){
        final BluetoothLeAdvertiser bluetoothLeAdvertiser = mBluetoothManager.getAdapter().getBluetoothLeAdvertiser();
        bluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
        mActivity.setAdvertisementStatus("Vending Machine OFF", false);
    }

    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            mActivity.setAdvertisementStatus("Vending Machine ON", true);
            Log.i("bleAdvertiser", "Advertising started successfully");
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            mActivity.setAdvertisementStatus("Vending Machine OFF", false);
            Log.i("bleAdvertiser", "Advertising failed error code = " + errorCode);
        }
    };
}
