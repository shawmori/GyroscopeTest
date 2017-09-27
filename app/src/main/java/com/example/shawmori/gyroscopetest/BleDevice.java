package com.example.shawmori.gyroscopetest;

import android.bluetooth.BluetoothDevice;

import java.io.Serializable;

public class BleDevice implements Serializable  {

    private BluetoothDevice device;
    private int rssi;

    public BleDevice(BluetoothDevice device, int rssi) {
        this.device = device;
        this.rssi = rssi;
    }
    public String getAddress(){return device.getAddress();}
    public String getName(){return device.getName();}
    public void setRssi(int i){this.rssi = i;}
    public int getRssi(){return rssi;}
}