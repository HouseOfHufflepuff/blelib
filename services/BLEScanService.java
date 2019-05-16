package io.elihu.blelib.services;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import io.elihu.blelib.BLEConstants;
import io.elihu.blelib.R;
import io.elihu.blelib.models.BLESensor;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by jeremyahrens in 2017
 */


public class BLEScanService extends IntentService {
    private static final String TAG = BLEScanService.class.getSimpleName();
    private ArrayList<BLESensor> sensors;
    private boolean isScanning;
    private static final long SCAN_PERIOD = 2000; // scan for 2 seconds
    Handler handler = new Handler();

    public BLEScanService() { super("BLEScanService"); }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, getString(R.string.scan_starting), Toast.LENGTH_SHORT).show();
        return super.onStartCommand(intent,flags,startId);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        sensors = new ArrayList<>();
        scanLeDevices(true);
    }

    private void scanLeDevices(final boolean enable) {
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getApplicationContext().getSystemService(Context.BLUETOOTH_SERVICE);
        final BluetoothAdapter btAdapter = bluetoothManager.getAdapter();
        Log.i(TAG, "start scanning " + sensors.size() + " sensors");
        if (enable) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.i(TAG, "done scanning found " + sensors.size() + " sensors");
                    isScanning = false;
                    btAdapter.stopLeScan(leScanCallback);
                    broadcastScanResults();
                }
            }, SCAN_PERIOD);
            isScanning = true;
            btAdapter.startLeScan(leScanCallback);
        } else {
            isScanning = false;
            btAdapter.stopLeScan(leScanCallback);
        }
    }

    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            final BLESensor sensor = new BLESensor();
            sensor.setDeviceAddress(device.getAddress());
            sensor.setRssiSignalStrength(rssi);
            if(isElihuSensor(device) == true) {
                addSensor(sensor);
            }
        }
    };

    public boolean isElihuSensor(BluetoothDevice scanDevice) {
        if(scanDevice != null && scanDevice.getName() != null && scanDevice.getAddress() != null) {
            Pattern pattern = Pattern.compile(BLEConstants.ELIHU_BLE_DEVICE_REGEX);
            Matcher matcher = pattern.matcher(scanDevice.getName());
            if (matcher.find() == true) {
                return true;
            }
        }
        return false;
    }

    private void addSensor(BLESensor sensor) {
        if(sensor != null) {
            for(BLESensor addedSensor : sensors) {
                //ensure not already in list
                if(addedSensor.getDeviceAddress().equalsIgnoreCase(sensor.getDeviceAddress())) {
                    return;
                }
            }
            sensors.add(sensor);
        }
    }

    // FIXME return sensor with highest signal strength.  range is -90 to 0.  Closest to 0 is the best.
    private BLESensor chooseStrongestSignalSensor() {
        BLESensor sensor = null;
        for(BLESensor addedSensor : sensors) {
            if(sensor == null) {
                return addedSensor;
            }
        }
        return sensor;
    }

    private void broadcastScanResults() {
        BLESensor sensor = chooseStrongestSignalSensor();
        Intent intent;
        if(sensor != null) {
            intent = new Intent(BLEConstants.BROADCAST_ACTION_SCAN_RESULT_FOUND).putExtra(BLEConstants.SENSOR_MAC_ADDRESS, sensor.getDeviceAddress());
        } else {
            intent = new Intent(BLEConstants.BROADCAST_ACTION_SCAN_RESULT_NOT_FOUND);
        }
        sendBroadcast(intent);
    }
}




