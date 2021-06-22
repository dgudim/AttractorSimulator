package com.deo.attractor.Attractors;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.deo.attractor.Utils.MathExpression;

public class Curve3D {
    
    Vector3 startingPosition;
    
    Array<Vector3> points;
    
    int maxPoints;
    float timestep;
    
    MathExpression[] simRules;
    
    Curve3D(Vector3 startingPoint, MathExpression[] simRules, float timeStep, int maxPoints) {
        
        startingPosition = startingPoint;
        this.simRules = simRules;
        this.maxPoints = maxPoints;
        timestep = timeStep;
        
        points = new Array<>();
        
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
        for (int i = 0; i < maxPoints - 1; i++) {
            points.set(i, points.get(i + 1));
        }
    }
    
    void advance() {
        shift();
        points.set(maxPoints - 1, calculateNextPosition(points.get(maxPoints - 1)));
    }
    
    void setMaxPoints(int maxPoints) {
        if (maxPoints > this.maxPoints) {
            for (int i = 0; i < maxPoints - this.maxPoints; i++) {
                points.add(points.get(this.maxPoints - 1));
            }
        }
        this.maxPoints = maxPoints;
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
    
    public void saveToFile(FileHandle saveTo) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < maxPoints; i++) {
            builder.append(points.get(i).x).append(",").append(points.get(i).y).append(",").append(points.get(i).z);
            builder.append("\n");
        }
        saveTo.writeString(builder.substring(0, builder.toString().length() - 1), false);
    }
    
    public void loadFromFile(FileHandle loadFrom) {
        String[] points = loadFrom.readString().split("\n");
        for (int i = 0; i < maxPoints; i++) {
            String[] xyz = points[i].split(",");
            this.points.set(i, new Vector3(Float.parseFloat(xyz[0]), Float.parseFloat(xyz[1]), Float.parseFloat(xyz[2])));
        }
    }
}
