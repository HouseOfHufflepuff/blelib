package io.elihu.blelib.services;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import io.elihu.blelib.BLEConstants;
import io.elihu.blelib.models.BLESensor;
import io.elihu.blelib.utilities.ByteUtility;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

/**
 * Created by jeremyahrens in 2017
 */

public class BLEDataService extends Service {
    private final static String TAG = BLEDataService.class.getSimpleName();
    private BluetoothManager btManager;
    private BluetoothAdapter btAdapter;
    private String btDeviceAddress;
    private BluetoothGatt btGatt;
    private BLESensor bleSensor;
    private Queue<BluetoothGattDescriptor> descriptorWriteQueue = new LinkedList<BluetoothGattDescriptor>();

    private final BluetoothGattCallback btGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if(newState == BluetoothProfile.STATE_CONNECTED) {
                bleSensor.setConnected(true);
                Intent intent = new Intent();
                intent.setAction(BLEConstants.ACTION_GATT_CONNECTED);
                intent.putExtra(BLEConstants.SENSOR_OBJECT, bleSensor);
                sendBroadcast(intent);
                btGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                bleSensor.setConnected(false);
                Intent intent = new Intent();
                intent.setAction(BLEConstants.ACTION_GATT_DISCONNECTED);
                intent.putExtra(BLEConstants.SENSOR_OBJECT, bleSensor);
                sendBroadcast(intent);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(BLEConstants.ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "warn: onServicesDiscovered received but not sent: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.i(TAG, "onCharacteristicRead status:" + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if(characteristic.getUuid().equals(UUID.fromString(BLEConstants.FIRMWARE_CHARACTERISTIC_UUID))) {
                    broadcastUpdate(BLEConstants.ACTION_FIRMWARE_VERSION_DATA_AVALABLE, characteristic);
                } else {
                    broadcastUpdate(BLEConstants.ACTION_DATA_AVAILABLE, characteristic);
                }
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.i(TAG, "onCharacteristicWrite:" + characteristic.getUuid() + " " + status);
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            //Log.i(TAG, "onCharacteristicChanged:" + characteristic.getUuid());
            if(characteristic.getUuid().equals(UUID.fromString(BLEConstants.CHARACTERISTIC_GYRO_DATA_UUID))) {
                broadcastUpdate(BLEConstants.ACTION_GYRO_DATA_AVAILABLE, characteristic);
            } else {
                broadcastUpdate(BLEConstants.ACTION_DATA_AVAILABLE, characteristic);
            }

        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.i(TAG, "onDescriptorRead");
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            Log.i(TAG, "onDescriptorWrite:" + descriptor.getUuid() + " " + status);

            descriptorWriteQueue.remove();  //pop the item that we just finishing writing
            //if there is more to write, do it!
            if(descriptorWriteQueue.size() > 0) {
                btGatt.writeDescriptor(descriptorWriteQueue.element());
            }
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            Log.i(TAG, "onReliableWriteCompleted");
            super.onReliableWriteCompleted(gatt, status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            Log.i(TAG, "onReliableWriteCompleted");
            super.onReadRemoteRssi(gatt, rssi, status);
        }
    };

    private void broadcastUpdate(final String action) {
        Log.i(TAG, "sending broadcast update:" + action);
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        //Log.i(TAG, action);
        final Intent intent = new Intent(action);
        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            if(action.equals(BLEConstants.ACTION_GYRO_DATA_AVAILABLE)) {
                intent.putExtra(BLEConstants.EXTRA_DATA, data);
            } else {
                String str = ByteUtility.bytesToString(data);
                intent.putExtra(BLEConstants.EXTRA_DATA, str);
            }
        }

        sendBroadcast(intent);
    }



    /**
     * Connects to the ble device
     *
     * @param address mac address of device
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (btAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        final BluetoothDevice device = btAdapter.getRemoteDevice(address);
        bleSensor.setDeviceAddress(address);

        // Previously connected device.  Try to reconnect.
        if (btDeviceAddress != null && address.equals(btDeviceAddress)
                && btGatt != null) {
            Log.d(TAG, "Trying to use an existing btGatt for connection.");
            if (btGatt.connect()) {

                return true;
            } else {
                return false;
            }
        }
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }

        btGatt = device.connectGatt(this, false, btGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        btDeviceAddress = address;
        return true;
    }

    /**
     * Disconnects
     * change is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (btAdapter == null || btGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        btGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (btGatt == null) {
            return;
        }
        btGatt.close();
        btGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (btAdapter == null || btGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        btGatt.readCharacteristic(characteristic);
    }

    public void writeGattDescriptor(BluetoothGattDescriptor d){
        //put the descriptor into the write queue
        descriptorWriteQueue.add(d);
        //if there is only 1 item in the queue, then write it.  If more than 1, we handle asynchronously in the callback above
        if(descriptorWriteQueue.size() == 1){
            btGatt.writeDescriptor(d);
        }
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (btAdapter == null || btGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        boolean didUpdateCharacteristic = btGatt.setCharacteristicNotification(characteristic, enabled);
        List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();
        if(descriptors.size() > 0) {
            // necessary for older phones
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) { /* do nothing */ }
            BluetoothGattDescriptor descriptor = descriptors.get(0);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            writeGattDescriptor(descriptor);
        }
    }


    public boolean turnOnGyroNotifications() {
        Log.i(TAG, "setting notifications");
        if (btAdapter == null || btGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }
        BluetoothGattService gyroService = btGatt.getService(UUID.fromString(BLEConstants.SERVICE_GYRO_UUID));
        BluetoothGattCharacteristic gyroDataCharacteristic = gyroService.getCharacteristic(UUID.fromString(BLEConstants.CHARACTERISTIC_GYRO_DATA_UUID));
        setCharacteristicNotification(gyroDataCharacteristic, true);
        return true;
    }



    public void readFirmwareVersion() {
        BluetoothGattCharacteristic firmwareCharacteristic = btGatt.getService(UUID.fromString(BLEConstants.DEVICE_INFO_SERVICE_UUID)).getCharacteristic(UUID.fromString(BLEConstants.FIRMWARE_CHARACTERISTIC_UUID));
        if(firmwareCharacteristic != null) {
            readCharacteristic(firmwareCharacteristic);
        }
    }


    public boolean turnOnGyro() {
        boolean gyroOn = false;
        byte[] data = new byte[1];
        data[0] = 1;

        BluetoothGattService gyroService = btGatt.getService(UUID.fromString(BLEConstants.SERVICE_GYRO_UUID));
        if(gyroService != null) {
            BluetoothGattCharacteristic gyroConfigureCharacteristic = gyroService.getCharacteristic(UUID.fromString(BLEConstants.CHARACTERISTIC_GYRO_CONFIGURE_UUID));
            if (gyroConfigureCharacteristic != null) {
                gyroConfigureCharacteristic.setValue(data);
                btGatt.writeCharacteristic(gyroConfigureCharacteristic);
                gyroOn = true;
            }
        }
        return gyroOn;
    }

    /*
    public boolean turnOnGyro() {
        byte[] data = new byte[]{(byte)1};
        BluetoothGattService gyroService = this.btGatt.getService(UUID.fromString(BLEConstants.SERVICE_GYRO_UUID));
        if(gyroService != null) {
            BluetoothGattCharacteristic gyroConfigureCharacteristic = gyroService.getCharacteristic(UUID.fromString(BLEConstants.CHARACTERISTIC_GYRO_CONFIGURE_UUID));
            if(gyroConfigureCharacteristic != null) {
                gyroConfigureCharacteristic.setValue(data);
                this.btGatt.writeCharacteristic(gyroConfigureCharacteristic);
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
    */

    public ArrayList<String> getActiveServices() {
        ArrayList ret = new ArrayList();
        Iterator var2 = this.btGatt.getServices().iterator();

        while(var2.hasNext()) {
            BluetoothGattService service = (BluetoothGattService)var2.next();
            ret.add(service.getUuid().toString());
        }

        return ret;
    }

    public void pollGyro() {
        int x = 0;
        while(x < 10) {
            x++;
            for(BluetoothGattService service : btGatt.getServices()) {
                if (service.getUuid().equals(UUID.fromString(BLEConstants.SERVICE_GYRO_UUID))) {
                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        if (characteristic.getUuid().equals(UUID.fromString(BLEConstants.CHARACTERISTIC_GYRO_DATA_UUID))) {
                            readCharacteristic(characteristic);
                        }
                    }
                }
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public BLESensor getConnectedSensor() {
        return bleSensor;
    }

    public class LocalBinder extends Binder {
        public BLEDataService getService() {
            return BLEDataService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        // close when UI is disconnected from service.
        close();
        return super.onUnbind(intent);
    }
    private final IBinder binder = new LocalBinder();

    /**
     * Create BluetoothAdapter
     *
     * @return Return true if successful
     */
    public boolean initialize() {
        if(bleSensor == null) {
            bleSensor = new BLESensor();
        }
        if (btManager == null) {
            btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (btManager == null) {
                Log.e(TAG, "Cannot initialize bluetooth manager");
                return false;
            }
        }
        btAdapter = btManager.getAdapter();
        if (btAdapter == null) {
            Log.e(TAG, "Cannot initialize bluetooth adapter");
            return false;
        }
        return true;
    }
}
