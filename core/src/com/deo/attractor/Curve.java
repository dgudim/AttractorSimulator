package com.deo.attractor;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.deo.attractor.Utils.MathExpression;

public class Curve {
    
    Vector3 startingPosition;
    
    Array<Vector3> points;
    Array<Color> colors;
    
    int maxPoints;
    float timestep;
    
    MathExpression[] simRules;
    
    Curve(Vector3 startingPoint, MathExpression[] simRules, float timeStep, int maxPoints) {
        
        startingPosition = startingPoint;
        this.simRules = simRules;
        this.maxPoints = maxPoints;
        timestep = timeStep;
        
        points = new Array<>();
        colors = new Array<>();
        
        init(startingPoint);
    }
    
    void reset() {
        for (int i = 0; i < maxPoints; i++) {
            points.set(i, startingPosition);
        }
    }
    
    void init(Vector3 startingPoint) {
        for (int i = 0; i < maxPoints; i++) {
            points.add(startingPoint);
        }
    }
    
    private void shift() {
        for (int i = 0; i < points.size - 1; i++) {
            points.set(i, points.get(i + 1));
        }
    }
    
    void advance() {
        shift();
        points.set(points.size - 1, calculateNextPosition(points.get(points.size - 1)));
    }
    
    Vector3 calculateNextPosition(Vector3 position) {
        float dX = (float) simRules[0].evaluateExp(position.x, position.y, position.z);
        float dY = (float) simRules[1].evaluateExp(position.x, position.y, position.z);
        float dZ = (float) simRules[2].evaluateExp(position.x, position.y, position.z);
        dX *= timestep;
        dY *= timestep;
        dZ *= timestep;
        return new Vector3(position.x + dX, position.y + dY, position.z + dZ);
    }
}
