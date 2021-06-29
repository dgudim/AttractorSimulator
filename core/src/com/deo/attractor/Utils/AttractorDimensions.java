package com.deo.attractor.Utils;

import static com.deo.attractor.Launcher.HEIGHT;
import static com.deo.attractor.Launcher.WIDTH;
import static java.lang.Math.max;

public class AttractorDimensions {
    
    public float minX = -4;
    public float minY = -4;
    public float maxX = 4;
    public float maxY = 4;
    
    public float origMinX = -4;
    public float origMinY = -4;
    public float origMaxX = 4;
    public float origMaxY = 4;
    
    public float height;
    public float width;
    public float zoom;
    public float offsetX;
    public float offsetY;
    
    public float additionalOffsetX = 0;
    public float additionalOffsetY = 0;
    
    public float multiplier = 1;
    
    public AttractorDimensions(){
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
