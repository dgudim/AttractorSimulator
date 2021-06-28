package com.deo.attractor.Attractors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;
import com.deo.attractor.Utils.MathExpression;
import com.deo.attractor.Utils.Vector4;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import static com.badlogic.gdx.graphics.GL20.GL_TEXTURE0;
import static com.badlogic.gdx.graphics.GL20.GL_TEXTURE1;
import static com.deo.attractor.Attractors.RenderMode.CPU;
import static com.deo.attractor.Attractors.RenderMode.GPU;
import static com.deo.attractor.Launcher.HEIGHT;
import static com.deo.attractor.Launcher.WIDTH;
import static com.deo.attractor.Utils.Utils.fontChars;
import static com.deo.attractor.Utils.Utils.makeAScreenShot;
import static com.deo.attractor.Utils.Utils.rgbToRGBA8888;
import static java.lang.StrictMath.abs;
import static java.lang.StrictMath.max;
import static java.lang.StrictMath.min;
import static java.lang.StrictMath.random;

enum RenderMode {GPU, CPU}

enum AttractorType {DE_JONG, SVENSSON, CLIFFORD, BOO, P_GHOST, PRODUCT, POPCORN, POPCORN2}

enum RuleSet {DEFAULT, PI, PI2, PI3, PI4, PI5, POPCORN_DEFAULT}

enum Palette {DEEP_BLUE, CHEMICAL_GREEN, DARK_PURPLE, FOREST_GREEN, ORANGE, RASPBERRY, PALE_LIME, PALE_PURPLE}

public class Attractor2D {
    
    private final RenderMode renderMode;
    private final AttractorType attractorType;
    private Palette palette;
    float contrast = 0.5f;
    
    Vector4 attractorDimensions;
    Vector2 attractorOffset;
    
    final Array<Curve2D> curves;
    
    int[][] intensityMap;
    int intensityMapMaxValue;
    Pixmap pixmap;
    Texture texture;
    
    float copyOverThreadProgress;
    
    boolean finished = false;
    
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
    
    Attractor2D(RenderMode renderMode, Stage stage, RuleSet ruleSet, AttractorType attractorType, Palette palette, int CPUThreads, int iterationsPerPosition, int startingPositionsPerThread, int GPUIterations, int GPUPointsDivider) {
        
        this.renderMode = renderMode;
        this.attractorType = attractorType;
        this.palette = palette;
        
        allIterations = CPUThreads * iterationsPerPosition * startingPositionsPerThread;
        
        pixmap = new Pixmap(WIDTH + 1, HEIGHT + 1, Format.RGBA8888);
        texture = new Texture(pixmap);
        intensityMap = new int[WIDTH + 1][HEIGHT + 1];
        
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("font.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 31;
        parameter.characters = fontChars;
        font = generator.generateFont(parameter);
        generator.dispose();
        font.getData().markupEnabled = true;
        
        String[] constants;
        float[] constants_numeric;
        String[] bulkArgs = new String[]{"x = 0", "y = 0"};
        switch (ruleSet) {
            case DEFAULT:
            default:
                constants = new String[]{"a = -1.234039", "b = 1.497206", "c = 2.104149", "d = -0.930567"};
                constants_numeric = new float[]{-1.234039f, 1.497206f, 2.104149f, -0.930567f};
                break;
            case PI:
                constants = new String[]{"a = 3.141592", "b = 3.141592", "c = 3.141592", "d = 3.141592"};
                constants_numeric = new float[]{3.141592f, 3.141592f, 3.141592f, 3.141592f};
                break;
            case PI2:
                constants = new String[]{"a = 1.570796", "b = 1.570796", "c = 1.570796", "d = 6.283184"};
                constants_numeric = new float[]{1.570796f, 1.570796f, 1.570796f, 6.283184f};
                break;
            case PI3:
                constants = new String[]{"a = 3.141592", "b = 1.570796", "c = 3.141592", "d = 6.283184"};
                constants_numeric = new float[]{3.141592f, 1.570796f, 3.141592f, 6.283184f};
                break;
            case PI4:
                constants = new String[]{"a = 1.570796", "b = 6.283184", "c = 3.141592", "d = 1.570796"};
                constants_numeric = new float[]{1.570796f, 6.283184f, 3.141592f, 1.570796f};
                break;
            case PI5:
                constants = new String[]{"a = 6.283184", "b = 1.570796", "c = 1.570796", "d = 6.283184"};
                constants_numeric = new float[]{6.283184f, 1.570796f, 1.570796f, 6.283184f};
                break;
            case POPCORN_DEFAULT:
                constants = new String[]{"a = 0.8", "b = 3", "c = 0.8", "d = 3"};
                constants_numeric = new float[]{0.8f, 3, 0.8f, 3};
                break;
        }
        
        attractorOffset = new Vector2();
        
        switch (attractorType) {
            case DE_JONG:
                attractorDimensions = new Vector4(-1.9999981f, -1.9999995f, 1.9889168f, 1.2860358f);
                break;
            case SVENSSON:
                attractorDimensions = new Vector4(-1.9305667f, -2.6320562f, 1.9305667f, 3.1041484f);
                break;
            case CLIFFORD:
                attractorDimensions = new Vector4(-3.1041484f, -1.9305661f, 3.1041484f, 1.9202048f);
                break;
            case BOO:
                attractorDimensions = new Vector4(-1.7690015f, -1.9999906f, 1.9999999f, 1.999998f);
                break;
            case P_GHOST:
                attractorDimensions = new Vector4(-1.7813787f, -1.9999957f, 1.9999976f, 1.286116f);
                break;
            case PRODUCT:
                attractorDimensions = new Vector4(-3.1415918f, -3.1415918f, 3.1415918f, 3.1415918f);
                break;
            case POPCORN:
            case POPCORN2:
                attractorDimensions = new Vector4(-constants_numeric[2], -constants_numeric[0], constants_numeric[2], constants_numeric[0]);
                break;
            default:
                attractorDimensions = Vector4.Zero;
        }
        
        curves = new Array<>();
        if (renderMode.equals(CPU)) {
            for (int i = 0; i < CPUThreads; i++) {
                MathExpression[] simRules_local = new MathExpression[2];
                switch (attractorType) {
                    case DE_JONG:
                    default:
                        simRules_local[0] = new MathExpression("sin(a*y) - cos(b*x)", bulkArgs, constants);
                        simRules_local[1] = new MathExpression("sin(c*x) - cos(d*y)", bulkArgs, constants);
                        break;
                    case SVENSSON:
                        simRules_local[0] = new MathExpression("d * sin(a*x) - sin(b*y)", bulkArgs, constants);
                        simRules_local[1] = new MathExpression("c * cos(a*x) + cos(b*y)", bulkArgs, constants);
                        break;
                    case CLIFFORD:
                        simRules_local[0] = new MathExpression("sin(a*y) + c*cos(a*x)", bulkArgs, constants);
                        simRules_local[1] = new MathExpression("sin(b*x) + d*cos(b*y)", bulkArgs, constants);
                        break;
                    case BOO:
                        simRules_local[0] = new MathExpression("cos(a*y) + cos(b*x)", bulkArgs, constants);
                        simRules_local[1] = new MathExpression("sin(c*x) + sin(d*y)", bulkArgs, constants);
                        break;
                    case P_GHOST:
                        simRules_local[0] = new MathExpression("cos(a*y) - sin(b*x)", bulkArgs, constants);
                        simRules_local[1] = new MathExpression("sin(c*x) - cos(d*y)", bulkArgs, constants);
                        break;
                    case PRODUCT:
                        simRules_local[0] = new MathExpression("3.1415926 * sin(a*y) * cos(b*x)", bulkArgs, constants);
                        simRules_local[1] = new MathExpression("3.1415926 * sin(c*x) * cos(d*y)", bulkArgs, constants);
                        break;
                    case POPCORN:
                        simRules_local[0] = new MathExpression("c * sin(y + tan(d * y))", bulkArgs, constants);
                        simRules_local[1] = new MathExpression("a * sin(x + tan(b * x))", bulkArgs, constants);
                        break;
                    case POPCORN2:
                        simRules_local[0] = new MathExpression("c * sin(y + tan(d * x))", bulkArgs, constants);
                        simRules_local[1] = new MathExpression("a * sin(x + tan(b * y))", bulkArgs, constants);
                        break;
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
            computeShader.setUniformi("ruleSet", attractorType.ordinal());
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
        
        TextureAtlas uiAtlas = new TextureAtlas(Gdx.files.internal("ui.atlas"));
        Skin uiTextures = new Skin();
        uiTextures.addRegions(uiAtlas);
        
        final Slider.SliderStyle sliderStyle = new Slider.SliderStyle();
        sliderStyle.background = uiTextures.getDrawable("progressBarBg");
        sliderStyle.knob = uiTextures.getDrawable("progressBarKnob");
        sliderStyle.knobDown = uiTextures.getDrawable("progressBarKnob_enabled");
        sliderStyle.knobOver = uiTextures.getDrawable("progressBarKnob_over");
        sliderStyle.background.setMinHeight(63);
        sliderStyle.knob.setMinHeight(30);
        sliderStyle.knobDown.setMinHeight(30);
        sliderStyle.knobOver.setMinHeight(30);
        
        final Slider contrastSlider = new Slider(0.01f, 2, 0.01f, false, sliderStyle);
        contrastSlider.setValue(contrast);
        final Label valueText = new Label("Contrast:" + contrast, new Label.LabelStyle(font, Color.WHITE));
        
        contrastSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                contrast = contrastSlider.getValue();
                valueText.setText("Contrast:" + (int) (contrast * 100) / 100f);
                
                updatePixmap();
            }
        });
        
        Table holder = new Table();
        holder.add(contrastSlider).size(500, 25);
        holder.add(valueText);
        holder.align(Align.left);
        holder.setBounds(-WIDTH / 2f + 15, -HEIGHT / 2f + 15, 550, 25);
        stage.addActor(holder);
    
        Pixmap pixmap = new Pixmap(100, 30, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.BLACK);
        pixmap.fill();
        TextureRegionDrawable BarBackgroundBlank = new TextureRegionDrawable(new TextureRegion(new Texture(pixmap)));
        pixmap.dispose();
    
        Pixmap pixmap2 = new Pixmap(100, 30, Pixmap.Format.RGBA8888);
        pixmap2.setColor(Color.valueOf("#000000AA"));
        pixmap2.fill();
        TextureRegionDrawable BarBackgroundGrey = new TextureRegionDrawable(new TextureRegion(new Texture(pixmap2)));
        pixmap2.dispose();
    
        Pixmap pixmap3 = new Pixmap(100, 30, Pixmap.Format.RGBA8888);
        pixmap3.setColor(Color.valueOf("#00000000"));
        pixmap3.fill();
        TextureRegionDrawable BarBackgroundEmpty = new TextureRegionDrawable(new TextureRegion(new Texture(pixmap3)));
        pixmap3.dispose();
        
        SelectBox.SelectBoxStyle selectBoxStyle = new SelectBox.SelectBoxStyle(font, Color.WHITE, BarBackgroundBlank,
                new ScrollPane.ScrollPaneStyle(BarBackgroundGrey, BarBackgroundEmpty, BarBackgroundEmpty, BarBackgroundEmpty, BarBackgroundEmpty),
                new List.ListStyle(font, Color.CORAL, Color.SKY, BarBackgroundGrey));
    
        final SelectBox<Palette> paletteSelector = new SelectBox<>(selectBoxStyle);
        paletteSelector.setItems(Palette.values());
        paletteSelector.setSelected(palette);
        paletteSelector.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Attractor2D.this.palette = paletteSelector.getSelected();
                updatePixmap();
            }
        });
        paletteSelector.setBounds(-WIDTH / 2f + 15, -HEIGHT / 2f + 55, 530, 25);
        stage.addActor(paletteSelector);
    }
    
    void translate(float amountX, float amountY){
        attractorOffset.add(amountX / attractorDimensions.multiplier, amountY / attractorDimensions.multiplier);
        for (int x = 0; x <= WIDTH; x++) {
            for (int y = 0; y <= HEIGHT; y++) {
                intensityMap[x][y] = 0;
            }
        }
        updateIntensityMap();
        updatePixmap();
    }
    
    void zoom(float amount){
        attractorDimensions.changeMultiplier(amount);
        for (int x = 0; x <= WIDTH; x++) {
            for (int y = 0; y <= HEIGHT; y++) {
                intensityMap[x][y] = 0;
            }
        }
        updateIntensityMap();
        updatePixmap();
    }
    
    void updatePixmap() {
        int curveFunc = 0;
        if (Math.abs(contrast - .5) < .04) curveFunc = 1;
        else if (Math.abs(contrast - 1.0) < .04)
            curveFunc = 2;
        
        for (int x = 0; x <= WIDTH; x++) {
            for (int y = 0; y <= HEIGHT; y++) {
                pixmap.drawPixel(x, y, colorFunc((int) (curveFunc(curveFunc, intensityMap[x][y] / (float) intensityMapMaxValue, contrast) * 768), palette));
            }
        }
    }
    
    void updateIntensityMap(){
        float minX = 100000, minY = 100000;
        float maxX = -100000, maxY = -100000;
    
        if (!attractorDimensions.equals(Vector4.Zero)) {
            minX = attractorDimensions.x;
            minY = attractorDimensions.y;
            maxX = attractorDimensions.z;
            maxY = attractorDimensions.w;
        } else {
            for (int i = 0; i < curves.size; i++) {
                for (int p = 0; p < curves.get(i).points.size; p++) {
                
                    minX = min(minX, curves.get(i).points.get(p).x);
                    minY = min(minY, curves.get(i).points.get(p).y);
                
                    maxX = max(maxX, curves.get(i).points.get(p).x);
                    maxY = max(maxY, curves.get(i).points.get(p).y);
                
                    copyOverThreadProgress = (i * curves.get(0).points.size + p) / (float) (curves.size * curves.get(0).points.size);
                }
            }
            attractorDimensions = new Vector4(minX, minY, maxX, maxY);
        }
        float width = maxX - minX;
        float height = maxY - minY;
    
        float zoom = Math.max(height / (float) HEIGHT, width / (float) WIDTH);
        float xOffset = WIDTH / 2f - width / zoom / 2f + attractorOffset.x;
        float yOffset = HEIGHT / 2f - height / zoom / 2f + attractorOffset.y;
    
        for (int i = 0; i < curves.size; i++) {
            for (int p = 0; p < curves.get(i).points.size; p++) {
            
                int translatedX = (int) ((curves.get(i).points.get(p).x - (minX < 0 ? minX : -minX)) / zoom + xOffset);
                int translatedY = (int) ((curves.get(i).points.get(p).y - (minY < 0 ? minY : -minY)) / zoom + yOffset);
            
                try {
                    intensityMap[translatedX][translatedY]++;
                } catch (IndexOutOfBoundsException e) {
                    //ignore
                }
                copyOverThreadProgress = (i * curves.get(0).points.size + p) / (float) (curves.size * curves.get(0).points.size);
            }
        }
        
        for (int x = 0; x <= WIDTH; x++) {
            for (int y = 0; y <= HEIGHT; y++) {
                intensityMapMaxValue = max(intensityMapMaxValue, intensityMap[x][y]);
            }
        }
    }
    
    void startThreads() {
        if (renderMode.equals(CPU)) {
            for (int i = 0; i < curves.size; i++) {
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
                updateIntensityMap();
                updatePixmap();
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
        
        if (renderMode.equals(GPU)) {
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
        if (finished && Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            makeAScreenShot("Attractor2D" + attractorType + "_" + random(), pixmap, false);
        }
        if (!finished) {
            
            renderer.setProjectionMatrix(camera.combined);
            renderer.begin(ShapeRenderer.ShapeType.Filled);
            
            renderer.setColor(Color.BLACK);
            renderer.rect(-WIDTH / 2f, -HEIGHT / 2f, WIDTH, HEIGHT);
            
            float divWidth = min(WIDTH / (float) curves.size, WIDTH / 30f);
            
            for (int i = 0; i < curves.size; i++) {
                renderer.setColor(new Color(1 - curves.get(i).progress, curves.get(i).progress, 0.3f, 1));
                renderer.rect(-WIDTH / 2f + divWidth * i + 10, -HEIGHT / 2f, divWidth - 10, HEIGHT / 4f * curves.get(i).progress);
            }
            
            renderer.setColor(Color.GOLDENROD);
            renderer.rect(-WIDTH / 2f, HEIGHT / 2f - 15, WIDTH * copyOverThreadProgress, 15);
            
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
                    -WIDTH / 2f + 25, -HEIGHT / 2f + 25);
            batch.end();
        }
    }
    
    public static double curveFunc(int type, double value1, double value2) {
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
    
    public static int capTo255(int a) {
        return Math.min(a, 255);
    }
    
    public static int colorFunc(int value, Palette palette) {
        switch (palette) {
            case DEEP_BLUE:
            default:
                return rgbToRGBA8888(capTo255(value), capTo255(value << 1), capTo255(value << 2));
            case CHEMICAL_GREEN:
                return rgbToRGBA8888(capTo255(value), capTo255(value << 2), capTo255(value << 1));
            case DARK_PURPLE:
                return rgbToRGBA8888(capTo255(value << 1), capTo255(value), capTo255(value << 2));
            case FOREST_GREEN:
                return rgbToRGBA8888(capTo255(value << 1), capTo255(value << 2), capTo255(value));
            case ORANGE:
                return rgbToRGBA8888(capTo255(value << 2), capTo255(value << 1), capTo255(value));
            case RASPBERRY:
                return rgbToRGBA8888(capTo255(value << 2), capTo255(value), capTo255(value << 1));
            case PALE_LIME:
                return rgbToRGBA8888(capTo255(value << 1), capTo255(value << 1), capTo255(value));
            case PALE_PURPLE:
                return rgbToRGBA8888(capTo255(value << 1), capTo255(value), capTo255(value << 1));
        }
    }
    
}
