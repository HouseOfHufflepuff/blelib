package io.elihu.blelib.utilities;

import io.elihu.blelib.models.EulerAngle;
import io.elihu.blelib.models.Quaternion;

/**
 * Created by jeremyahrens in 2017.
 */

public class MathUtility {

    // angles in radians
    public static EulerAngle convertEulerAngle(Quaternion q) {
        EulerAngle euler = new EulerAngle();
        float sqw = (float) (q.getW() * q.getW());
        float sqx = (float) (q.getX() * q.getX());
        float sqy = (float) (q.getY() * q.getY());
        float sqz = (float) (q.getZ() * q.getZ());

        // Unit will be a correction factor if the quaternion is not normalized
        float unit = sqx + sqy + sqz + sqw;
        double test = q.getX() * q.getY() + q.getZ() * q.getW();
        
        euler.setRoll((float)Math.atan2(2f * q.getY() * q.getW() - 2f * q.getX() * q.getZ(), sqx - sqy - sqz + sqw));
        euler.setPitch((float)Math.atan2(2f * q.getX() * q.getW() - 2f * q.getY() * q.getZ(), -sqx + sqy - sqz + sqw));
        euler.setYaw((float)Math.asin(2f * test / unit));

        return euler;
    }

    public static float radiansToDegrees(float angleInRadians) {
        return (float) (angleInRadians * (180 / Math.PI));
    }

    public static float degreesToRadians(float angleInDegrees) {
        return (float) (angleInDegrees * (Math.PI / 180));
    }
}
