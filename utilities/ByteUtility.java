package io.elihu.blelib.utilities;

import android.util.Log;

import io.elihu.blelib.models.Quaternion;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Created by jeremyahrens in 2017.
 */

public class ByteUtility {
    private final static String TAG = ByteUtility.class.getSimpleName();
    public static byte[] stringToBytes(String str) {
        try {
            return str.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            Log.d(TAG, e.getMessage());
        }
        return null;
    }

    public static byte[] intToBytes(int theInt) {
        byte[] bytes = ByteBuffer.allocate(Integer.SIZE / 8).putInt(theInt).array();
        return bytes;
    }

    public static String bytesToString(byte[] data) {
        final StringBuilder stringBuilder = new StringBuilder(data.length);
        for(byte byteChar : data) {
            stringBuilder.append(String.format("%02X ", byteChar));
        }
        return stringBuilder.toString();
    }

    public static int toInt32(byte[] bytes, int offset) {
        int result = (int)bytes[offset]&0xff;
        result |= ((int)bytes[offset+1]&0xff) << 8;
        result |= ((int)bytes[offset+2]&0xff) << 16;
        result |= ((int)bytes[offset+3]&0xff) << 24;

        return result;
    }

    public static Quaternion convertByteArray(byte[] data) {
        // little endian
        Quaternion q = new Quaternion();

        byte[] bytesW = Arrays.copyOfRange(data, 0, 4);
        ArrayUtility.reverseByteArray(bytesW);
        q.setW(ByteUtility.toInt32(bytesW, 0) / ((float)(1L << 30)));

        byte[] bytesX = Arrays.copyOfRange(data, 4, 8);
        ArrayUtility.reverseByteArray(bytesX);
        q.setX(ByteUtility.toInt32(bytesX, 0) / ((float)(1L << 30)));

        byte[] bytesY = Arrays.copyOfRange(data, 8, 12);
        ArrayUtility.reverseByteArray(bytesY);
        q.setY(ByteUtility.toInt32(bytesY, 0) / ((float)(1L << 30)));

        byte[] bytesZ = Arrays.copyOfRange(data, 12, 16);
        ArrayUtility.reverseByteArray(bytesZ);
        q.setZ(ByteUtility.toInt32(bytesZ, 0) / ((float)(1L << 30)));

        return q;
    }
}

