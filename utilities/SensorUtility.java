package io.elihu.blelib.utilities;

import android.bluetooth.BluetoothDevice;
import io.elihu.blelib.BLEConstants;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by jeremyahrens in 2017.
 */

public class SensorUtility {
    private final static String TAG = SensorUtility.class.getSimpleName();

    public static boolean isElihuSensor(BluetoothDevice scanDevice) {
        if(scanDevice != null && scanDevice.getName() != null && scanDevice.getAddress() != null) {
            Pattern pattern = Pattern.compile(BLEConstants.ELIHU_BLE_DEVICE_REGEX);
            Matcher matcher = pattern.matcher(scanDevice.getName());
            if (matcher.find() == true) {
                return true;
            }
        }
        return false;
    }

}
