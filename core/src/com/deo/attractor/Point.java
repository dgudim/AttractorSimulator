package com.deo.attractor;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.deo.attractor.Utils.MathExpression;

import java.util.ArrayList;

import static java.lang.StrictMath.max;
import static java.lang.StrictMath.min;

public class Point {
    
    Vector3 startingPosition;
    
    Array<Vector3> points;
    Array<Color> colors;
    
    int maxPoints;
    float currentTimestep;
    float scale;
    
    MathExpression[] simRules;
    
    Point(Vector3 startingPoint, MathExpression[] simRules, float timeStep, float scale, int maxPoints) {
        
        startingPosition = startingPoint;
        this.simRules = simRules;
        this.scale = scale;
        this.maxPoints = maxPoints;
        currentTimestep = timeStep;
        
        points = new Array<>();
        colors = new Array<>();
        
        init(startingPoint);
    }
    
    void reset() {
        points.clear();
        init(startingPosition);
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
    
    void resetTrail() {
        Vector3 prevPos = points.get(points.size - 1);
        points.clear();
        init(prevPos);
    }
    
    Vector3 calculateNextPosition(Vector3 position) {
        float dX = (float) simRules[0].evaluateExp(position.x, position.y, position.z);
        float dY = (float) simRules[1].evaluateExp(position.x, position.y, position.z);
        float dZ = (float) simRules[2].evaluateExp(position.x, position.y, position.z);
        dX *= currentTimestep;
        dY *= currentTimestep;
        dZ *= currentTimestep;
        return new Vector3(position.x + dX, position.y + dY, position.z + dZ);
    }
}
