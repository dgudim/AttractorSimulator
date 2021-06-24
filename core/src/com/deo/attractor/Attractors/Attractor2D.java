package com.deo.attractor.Attractors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;
import com.deo.attractor.Utils.MathExpression;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import static com.deo.attractor.Launcher.HEIGHT;
import static com.deo.attractor.Launcher.WIDTH;
import static com.deo.attractor.Utils.Utils.fontChars;
import static com.deo.attractor.Utils.Utils.makeAScreenShot;
import static java.lang.StrictMath.max;
import static java.lang.StrictMath.min;
import static java.lang.StrictMath.random;

public class Attractor2D {
    
    ArrayList<MathExpression[]> simRules;
    final Array<Curve2D> curves;
    private final int threads;
    String[] constants;
    
    int[][] intensityMap;
    Pixmap pixmap;
    Texture texture;
    
    int attractorType;
    float contrast;
    int colorPalette = 4;
    
    boolean finished = false;
    boolean screenshotMade = false;
    
    BitmapFont font;
    
    public static int computedIterations;
    public static int allIterations;
    ArrayList<Integer> computeIterationsPrev = new ArrayList<>();
    ArrayList<Float> deltaPrev = new ArrayList<>();
    
    SpriteBatch computeBatch;
    Pixmap computePixmap_x;
    Pixmap computePixmap_y;
    Texture computeTexture_x;
    Texture computeTexture_y;
    ShaderProgram computeShader;
    int displayWidth, displayHeight, allPixels;
    
    float[] xValuesGPU_current;
    float[] yValuesGPU_current;
    
    ArrayList<Float> xValuesGPU;
    ArrayList<Float> yValuesGPU;
    
    int gpuIterations;
    int targetIterations;
    
    float divisionPrecision = 100000000f;
    
    Attractor2D(int attractorType, int numberOfThreads, int iterations, int startingPositionsPerThread) {
        
        targetIterations = 10;
        allIterations = numberOfThreads * iterations * startingPositionsPerThread;
        
        pixmap = new Pixmap(WIDTH + 1, HEIGHT + 1, Format.RGBA8888);
        texture = new Texture(pixmap);
        intensityMap = new int[WIDTH + 1][HEIGHT + 1];
        
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("font.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 100;
        parameter.characters = fontChars;
        font = generator.generateFont(parameter);
        generator.dispose();
        font.getData().markupEnabled = true;
        
        this.attractorType = attractorType;
        threads = numberOfThreads;
        
        simRules = new ArrayList<>();
        
        String[] bulkArgs = new String[]{"x = 0", "y = 0"};
        constants = new String[]{"a = -1.23403895", "b = 1.49720576", "c = 2.10414903", "d = -0.93056731"};
        
        switch (attractorType) {
            case (6):
            case (8):
                constants = new String[]{"a = 3.1415926", "b = 3.1415926", "c = 3.1415926", "d = 3.1415926"};
                break;
            case (7):
            default:
                constants = new String[]{"a = 0.8", "b = 3"};
                break;
        }
        
        curves = new Array<>();
        contrast = 0.5f;
        for (int i = 0; i < threads; i++) {
            MathExpression[] simRules_local = new MathExpression[2];
            switch (attractorType) {
                case (0):
                    simRules_local[0] = new MathExpression("sin(a*y) - cos(b*x)", bulkArgs, constants);
                    simRules_local[1] = new MathExpression("sin(c*x) - cos(d*y)", bulkArgs, constants);
                    contrast = 0.4f;
                    break;
                case (1):
                    simRules_local[0] = new MathExpression("d * sin(a*x) - sin(b*y)", bulkArgs, constants);
                    simRules_local[1] = new MathExpression("c * cos(a*x) + cos(b*y)", bulkArgs, constants);
                    contrast = 0.7f;
                    break;
                case (2):
                case (8):
                    simRules_local[0] = new MathExpression("sin(a*y) + c*cos(a*x)", bulkArgs, constants);
                    simRules_local[1] = new MathExpression("sin(b*x) + d*cos(b*y)", bulkArgs, constants);
                    contrast = 0.7f;
                    break;
                case (3):
                    simRules_local[0] = new MathExpression("cos(a*y) + cos(b*x)", bulkArgs, constants);
                    simRules_local[1] = new MathExpression("sin(c*x) + sin(d*y)", bulkArgs, constants);
                    contrast = 0.43f;
                    break;
                case (4):
                    simRules_local[0] = new MathExpression("cos(a*y) - sin(b*x)", bulkArgs, constants);
                    simRules_local[1] = new MathExpression("sin(c*x) - cos(d*y)", bulkArgs, constants);
                    contrast = 0.43f;
                    break;
                case (5):
                case (6):
                    simRules_local[0] = new MathExpression("3.1415926 * sin(a*y) * cos(b*x)", bulkArgs, constants);
                    simRules_local[1] = new MathExpression("3.1415926 * sin(c*x) * cos(d*y)", bulkArgs, constants);
                    contrast = 0.8f;
                    break;
                case (7):
                default:
                    simRules_local[0] = new MathExpression("a * sin(y + tan(b * y))", bulkArgs, constants);
                    simRules_local[1] = new MathExpression("a * sin(x + tan(b * x))", bulkArgs, constants);
                    contrast = 0.6f;
            }
            simRules.add(simRules_local);
            curves.add(new Curve2D(simRules_local, iterations, startingPositionsPerThread));
        }
        
        ShaderProgram.pedantic = false;
        computeShader = new ShaderProgram(Gdx.files.internal("vertex.glsl"), Gdx.files.internal("fragment.glsl"));
        computeBatch = new SpriteBatch();
        computeBatch.disableBlending();
        computeBatch.setShader(computeShader);
        
        displayWidth = Gdx.graphics.getWidth();
        displayHeight = Gdx.graphics.getHeight();
        allPixels = displayWidth * displayHeight;
        
        computePixmap_x = new Pixmap(displayWidth, displayHeight, Format.RGBA8888);
        computePixmap_y = new Pixmap(displayWidth, displayHeight, Format.RGBA8888);
        computePixmap_x.setBlending(Pixmap.Blending.None);
        computePixmap_y.setBlending(Pixmap.Blending.None);
        computeTexture_x = new Texture(computePixmap_x);
        computeTexture_y = new Texture(computePixmap_y);
    
        xValuesGPU = new ArrayList<>();
        yValuesGPU = new ArrayList<>();
    
        xValuesGPU_current = new float[allPixels];
        yValuesGPU_current = new float[allPixels];
        for (int i = 0; i < allPixels; i++) {
            xValuesGPU_current[i] = (float) ((random() - 0.5) * 4);
            yValuesGPU_current[i] = (float) ((random() - 0.5) * 4);
        }
    }
    
    void startThreads() {
        for (int i = 0; i < threads; i++) {
            curves.get(i).startThread();
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean finished = false;
                while (!finished) {
                    finished = true;
                    for (int i = 0; i < threads; i++) {
                        if (!curves.get(i).finished) {
                            finished = false;
                        }
                    }
                }
                double width = 0;
                double height = 0;
                double minX = 100000, minY = 100000;
                
                for(int i = 0; i<xValuesGPU.size(); i++){
                    curves.get(0).points.add(new Vector2(xValuesGPU.get(i), yValuesGPU.get(i)));
                }
                
                for (int i = 0; i < threads; i++) {
                    for (int p = 0; p < curves.get(i).points.size; p++) {
                        minX = min(minX, curves.get(i).points.get(p).x);
                        minY = min(minY, curves.get(i).points.get(p).y);
                    }
                }
                
                for (int i = 0; i < threads; i++) {
                    for (int p = 0; p < curves.get(i).points.size; p++) {
                        curves.get(i).points.get(p).x -= minX < 0 ? minX : -minX;
                        curves.get(i).points.get(p).y -= minY < 0 ? minY : -minY;
                    }
                }
                
                for (int i = 0; i < threads; i++) {
                    for (int p = 0; p < curves.get(i).points.size; p++) {
                        width = max(width, curves.get(i).points.get(p).x);
                        height = max(height, curves.get(i).points.get(p).y);
                    }
                }
                
                double tempScaleH = height / (double) HEIGHT;
                double tempScaleW = width / (double) WIDTH;
                double zoom = Math.max(tempScaleH, tempScaleW);
                double xOffset = WIDTH / 2d - width / zoom / 2d;
                double yOffset = HEIGHT / 2d - height / zoom / 2d;
                
                for (int i = 0; i < threads; i++) {
                    for (int p = 0; p < curves.get(i).points.size; p++) {
                        curves.get(i).points.get(p).x /= zoom;
                        curves.get(i).points.get(p).y /= zoom;
                        
                        curves.get(i).points.get(p).x += xOffset;
                        curves.get(i).points.get(p).y += yOffset;
                    }
                }
                
                for (int i = 0; i < threads; i++) {
                    for (int p = 0; p < curves.get(i).points.size; p++) {
                        
                        int x = (int) curves.get(i).points.get(p).x;
                        int y = (int) curves.get(i).points.get(p).y;
                        
                        intensityMap[x][y]++;
                        
                        for (int xOff = -1; xOff <= 1; ++xOff) {
                            for (int yOff = -1; yOff <= 1; ++yOff) {
                                if (x + xOff >= 0 && x + xOff < WIDTH + 1 && y + yOff >= 0 && y + yOff < HEIGHT + 1) {
                                    intensityMap[x + xOff][y + yOff]++;
                                }
                            }
                        }
                    }
                }
                
                double maxValue = 0;
                for (int x = 0; x <= WIDTH; x++) {
                    for (int y = 0; y <= HEIGHT; y++) {
                        maxValue = max(maxValue, intensityMap[x][y]);
                    }
                }
                
                contrast = 0.8f;
                
                int curveFunc = 0;
                if (Math.abs(contrast - .5) < .04) curveFunc = 1;
                else if (Math.abs(contrast - 1.0) < .04)
                    curveFunc = 2;
                
                for (int x = 0; x <= WIDTH; x++) {
                    for (int y = 0; y <= HEIGHT; y++) {
                        pixmap.drawPixel(x, y, colorFunc((int) (curveFunc(curveFunc, intensityMap[x][y] / maxValue, contrast) * 768), colorPalette));
                    }
                }
                Attractor2D.this.finished = true;
            }
        }).start();
    }
    
    double curveFunc(int type, double value1, double value2) {
        switch (type) {
            case (0):
            default:
                return Math.pow(value1, value2);
            case (1):
                return Math.sqrt(value1);
            case (2):
                return value1;
        }
    }
    
    int cap(int a) {
        return Math.min(a, 255);
    }
    
    int colorFunc(int v, int type) {
        switch (type) {
            case (0):
            default:
                return rgbToRGBA8888(cap(v), cap(v << 1), cap(v << 2));
            case (1):
                return rgbToRGBA8888(cap(v), cap(v << 2), cap(v << 1));
            case (2):
                return rgbToRGBA8888(cap(v << 1), cap(v), cap(v << 2));
            case (3):
                return rgbToRGBA8888(cap(v << 1), cap(v << 2), cap(v));
            case (4):
                return rgbToRGBA8888(cap(v << 2), cap(v << 1), cap(v));
            case (5):
                return rgbToRGBA8888(cap(v << 2), cap(v), cap(v << 1));
            case (6):
                return rgbToRGBA8888(cap(v << 1), cap(v << 1), cap(v));
            case (7):
                return rgbToRGBA8888(cap(v << 1), cap(v), cap(v << 1));
        }
    }
    
    int rgbToRGBA8888(int r, int g, int b) {
        return r << 24 | g << 16 | b << 8 | 255;
    }
    
    float[] decodeFloatsFromFrameBuffer() {
        
        float[] decodedValues = new float[allPixels];
        
        Gdx.gl.glPixelStorei(GL20.GL_PACK_ALIGNMENT, 1);
        ByteBuffer pixels = BufferUtils.newByteBuffer(displayWidth * displayHeight * 4);
        Gdx.gl.glReadPixels(0, 0, displayWidth, displayHeight, GL20.GL_RGBA, GL20.GL_UNSIGNED_BYTE, pixels);
        
        for (int i = 0; i < pixels.capacity() - 4; i += 4) {
            int[] offsets = new int[4];
            for (int off = i; off < i + 4; off++) {
                if (pixels.get(off) < 0) {
                    offsets[off - i] = 256;
                }
            }
            float value = (
                    (pixels.get(i) + offsets[0]) << 24)
                    | ((pixels.get(i + 1) + offsets[1]) << 16)
                    | ((pixels.get(i + 2) + offsets[2]) << 8)
                    | (pixels.get(i + 3) + offsets[3]);
            decodedValues[i / 4] = value;
        }
        return decodedValues;
    }
    
    float[] processAndDecodeData(boolean y) {
        computeShader.setUniformi("processY", y ? 1 : 0);
        
        computeBatch.begin();
        computeTexture_x.bind(0);
        computeTexture_y.bind(1);
        computeShader.setUniformi("u_sampler2D_x", 0);
        computeShader.setUniformi("u_sampler2D_y", 1);
        computeBatch.draw(computeTexture_y, 0, 0);
        computeBatch.end();
        
        return decodeFloatsFromFrameBuffer();
    }
    
    void processOnGPU() {
        
        computeShader.setUniformi("attractorType", attractorType);
        
        for (int x = 0; x < displayWidth; x++) {
            for (int y = 0; y < displayHeight; y++) {
                computePixmap_x.drawPixel(x, y, (int) (xValuesGPU_current[x + y * displayWidth] * divisionPrecision));
            }
        }
        for (int x = 0; x < displayWidth; x++) {
            for (int y = 0; y < displayHeight; y++) {
                computePixmap_y.drawPixel(x, y, (int) (yValuesGPU_current[x + y * displayWidth] * divisionPrecision));
            }
        }
        computeTexture_x.draw(computePixmap_x, 0, 0);
        computeTexture_y.draw(computePixmap_y, 0, 0);
    
        xValuesGPU_current = processAndDecodeData(false);
        yValuesGPU_current = processAndDecodeData(true);
    
        for (float v : xValuesGPU_current) {
            xValuesGPU.add(v);
        }
        for (float v : yValuesGPU_current) {
            yValuesGPU.add(v);
        }
        gpuIterations ++;
        computedIterations += allPixels;
    }
    
    void render(SpriteBatch batch, OrthographicCamera camera, ShapeRenderer renderer, float delta) {
        
        /*
        if(!finished && gpuIterations < targetIterations){
            processOnGPU();
        }
         */
        
        int computedItersNow = computedIterations;
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        texture.draw(pixmap, 0, 0);
        batch.draw(texture, -WIDTH / 2f - 1, -HEIGHT / 2f - 1);
        batch.end();
        if (finished && !screenshotMade) {
            makeAScreenShot("Attractor2D" + attractorType + "_" + random());
            screenshotMade = true;
        }
        if (!finished) {
            
            renderer.setProjectionMatrix(camera.combined);
            renderer.begin(ShapeRenderer.ShapeType.Filled);
            
            float divWidth = WIDTH / (float) curves.size;
            
            for (int i = 0; i < curves.size; i++) {
                renderer.setColor(new Color(1 - curves.get(i).progress, curves.get(i).progress, 0.3f, 1));
                renderer.rect(-WIDTH / 2f + divWidth * i + 10, -HEIGHT / 2f, divWidth - 10, HEIGHT / 4f * curves.get(i).progress);
            }
            renderer.end();
            batch.begin();
            
            if (computeIterationsPrev.size() < 150) {
                computeIterationsPrev.add(computedIterations - computedItersNow);
                deltaPrev.add(delta);
            } else {
                for (int i = 0; i < computeIterationsPrev.size() - 1; i++) {
                    computeIterationsPrev.set(i, computeIterationsPrev.get(i + 1));
                    deltaPrev.set(i, deltaPrev.get(i + 1));
                }
                computeIterationsPrev.set(computeIterationsPrev.size() - 1, computedIterations - computedItersNow);
                deltaPrev.set(deltaPrev.size() - 1, delta);
            }
            
            float itersSmoothed = 0;
            float deltaSmoothed = 0;
            for (int i = 0; i < computeIterationsPrev.size(); i++) {
                itersSmoothed += computeIterationsPrev.get(i);
                deltaSmoothed += deltaPrev.get(i);
            }
            itersSmoothed /= (float) computeIterationsPrev.size();
            deltaSmoothed /= (float) deltaPrev.size();
            
            font.draw(batch,
                    "KIpS:" + (int) ((itersSmoothed / deltaSmoothed) / 1000)
                            + " Delta:" + (int) (deltaSmoothed * 1000) + "ms "
                            + (int) (computedIterations / (float) allIterations * 100 + 0.5f) + "%",
                    -WIDTH / 2f + 50, -HEIGHT / 2f + 100);
            batch.end();
        }
    }
}
