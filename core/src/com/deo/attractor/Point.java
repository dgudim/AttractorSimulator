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
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.deo.attractor.Utils.MathExpression;

import static java.lang.StrictMath.max;
import static java.lang.StrictMath.min;

public class Point {
    
    private final ModelBuilder modelBuilder;
    
    Array<Vector3> points;
    Array<Model> models;
    Array<ModelInstance> instances;
    Array<Color> colors;
    
    int maxPoints = 100;
    float maxTimestep;
    float currentTimestep;
    float scale;
    
    boolean colorAccordingToPosition = false;
    
    private float maxX = 0.0001f;
    private float maxY = 0.0001f;
    private float maxZ = 0.0001f;
    
    private final Color[] palette;
    MathExpression[] simRules;
    
    Point(Vector3 startingPoint, Color[] palette, MathExpression[] simRules, float maxTimestep, float scale) {
        
        this.simRules = simRules;
        this.maxTimestep = maxTimestep;
        this.scale = scale;
        currentTimestep = maxTimestep;
        this.palette = palette;
        
        modelBuilder = new ModelBuilder();
        
        points = new Array<>();
        colors = new Array<>();
        models = new Array<>();
        instances = new Array<>();
        
        init(startingPoint);
    }
    
    void setTimestep(float newTimestep) {
        currentTimestep = min(newTimestep, maxTimestep);
    }
    
    void init(Vector3 startingPoint) {
        for (int i = 0; i < maxPoints; i++) {
            points.add(startingPoint);
            colors.add(palette[palette.length - 1]);
            Model bulk = new Model();
            ModelInstance bulkInstance = new ModelInstance(bulk);
            models.add(bulk);
            instances.add(bulkInstance);
        }
    }
    
    private void shift() {
        for (int i = 0; i < instances.size - 1; i++) {
            points.set(i, points.get(i + 1));
            models.set(i, models.get(i + 1));
            instances.set(i, instances.get(i + 1));
            try {
                if (colorAccordingToPosition) {
                    float a = i / (float) maxPoints;
                    instances.get(i).materials.get(0).set(
                            ColorAttribute.createDiffuse(new Color(
                                    points.get(i).x / maxX,
                                    points.get(i).y / maxY,
                                    points.get(i).z / maxZ, a)),
                            new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA));
                    
                    maxX = max(points.get(i).x, maxX);
                    maxY = max(points.get(i).y, maxY);
                    maxZ = max(points.get(i).z, maxZ);
                } else {
                    instances.get(i).materials.get(0).set(
                            ColorAttribute.createDiffuse(colors.get(i)),
                            new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA));
                }
            } catch (Exception e) {
                //ignore
            }
            if (!colorAccordingToPosition) {
                colors.set(i, new Color(interpolate(i, maxPoints, palette)));
            }
        }
    }
    
    void advance() {
        shift();
        Vector3 prevPos = points.get(points.size - 1);
        Color color = colors.get(colors.size - 1);
        
        if (colorAccordingToPosition) {
            color = new Color(
                    points.get(points.size - 1).x / 2f,
                    points.get(points.size - 1).y / 2f,
                    points.get(points.size - 1).z / 2f, 1);
        }
        
        points.set(points.size - 1, calculateNextPosition(prevPos));
        
        addLine(points.get(points.size - 2), points.get(points.size - 1), color);
    }
    
    void resetTrail() {
        dispose();
        instances.clear();
        colors.clear();
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
    
    void render(Environment environment, ModelBatch batch) {
        for (int i = instances.size - 1; i >= 0; i--) {
            batch.render(instances.get(i), environment);
        }
    }
    
    void addLine(Vector3 beginning, Vector3 end, Color color) {
        modelBuilder.begin();
        MeshPartBuilder builder = modelBuilder.part("line", 1, 3, new Material(ColorAttribute.createDiffuse(color), new BlendingAttribute(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)));
        builder.line(
                beginning.x * scale, beginning.y * scale, beginning.z * scale,
                end.x * scale, end.y * scale, end.z * scale);
        Model model = modelBuilder.end();
        
        ModelInstance instance = new ModelInstance(model);
        models.set(models.size - 1, model);
        instances.set(instances.size - 1, instance);
    }
    
    static int interpolate(float step, float maxValue, Color... colors) {
        step = Math.max(Math.min(step / maxValue, 1.0f), 0.0f);
        
        switch (colors.length) {
            case 0:
                throw new IllegalArgumentException("At least one color required.");
            
            case 1:
                return Color.argb8888(colors[0]);
            
            case 2:
                return mixTwoColors(colors[0], colors[1], step);
            
            default:
                
                int firstColorIndex = (int) (step * (colors.length - 1));
                
                if (firstColorIndex == colors.length - 1) {
                    return Color.argb8888(colors[colors.length - 1]);
                }
                
                // stepAtFirstColorIndex will be a bit smaller than step
                float stepAtFirstColorIndex = (float) firstColorIndex
                        / (colors.length - 1);
                
                // multiply to increase values to range between 0.0f and 1.0f
                float localStep = (step - stepAtFirstColorIndex)
                        * (colors.length - 1);
                
                return mixTwoColors(colors[firstColorIndex],
                        colors[firstColorIndex + 1], localStep);
        }
        
    }
    
    static int mixTwoColors(Color color1, Color color2, float ratio) {
        return Color.rgba8888(color1.r * (1f - ratio) + color2.r * ratio, color1.g * (1f - ratio) + color2.g * ratio, color1.b * (1f - ratio) + color2.b * ratio, color1.a * (1f - ratio) + color2.a * ratio);
    }
    
    void dispose() {
        for (int i = 0; i < models.size; i++) {
            models.get(i).dispose();
        }
        models.clear();
    }
    
}
