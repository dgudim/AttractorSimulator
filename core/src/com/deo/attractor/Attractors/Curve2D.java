package com.deo.attractor.Attractors;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.deo.attractor.Utils.MathExpression;

import static java.lang.StrictMath.random;
import static com.deo.attractor.Attractors.Attractor2D.computedIterations;

public class Curve2D {
    
    Array<Vector2> points;
    
    int iterations;
    
    MathExpression[] simRules;
    
    Thread computeThread;
    volatile boolean finished = false;
    volatile float progress = 0;
    
    Curve2D(MathExpression[] simRules, final int iterations, final int startingPositions) {
        
        this.simRules = simRules;
        this.iterations = iterations;
        
        points = new Array<>();
        
        computeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < startingPositions; i++) {
                    points.add(calculateNextPosition(new Vector2((float) ((random() - 0.5) * 4), (float) ((random() - 0.5) * 4))));
                    for (int iter = 0; iter < iterations; iter++) {
                        points.add(calculateNextPosition(points.get(points.size - 1)));
                        progress = (i * iterations + iter) / (float) (iterations * startingPositions);
                        computedIterations ++;
                    }
                }
                finished = true;
            }
        });
    }
    
    void startThread() {
        computeThread.start();
    }
    
    Vector2 calculateNextPosition(Vector2 position) {
        float newX = (float) simRules[0].evaluateExp(position.x, position.y);
        float newY = (float) simRules[1].evaluateExp(position.x, position.y);
        return new Vector2(newX, newY);
    }
}
