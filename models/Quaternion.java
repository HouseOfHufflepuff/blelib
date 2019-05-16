package io.elihu.blelib.models;


/******************************************************************************
 *  Created by Jeremy Ahrens from
 *  http://mathworld.wolfram.com/Quaternion.html
 ******************************************************************************/

public class Quaternion {
    private double w, x, y, z;

    public Quaternion() {
        this.w = 0;
        this.x = 0;
        this.y = 0;
        this.z = 0;
    }

    public Quaternion(double w, double x, double y, double z) {
        this.w = w;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public String toString() {
        return "Quaternion{" +
                "w=" + w +
                ", x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
    }

    // return the quaternion norm
    public double norm() {
        return Math.sqrt(w*w + x*x +y*y + z*z);
    }

    // return the quaternion conjugate
    public Quaternion conjugate() {
        return new Quaternion(w, -x, -y, -z);
    }

    // return a new Quaternion whose value is (this + b)
    public Quaternion plus(Quaternion b) {
        Quaternion a = this;
        return new Quaternion(a.w+b.w, a.x+b.x, a.y+b.y, a.z+b.z);
    }


    // return a new Quaternion whose value is (this * b)
    public Quaternion times(Quaternion b) {
        Quaternion a = this;
        double y0 = a.w*b.w - a.x*b.x - a.y*b.y - a.z*b.z;
        double y1 = a.w*b.x + a.x*b.w + a.y*b.z - a.z*b.y;
        double y2 = a.w*b.y - a.x*b.z + a.y*b.w + a.z*b.x;
        double y3 = a.w*b.z + a.x*b.y - a.y*b.x + a.z*b.w;
        return new Quaternion(y0, y1, y2, y3);
    }

    // return a new Quaternion whose value is the inverse of this
    public Quaternion inverse() {
        double d = w*w + x*x + y*y + z*z;
        return new Quaternion(w/d, -x/d, -y/d, -z/d);
    }


    // return a / b
    // we use the definition a * b^-1 (as opposed to b^-1 a)
    public Quaternion divides(Quaternion b) {
        Quaternion a = this;
        return a.times(b.inverse());
    }

    public double getW() {
        return w;
    }

    public void setW(double w) {
        this.w = w;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }
}
