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

import static com.badlogic.gdx.graphics.GL20.GL_TEXTURE0;
import static com.badlogic.gdx.graphics.GL20.GL_TEXTURE1;
import static com.deo.attractor.Attractors.RenderMode.CPU;
import static com.deo.attractor.Attractors.RenderMode.GPU;
import static com.deo.attractor.Launcher.HEIGHT;
import static com.deo.attractor.Launcher.WIDTH;
import static com.deo.attractor.Utils.Utils.colorFunc;
import static com.deo.attractor.Utils.Utils.curveFunc;
import static com.deo.attractor.Utils.Utils.fontChars;
import static com.deo.attractor.Utils.Utils.makeAScreenShot;
import static java.lang.StrictMath.abs;
import static java.lang.StrictMath.max;
import static java.lang.StrictMath.min;
import static java.lang.StrictMath.random;

enum RenderMode {GPU, CPU}

public class Attractor2D {
    
    private final RenderMode renderMode;
    
    final Array<Curve2D> curves;
    private final int threads;
    
    int[][] intensityMap;
    Pixmap pixmap;
    Texture texture;
    
    int attractorType;
    float contrast;
    int colorPalette = 4;
    float copyOverThreadProgress;
    
    boolean finished = false;
    boolean screenshotMade = false;
    
    private final BitmapFont font;
    
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
    
    Array<Vector2> GPUValues;
    Curve2D GPUCurve;
    
    float divisionPrecision = 100000000f;
    
    Attractor2D(RenderMode renderMode, int attractorType, int CPUThreads, int iterationsPerPosition, int startingPositionsPerThread, int GPUIterations, int GPUPointsDivider) {
        
        this.renderMode = renderMode;
        
        allIterations = CPUThreads * iterationsPerPosition * startingPositionsPerThread;
        
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
        threads = CPUThreads;
        
        String[] constants;
        float[] constants_numeric;
        String[] bulkArgs = new String[]{"x = 0", "y = 0"};
        switch (attractorType) {
            case (6):
            case (8):
                constants = new String[]{"a = 3.141592", "b = 3.141592", "c = 3.141592", "d = 3.141592"};
                constants_numeric = new float[]{3.141592f, 3.141592f, 3.141592f, 3.141592f};
                break;
            case (7):
                constants = new String[]{"a = 0.8", "b = 3"};
                constants_numeric = new float[]{0.8f, 3, 0, 0};
                break;
            default:
                constants = new String[]{"a = -1.234039", "b = 1.497206", "c = 2.104149", "d = -0.930567"};
                constants_numeric = new float[]{-1.234039f, 1.497206f, 2.104149f, -0.930567f};
        }
        
        contrast = 0.5f;
        switch (attractorType) {
            case (0):
                contrast = 0.4f;
                break;
            case (1):
            case (2):
            case (8):
                contrast = 0.7f;
                break;
            case (3):
            case (4):
                contrast = 0.43f;
                break;
            case (5):
            case (6):
                contrast = 0.8f;
                break;
            case (7):
            default:
                contrast = 0.6f;
        }
        
        curves = new Array<>();
        if (renderMode.equals(CPU)) {
            for (int i = 0; i < threads; i++) {
                MathExpression[] simRules_local = new MathExpression[2];
                switch (attractorType) {
                    case (0):
                        simRules_local[0] = new MathExpression("sin(a*y) - cos(b*x)", bulkArgs, constants);
                        simRules_local[1] = new MathExpression("sin(c*x) - cos(d*y)", bulkArgs, constants);
                        break;
                    case (1):
                        simRules_local[0] = new MathExpression("d * sin(a*x) - sin(b*y)", bulkArgs, constants);
                        simRules_local[1] = new MathExpression("c * cos(a*x) + cos(b*y)", bulkArgs, constants);
                        break;
                    case (2):
                    case (8):
                        simRules_local[0] = new MathExpression("sin(a*y) + c*cos(a*x)", bulkArgs, constants);
                        simRules_local[1] = new MathExpression("sin(b*x) + d*cos(b*y)", bulkArgs, constants);
                        break;
                    case (3):
                        simRules_local[0] = new MathExpression("cos(a*y) + cos(b*x)", bulkArgs, constants);
                        simRules_local[1] = new MathExpression("sin(c*x) + sin(d*y)", bulkArgs, constants);
                        break;
                    case (4):
                        simRules_local[0] = new MathExpression("cos(a*y) - sin(b*x)", bulkArgs, constants);
                        simRules_local[1] = new MathExpression("sin(c*x) - cos(d*y)", bulkArgs, constants);
                        break;
                    case (5):
                    case (6):
                        simRules_local[0] = new MathExpression("3.1415926 * sin(a*y) * cos(b*x)", bulkArgs, constants);
                        simRules_local[1] = new MathExpression("3.1415926 * sin(c*x) * cos(d*y)", bulkArgs, constants);
                        break;
                    case (7):
                    default:
                        simRules_local[0] = new MathExpression("a * sin(y + tan(b * y))", bulkArgs, constants);
                        simRules_local[1] = new MathExpression("a * sin(x + tan(b * x))", bulkArgs, constants);
                }
                curves.add(new Curve2D(simRules_local, iterationsPerPosition, startingPositionsPerThread));
            }
        }
        
        if (renderMode.equals(GPU)) {
            displayWidth = Gdx.graphics.getWidth() / GPUPointsDivider;
            displayHeight = Gdx.graphics.getHeight() / GPUPointsDivider;
            allPixels = displayWidth * displayHeight;
            
            allIterations = GPUIterations * allPixels;
            
            computePixmap_x = new Pixmap(displayWidth, displayHeight, Format.RGBA8888);
            computePixmap_y = new Pixmap(displayWidth, displayHeight, Format.RGBA8888);
            computePixmap_x.setBlending(Pixmap.Blending.None);
            computePixmap_y.setBlending(Pixmap.Blending.None);
            computeTexture_x = new Texture(computePixmap_x);
            computeTexture_y = new Texture(computePixmap_y);
            
            ShaderProgram.pedantic = false;
            computeShader = new ShaderProgram(Gdx.files.internal("vertex.glsl"), Gdx.files.internal("fragment.glsl"));
            computeShader.bind();
            computeShader.setUniformi("attractorType", attractorType);
            computeShader.setUniformi("u_sampler2D_y", 1);
            computeShader.setUniformf("a", constants_numeric[0]);
            computeShader.setUniformf("b", constants_numeric[1]);
            computeShader.setUniformf("c", constants_numeric[2]);
            computeShader.setUniformf("d", constants_numeric[3]);
            
            computeBatch = new SpriteBatch();
            computeBatch.disableBlending();
            computeBatch.setShader(computeShader);
            
            GPUCurve = new Curve2D(null, 0, 0);
            GPUValues = GPUCurve.points;
            curves.add(GPUCurve);
            
            xValuesGPU_current = new float[allPixels];
            yValuesGPU_current = new float[allPixels];
            for (int i = 0; i < allPixels; i++) {
                xValuesGPU_current[i] = (float) ((random() - 0.5) * 4);
                yValuesGPU_current[i] = (float) ((random() - 0.5) * 4);
            }
        }
    }
    
    void startThreads() {
        if (renderMode.equals(CPU)) {
            for (int i = 0; i < threads; i++) {
                curves.get(i).startThread();
            }
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean finished = false;
                while (!finished) {
                    finished = true;
                    for (int i = 0; i < curves.size; i++) {
                        if (!curves.get(i).finished) {
                            finished = false;
                        }
                    }
                }
                
                double width = 0;
                double height = 0;
                double minX = 100000, minY = 100000;
                
                for (int i = 0; i < curves.size; i++) {
                    for (int p = 0; p < curves.get(i).points.size; p++) {
                        minX = min(minX, curves.get(i).points.get(p).x);
                        minY = min(minY, curves.get(i).points.get(p).y);
                        copyOverThreadProgress = (i * curves.get(0).points.size + p) / (float) (curves.size * curves.get(0).points.size);
                    }
                }
                
                for (int i = 0; i < curves.size; i++) {
                    for (int p = 0; p < curves.get(i).points.size; p++) {
                        curves.get(i).points.get(p).x -= minX < 0 ? minX : -minX;
                        curves.get(i).points.get(p).y -= minY < 0 ? minY : -minY;
                        copyOverThreadProgress = (i * curves.get(0).points.size + p) / (float) (curves.size * curves.get(0).points.size);
                    }
                }
                
                for (int i = 0; i < curves.size; i++) {
                    for (int p = 0; p < curves.get(i).points.size; p++) {
                        width = max(width, curves.get(i).points.get(p).x);
                        height = max(height, curves.get(i).points.get(p).y);
                        copyOverThreadProgress = (i * curves.get(0).points.size + p) / (float) (curves.size * curves.get(0).points.size);
                    }
                }
                
                double tempScaleH = height / (double) HEIGHT;
                double tempScaleW = width / (double) WIDTH;
                double zoom = Math.max(tempScaleH, tempScaleW);
                double xOffset = WIDTH / 2d - width / zoom / 2d;
                double yOffset = HEIGHT / 2d - height / zoom / 2d;
                
                for (int i = 0; i < curves.size; i++) {
                    for (int p = 0; p < curves.get(i).points.size; p++) {
                        curves.get(i).points.get(p).x /= zoom;
                        curves.get(i).points.get(p).y /= zoom;
                        
                        curves.get(i).points.get(p).x += xOffset;
                        curves.get(i).points.get(p).y += yOffset;
                        
                        copyOverThreadProgress = (i * curves.get(0).points.size + p) / (float) (curves.size * curves.get(0).points.size);
                    }
                }
                
                for (int i = 0; i < curves.size; i++) {
                    for (int p = 0; p < curves.get(i).points.size; p++) {
                        
                        int x = (int) curves.get(i).points.get(p).x;
                        int y = (int) curves.get(i).points.get(p).y;
                        
                        intensityMap[x][y]++;
                        
                        for (int xOff = -1; xOff <= 1; ++xOff) {
                            for (int yOff = -1; yOff <= 1; ++yOff) {
                                if (x + xOff >= 0 && x + xOff <= WIDTH && y + yOff >= 0 && y + yOff < HEIGHT) {
                                    intensityMap[x + xOff][y + yOff]++;
                                }
                            }
                        }
                        copyOverThreadProgress = (i * curves.get(0).points.size + p) / (float) (curves.size * curves.get(0).points.size);
                    }
                }
                
                double maxValue = 0;
                for (int x = 0; x <= WIDTH; x++) {
                    for (int y = 0; y <= HEIGHT; y++) {
                        maxValue = max(maxValue, intensityMap[x][y]);
                    }
                }
                
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
    
    void decodeFloatsFromFrameBuffer(boolean y) {
        
        float[] decodedValuesBufferArray;
        if (y) {
            decodedValuesBufferArray = yValuesGPU_current;
        } else {
            decodedValuesBufferArray = xValuesGPU_current;
        }
        
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
            decodedValuesBufferArray[i / 4] = value / divisionPrecision;
        }
    }
    
    void processAndDecodeData(boolean y) {
        computeShader.bind();
        computeShader.setUniformi("processY", y ? 1 : 0);
        
        Gdx.gl.glActiveTexture(GL_TEXTURE1);
        computeTexture_y.bind();
        Gdx.gl.glActiveTexture(GL_TEXTURE0);
        
        computeBatch.begin();
        computeBatch.draw(computeTexture_x, 0, 0);
        computeBatch.end();
        
        decodeFloatsFromFrameBuffer(y);
    }
    
    void processOnGPU() {
        
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
        
        processAndDecodeData(false);
        processAndDecodeData(true);
        
        for (int i = 0; i < xValuesGPU_current.length; i++) {
            if (abs(xValuesGPU_current[i]) > 15) {
                System.exit(0);
            }
            GPUValues.add(new Vector2(xValuesGPU_current[i], yValuesGPU_current[i]));
        }
        computedIterations += allPixels;
    }
    
    void render(SpriteBatch batch, OrthographicCamera camera, ShapeRenderer renderer, float delta) {
        int computedItersNow = computedIterations;
        
        if(renderMode.equals(GPU)){
            if (computedIterations < allIterations) {
                processOnGPU();
                GPUCurve.progress = computedIterations / (float) allIterations;
            } else if (!GPUCurve.finished) {
                GPUCurve.finished = true;
            }
        }
        
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
            
            renderer.setColor(Color.GOLDENROD);
            renderer.rect(-WIDTH / 2f, HEIGHT / 2f - 70, WIDTH * copyOverThreadProgress, 70);
            
            renderer.end();
            batch.begin();
            
            int iterationsPerTick = computedIterations - computedItersNow;
            
            if (computeIterationsPrev.size() < (renderMode.equals(GPU) ? 10 : 150)) {
                computeIterationsPrev.add(iterationsPerTick);
                deltaPrev.add(delta);
            } else {
                for (int i = 0; i < computeIterationsPrev.size() - 1; i++) {
                    computeIterationsPrev.set(i, computeIterationsPrev.get(i + 1));
                    deltaPrev.set(i, deltaPrev.get(i + 1));
                }
                computeIterationsPrev.set(computeIterationsPrev.size() - 1, iterationsPerTick);
                deltaPrev.set(deltaPrev.size() - 1, delta);
            }
            
            float iterationsSmoothed = 0;
            float deltaSmoothed = 0;
            for (int i = 0; i < computeIterationsPrev.size(); i++) {
                iterationsSmoothed += computeIterationsPrev.get(i);
                deltaSmoothed += deltaPrev.get(i);
            }
            iterationsSmoothed /= (float) computeIterationsPrev.size();
            deltaSmoothed /= (float) deltaPrev.size();
            
            font.draw(batch,
                    "KIpS:" + (int) ((iterationsSmoothed / deltaSmoothed) / 1000)
                            + " Delta:" + (int) (deltaSmoothed * 1000) + "ms "
                            + (int) (computedIterations / (float) allIterations * 100) + "%",
                    -WIDTH / 2f + 50, -HEIGHT / 2f + 100);
            batch.end();
        }
    }
}
