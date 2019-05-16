package io.elihu.blelib.models;

import io.elihu.blelib.utilities.MathUtility;

/**
 * Created by jeremyahrens
 * angle in radians
 * toString prints degrees
 */

public class EulerAngle {
    private float roll; // rotation about x-axis
    private float pitch; // rotation about y-axis
    private float yaw; // rotation about z-axis

    @Override
    public String toString() {
        return "EulerAngle{" +
                "roll=" + MathUtility.radiansToDegrees(roll) +
                ", pitch=" + MathUtility.radiansToDegrees(pitch) +
                ", yaw=" + MathUtility.radiansToDegrees(yaw) +
                '}';
    }

    public float getRoll() {
        return roll;
    }

    public void setRoll(float roll) {
        this.roll = roll;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }
}
