package com.deo.attractor.Utils;

public class Vector4 {
    
    public final static Vector4 Zero = new Vector4(0, 0, 0, 0);
    
    public float x;
    public float y;
    public float z;
    public float w;
    
    public float origX;
    public float origY;
    public float origZ;
    public float origW;
    
    public float multiplier = 1;
    
    public Vector4(float x, float y, float z, float w) {
        origX = x;
        origY = y;
        origZ = z;
        origW = w;
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }
    
    public void changeMultiplier(float amount) {
        multiplier += amount;
        x = origX * multiplier;
        y = origY * multiplier;
        z = origZ * multiplier;
        w = origW * multiplier;
    }
    
}
