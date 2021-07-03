package com.deo.attractor.Utils;

import static com.deo.attractor.Launcher.HEIGHT;
import static com.deo.attractor.Launcher.WIDTH;
import static java.lang.Math.max;

public class AttractorDimensions {
    
    public float minX;
    public float minY;
    public float maxX;
    public float maxY;
    
    public float origMinX;
    public float origMinY;
    public float origMaxX;
    public float origMaxY;
    
    public float height;
    public float width;
    public float zoom;
    public float offsetX;
    public float offsetY;
    
    public float additionalOffsetX = 0;
    public float additionalOffsetY = 0;
    
    public float multiplier = 1;
    
    public AttractorDimensions(float[] dimensions){
        minX = dimensions[0];
        minY = dimensions[1];
        maxX = dimensions[2];
        maxY = dimensions[3];
        origMinX = dimensions[0];
        origMinY = dimensions[1];
        origMaxX = dimensions[2];
        origMaxY = dimensions[3];
        calculate();
    }
    
    public AttractorDimensions(float minX, float minY, float maxX, float maxY) {
        origMinX = minX;
        origMinY = minY;
        origMaxX = maxX;
        origMaxY = maxY;
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        calculate();
    }
    
    private void calculate(){
        width = maxX - minX;
        height = maxY - minY;
    
        zoom = max(height / (float) HEIGHT, width / (float) WIDTH);
        offsetX = WIDTH / 2f - width / zoom / 2f + additionalOffsetX;
        offsetY = HEIGHT / 2f - height / zoom / 2f + additionalOffsetY;
    }
    
    public void translate(float amountX, float amountY, float amountZ){
        
        additionalOffsetX += amountX;
        additionalOffsetY += amountY;
        multiplier += amountZ;
        multiplier = max(multiplier, 0.01f);
        
        minX = origMinX * multiplier;
        minY = origMinY * multiplier;
        maxX = origMaxX * multiplier;
        maxY = origMaxY * multiplier;
        calculate();
    }
    
    @Override
    public String toString() {
        return "minX:" + minX + " minY:" + minY + " maxX:" + maxX + " maxY:" + maxY;
    }
}
