package com.deo.attractor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.audio.AudioDevice;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.deo.attractor.Utils.MathExpression;

import java.util.ArrayList;

import static com.badlogic.gdx.math.MathUtils.clamp;
import static com.deo.attractor.Utils.Utils.availablePalettes;
import static com.deo.attractor.Utils.Utils.generateSine;
import static com.deo.attractor.Utils.Utils.interpolate;
import static java.lang.StrictMath.random;
import static java.lang.StrictMath.sqrt;

public class Attractor {
    
    ArrayList<MathExpression[]> simRules;
    final Array<Curve> curves;
    private int pointsPerCurve;
    String[] constants;
    
    float scale = 1;
    
    int palette;
    
    AudioDevice attractorAudio;
    private final int sampleRate = 22050;
    
    Array<Color> colors;
    
    private boolean resetInTheNextIteration;
    
    volatile boolean threadActive = true;
    volatile boolean threadComputeCycleFinished = false;
    
    Attractor(int attractorType, int numberOfCurves, int pointsPerCurve, int palette, float spread) {
        
        this.palette = palette;
        this.pointsPerCurve = pointsPerCurve;
        
        simRules = new ArrayList<>();
        
        float maxTimestep;
        String[] bulkArgs = new String[]{"x = 0", "y = 0", "z = 0"};
        switch (attractorType) {
            case (0):
                constants = new String[]{"t = 1", "a = 20", "b = 40", "c = 0.8333", "d = 0.65", "f = 0.5"};
                maxTimestep = 0.001f;
                scale = 0.3f;
                spread = 3;
                break;
            case (1):
                constants = new String[]{"t = 1"};
                maxTimestep = 0.05f;
                break;
            case (2):
                constants = new String[]{"t = 1", "a = -1.4", "b = 4"};
                maxTimestep = 0.004f;
                break;
            case (3):
                constants = new String[]{"t = 1", "a = 0.45", "b = 0.75"};
                maxTimestep = 0.07f;
                scale = 5;
                break;
            case (4):
            default:
                constants = new String[]{"t = 1", "a = 0.7", "b = 3.5", "c = 0.95", "d = 0.25", "f = 0.1", "k = 0.6"};
                maxTimestep = 0.002f;
                scale = 4;
                break;
        }
        
        curves = new Array<>();
        for (int i = 0; i < numberOfCurves; i++) {
            MathExpression[] simRules_local = new MathExpression[3];
            switch (attractorType) {
                case (0):
                    simRules_local[0] = new MathExpression("(b * (y - x) + f * x * z)*t", bulkArgs, constants);
                    simRules_local[1] = new MathExpression("(a * y - x * z)*t", bulkArgs, constants);
                    simRules_local[2] = new MathExpression("(c * z + x * y - d * x * x)*t", bulkArgs, constants);
                    break;
                case (1):
                    simRules_local[0] = new MathExpression("y*t", bulkArgs, constants);
                    simRules_local[1] = new MathExpression("(-x + y * z)*t", bulkArgs, constants);
                    simRules_local[2] = new MathExpression("(1 - y * y)*t", bulkArgs, constants);
                    break;
                case (2):
                    simRules_local[0] = new MathExpression("(a * x - b * y - b * z - y*y)*t", bulkArgs, constants);
                    simRules_local[1] = new MathExpression("(a * y - b * z - b * x - z*z)*t", bulkArgs, constants);
                    simRules_local[2] = new MathExpression("(a * z - b * x - b * y - x*x)*t", bulkArgs, constants);
                    break;
                case (3):
                    simRules_local[0] = new MathExpression("y*t", bulkArgs, constants);
                    simRules_local[1] = new MathExpression("((1 - z) * x - b * y)*t", bulkArgs, constants);
                    simRules_local[2] = new MathExpression("(x * x - a * z)*t", bulkArgs, constants);
                    break;
                case (4):
                default:
                    simRules_local[0] = new MathExpression("((z - a) * x - b * y)*t", bulkArgs, constants);
                    simRules_local[1] = new MathExpression("(b * x + (z - a) * y)*t", bulkArgs, constants);
                    simRules_local[2] = new MathExpression("(k + c * z - (z * z * z) / 3 - (x * x + y * y) * (1 + d * z) + f * z * x * x * x)*t", bulkArgs, constants);
                    break;
            }
            curves.add(new Curve(new Vector3(
                    (float) (random() * spread),
                    (float) (random() * spread),
                    (float) (random() * spread)),
                    simRules_local, maxTimestep, pointsPerCurve));
        }
        
        colors = new Array<>();
        for (int i = 0; i < pointsPerCurve; i++) {
            colors.add(new Color(interpolate(i, pointsPerCurve, availablePalettes[palette])));
        }
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    threadComputeCycleFinished = false;
                    if (resetInTheNextIteration) {
                        for (int i = 0; i < curves.size; i++) {
                            curves.get(i).reset();
                        }
                        resetInTheNextIteration = false;
                    }
                    if (threadActive) {
                        for (int i = 0; i < curves.size; i++) {
                            curves.get(i).advance();
                        }
                    }
                    threadComputeCycleFinished = true;
                    if (Gdx.input.isKeyPressed(Input.Keys.M)) {
                        float frequency;
                        if (Gdx.input.isKeyPressed(Input.Keys.NUM_1)) {
                            frequency = curves.get(0).points.get(curves.get(0).maxPoints - 1).x;
                        } else if (Gdx.input.isKeyPressed(Input.Keys.NUM_2)) {
                            frequency = curves.get(0).points.get(curves.get(0).maxPoints - 1).y;
                        } else {
                            frequency = curves.get(0).points.get(curves.get(0).maxPoints - 1).z;
                        }
                        ArrayList<Float> audioSamples = generateSine(sampleRate, frequency * 1000, 1 / 100f);
                        int len = audioSamples.size();
                        float[] convertedSamples = new float[len];
                        for (int i = 0; i < len; i++) {
                            convertedSamples[i] = audioSamples.get(i);
                        }
                        attractorAudio.writeSamples(convertedSamples, 0, len);
                    }
                }
            }
        }).start();
        
        attractorAudio = Gdx.audio.newAudioDevice(sampleRate, true);
        
    }
    
    void setPointsPerCurve(int pointsPerCurve) {
        for (int i = 0; i < curves.size; i++) {
            curves.get(i).setMaxPoints(pointsPerCurve);
        }
        this.pointsPerCurve = pointsPerCurve;
        colors.clear();
        for (int i = 0; i < pointsPerCurve; i++) {
            colors.add(new Color(interpolate(i, pointsPerCurve, availablePalettes[palette])));
        }
    }
    
    void render(ShapeRenderer renderer, PerspectiveCamera camera, boolean speedColoring, boolean hsv, float speedColoringScale, boolean pointRender) {
        renderer.setProjectionMatrix(camera.combined);
        if (pointRender) {
            renderer.begin(ShapeRenderer.ShapeType.Point);
        } else {
            renderer.begin(ShapeRenderer.ShapeType.Line);
        }
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        for (int curve = 0; curve < curves.size; curve++) {
            for (int point = 0; point < pointsPerCurve - 1; point++) {
                Vector3 pos = curves.get(curve).points.get(point);
                Vector3 nextPos = curves.get(curve).points.get(point + 1);
                if (!speedColoring) {
                    renderer.setColor(colors.get(point));
                } else {
                    float dx = clamp(Math.abs(nextPos.x - pos.x) * speedColoringScale, 0, 1);
                    float dy = clamp(Math.abs(nextPos.y - pos.y) * speedColoringScale, 0, 1);
                    float dz = clamp(Math.abs(nextPos.z - pos.z) * speedColoringScale, 0, 1);
                    if (hsv) {
                        float dist = (float) sqrt(dx * dx + dy * dy + dz * dz);
                        renderer.setColor(new Color().fromHsv(dist * 180, 0.5f, 1).add(0, 0, 0, clamp(dist / 2f, 0, 0.7f)));
                    } else {
                        renderer.setColor(new Color(1 - dx, 1 - dy, 1 - dz, (dx + dy + dz) / 3f));
                    }
                }
                
                if (pointRender) {
                    renderer.point(pos.x * scale,
                            pos.y * scale,
                            pos.z * scale);
                } else {
                    renderer.line(pos.x * scale,
                            pos.y * scale,
                            pos.z * scale,
                            nextPos.x * scale,
                            nextPos.y * scale,
                            nextPos.z * scale);
                }
                
            }
        }
        renderer.end();
    }
    
    void reset() {
        resetInTheNextIteration = true;
    }
}
