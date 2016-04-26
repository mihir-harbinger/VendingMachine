package com.example.mihir.vendingmachine;

/**
 * Created by mihir on 21/4/16.
 */

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;

import java.util.UUID;

import static android.bluetooth.BluetoothGattService.SERVICE_TYPE_PRIMARY;

public class ServiceFactory {

    public static BluetoothGattService generateService() {
        BluetoothGattCharacteristic characteristic = new BluetoothGattCharacteristic(UUID.fromString(Constants.VM_CHARACTERISTIC_UUID),
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_BROADCAST | BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE);
        BluetoothGattService service = new BluetoothGattService(UUID.fromString(Constants.VM_SERVICE_UUID), SERVICE_TYPE_PRIMARY);
        service.addCharacteristic(characteristic);
        return service;
    }

}
