package io.elihu.blelib.models;

import android.bluetooth.BluetoothDevice;

import java.io.Serializable;

/**
 * Created by jeremyahrens
 */

public class BLESensor implements Serializable {

    private String deviceAddress;
    private int rssiSignalStrength;
    private boolean isConnected;
    private String firmwareVersion;

    public String getDeviceAddress() {
        return deviceAddress;
    }

    public void setDeviceAddress(String deviceAddress) {
        this.deviceAddress = deviceAddress;
    }

    public int getRssiSignalStrength() {
        return rssiSignalStrength;
    }

    public void setRssiSignalStrength(int rssiSignalStrength) {
        this.rssiSignalStrength = rssiSignalStrength;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    @Override
    public String toString() {
        return "BLESensor{" +
                "deviceAddress=" + deviceAddress +
                ", rssiSignalStrength=" + rssiSignalStrength +
                ", isConnected=" + isConnected +
                ", firmwareVersion='" + firmwareVersion + '\'' +
                '}';
    }
}