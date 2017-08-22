package com.example.shawmori.gyroscopetest;

import java.io.Serializable;

/**
 * Created by Shaw Mori on 22-Aug-17.
 */
public class Coordinate implements Serializable {

    private float x;
    private float y;
    private float z;

    public Coordinate(float x, float y, float z){
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public float getZ() {
        return z;
    }
}
