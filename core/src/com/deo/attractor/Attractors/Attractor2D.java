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
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Slider.SliderStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.deo.attractor.Utils.AttractorDimensions;

import java.nio.ByteBuffer;
import java.util.ArrayList;

import static com.badlogic.gdx.graphics.GL20.GL_TEXTURE0;
import static com.badlogic.gdx.graphics.GL20.GL_TEXTURE1;
import static com.deo.attractor.Launcher.HEIGHT;
import static com.deo.attractor.Launcher.WIDTH;
import static com.deo.attractor.Utils.Utils.alphabet;
import static com.deo.attractor.Utils.Utils.fontChars;
import static com.deo.attractor.Utils.Utils.formatNumber;
import static com.deo.attractor.Utils.Utils.makeAScreenShot;
import static com.deo.attractor.Utils.Utils.makeFilledRectangle;
import static com.deo.attractor.Utils.Utils.rgbToRGBA8888;
import static java.lang.StrictMath.max;
import static java.lang.StrictMath.min;
import static java.lang.StrictMath.random;

public class Attractor2D {
    
    private int currentAttractor = 0;
    private int currentPalette = 0;
    
    private final String[] availableAttractors_displayNames;
    private final String[] availableVarSets_displayNames;
    private final String[] availablePalettes_displayNames;
    
    private final String[] availableAttractors;
    private final String[] availableVarSets;
    private final String[] availablePalettes;
    
    private AttractorDimensions attractorDimensions;
    
    float contrast = 0.5f;
    private static final float zoomContrast = 0.35f;
    
    int[][] intensityMap;
    int intensityMapMaxValue = -1;
    
    final float divisionPrecision = 100000000f;
    boolean finished = false;
    float progress;
    boolean rendering = false;
    int FPS = 30;
    int duration = 30;
    int frames = duration * FPS;
    int currentFrame = 0;
    int maxIntensityMapValueForRender = 0;
    long lastZoomTranslateTime;
    boolean needIntensityMapUpdate;
    private final ArrayList<Integer> computeIterationsPrev = new ArrayList<>();
    private final ArrayList<Float> deltaPrev = new ArrayList<>();
    public static int computedIterations;
    public static int allIterations;
    
    float[] constantsMinMax;
    float[] variables;
    float[] startFrom;
    float[] goTo;
    float[] steps;
    
    SliderStyle sliderStyle;
    Stage stage;
    Slider contrastSlider;
    Table contrastTable;
    Slider[] variableSliders;
    Label[] variableSliderLabels;
    
    private final BitmapFont font;
    
    SpriteBatch computeBatch;
    Pixmap computePixmap_x;
    Pixmap computePixmap_y;
    Texture computeTexture_x;
    Texture computeTexture_y;
    ShaderProgram computeShader;
    Pixmap pixmap;
    Texture texture;
    int displayWidth, displayHeight, allPixels;
    
    float[] xValues_current;
    float[] yValues_current;
    Array<Vector2> points;
    
    JsonValue attractorsConfig = new JsonReader().parse(Gdx.files.internal("2DAttractors.json"));
    JsonValue attractors = attractorsConfig.get("Attractors");
    JsonValue ruleSets = attractorsConfig.get("VarSets");
    JsonValue palettes = attractorsConfig.get("Palettes");
    
    Attractor2D(Stage stage, int iterations, int pointsDivider) {
        
        this.stage = stage;
        
        availableAttractors = new String[attractors.size];
        availableAttractors_displayNames = new String[attractors.size];
        for (int i = 0; i < availableAttractors.length; i++) {
            availableAttractors[i] = attractors.get(i).name();
            availableAttractors_displayNames[i] = attractors.get(i).getString("displayName");
        }
        availablePalettes = new String[palettes.size];
        availablePalettes_displayNames = new String[palettes.size];
        for (int i = 0; i < availablePalettes.length; i++) {
            availablePalettes[i] = palettes.get(i).name();
            availablePalettes_displayNames[i] = palettes.get(i).getString("displayName");
        }
        availableVarSets = new String[ruleSets.size];
        availableVarSets_displayNames = new String[ruleSets.size];
        for (int i = 0; i < availableVarSets.length; i++) {
            availableVarSets[i] = ruleSets.get(i).name();
            availableVarSets_displayNames[i] = ruleSets.get(i).getString("displayName");
        }
        
        constantsMinMax = attractorsConfig.get("minMaxLimits").asFloatArray();
        
        startFrom = new float[]{-7, 1.497206f, 2.104149f, -0.930567f};
        goTo = new float[]{0, 1.497206f, 2.104149f, -0.930567f};
        steps = new float[4];
        
        for (int i = 0; i < startFrom.length; i++) {
            steps[i] = (goTo[i] - startFrom[i]) / frames;
        }
        
        pixmap = new Pixmap(WIDTH, HEIGHT, Format.RGBA8888);
        texture = new Texture(pixmap);
        intensityMap = new int[WIDTH][HEIGHT];
        
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("font.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 31;
        parameter.characters = fontChars;
        font = generator.generateFont(parameter);
        generator.dispose();
        font.getData().markupEnabled = true;
        
        displayWidth = Gdx.graphics.getWidth() / pointsDivider;
        displayHeight = Gdx.graphics.getHeight() / pointsDivider;
        allPixels = displayWidth * displayHeight;
        
        allIterations = iterations * allPixels;
        
        computePixmap_x = new Pixmap(displayWidth, displayHeight, Format.RGBA8888);
        computePixmap_y = new Pixmap(displayWidth, displayHeight, Format.RGBA8888);
        computePixmap_x.setBlending(Pixmap.Blending.None);
        computePixmap_y.setBlending(Pixmap.Blending.None);
        computeTexture_x = new Texture(computePixmap_x);
        computeTexture_y = new Texture(computePixmap_y);
        
        ShaderProgram.pedantic = false;
        computeShader = new ShaderProgram(Gdx.files.internal("vertex.glsl").readString(), buildShader());
        
        computeBatch = new SpriteBatch(1000, computeShader);
        computeBatch.disableBlending();
        
        points = new Array<>();
        
        xValues_current = new float[allPixels];
        yValues_current = new float[allPixels];
        
        fillGPUBuffersWithRandomValues();
        
        TextureAtlas uiAtlas = new TextureAtlas(Gdx.files.internal("ui.atlas"));
        Skin uiTextures = new Skin();
        uiTextures.addRegions(uiAtlas);
        
        sliderStyle = new SliderStyle();
        sliderStyle.background = uiTextures.getDrawable("progressBarBg");
        sliderStyle.knob = uiTextures.getDrawable("progressBarKnob");
        sliderStyle.knobDown = uiTextures.getDrawable("progressBarKnob_enabled");
        sliderStyle.knobOver = uiTextures.getDrawable("progressBarKnob_over");
        sliderStyle.background.setMinHeight(63);
        sliderStyle.knob.setMinHeight(30);
        sliderStyle.knobDown.setMinHeight(30);
        sliderStyle.knobOver.setMinHeight(30);
        
        contrastSlider = new Slider(0.01f, 2, 0.01f, false, sliderStyle);
        contrastSlider.setValue(contrast);
        
        final Label valueText = new Label("Contrast:" + contrast, new Label.LabelStyle(font, Color.WHITE));
        
        contrastSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                contrast = contrastSlider.getValue();
                valueText.setText("Contrast:" + formatNumber(2, contrast));
            }
        });
        
        contrastTable = new Table();
        contrastTable.add(contrastSlider).size(500, 25);
        contrastTable.add(valueText);
        contrastTable.align(Align.left);
        contrastTable.setBounds(-WIDTH / 2f + 15, -HEIGHT / 2f + 15, 550, 25);
        stage.addActor(contrastTable);
        
        loadAttractor();
        updateShaderUniforms();
        startThreads();
    }
    
    void reconstructStage() {
        
        stage.clear();
        
        stage.addActor(contrastTable);
        
        variableSliders = new Slider[variables.length];
        variableSliderLabels = new Label[variables.length];
        
        for (int i = 0; i < variables.length; i++) {
            final Slider parameterSlider = new Slider(constantsMinMax[0], constantsMinMax[1], 0.01f, false, sliderStyle);
            variableSliders[i] = parameterSlider;
            parameterSlider.setValue(variables[i]);
            final Label textLabel = new Label("" + formatNumber(2, variables[i]), new Label.LabelStyle(font, Color.WHITE));
            variableSliderLabels[i] = textLabel;
            
            final int finalI = i;
            parameterSlider.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    textLabel.setText("" + formatNumber(2, parameterSlider.getValue()));
                    variables[finalI] = parameterSlider.getValue();
                    reset();
                }
            });
            
            Table table = new Table();
            table.add(textLabel);
            table.add(parameterSlider).size(500, 25);
            table.align(Align.right);
            table.setBounds(WIDTH / 2f - 555, i * 60 - HEIGHT / 2f + 25, 550, 25);
            stage.addActor(table);
        }
        
        TextureRegionDrawable BarBackgroundBlank = makeFilledRectangle(100, 30, Color.BLACK);
        TextureRegionDrawable BarBackgroundGrey = makeFilledRectangle(100, 30, Color.valueOf("#000000AA"));
        TextureRegionDrawable BarBackgroundEmpty = makeFilledRectangle(100, 30, Color.valueOf("#00000000"));
        
        SelectBox.SelectBoxStyle selectBoxStyle = new SelectBox.SelectBoxStyle(font, Color.WHITE, BarBackgroundBlank,
                new ScrollPane.ScrollPaneStyle(BarBackgroundGrey, BarBackgroundEmpty, BarBackgroundEmpty, BarBackgroundEmpty, BarBackgroundEmpty),
                new List.ListStyle(font, Color.CORAL, Color.SKY, BarBackgroundGrey));
        
        final SelectBox<String> paletteSelector = new SelectBox<>(selectBoxStyle);
        paletteSelector.setItems(availablePalettes_displayNames);
        paletteSelector.setSelectedIndex(currentPalette);
        paletteSelector.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                currentPalette = paletteSelector.getSelectedIndex();
            }
        });
        paletteSelector.setBounds(-WIDTH / 2f + 15, -HEIGHT / 2f + 55, 530, 25);
        stage.addActor(paletteSelector);
        
        final SelectBox<String> varSetSelector = new SelectBox<>(selectBoxStyle);
        varSetSelector.setItems(availableVarSets_displayNames);
        varSetSelector.setSelectedIndex(0);
        varSetSelector.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                loadVariablesFromVarSet(availableVarSets[varSetSelector.getSelectedIndex()]);
                for (int i = 0; i < variables.length; i++) {
                    variableSliders[i].setProgrammaticChangeEvents(false);
                    variableSliders[i].setValue(variables[i]);
                    variableSliders[i].setProgrammaticChangeEvents(true);
                    variableSliderLabels[i].setText("" + formatNumber(2, variables[i]));
                }
                reset();
            }
        });
        varSetSelector.setBounds(-WIDTH / 2f + 15, -HEIGHT / 2f + 85, 530, 25);
        stage.addActor(varSetSelector);
        
        final SelectBox<String> attractorTypeSelector = new SelectBox<>(selectBoxStyle);
        attractorTypeSelector.setItems(availableAttractors_displayNames);
        attractorTypeSelector.setSelectedIndex(currentAttractor);
        attractorTypeSelector.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                currentAttractor = attractorTypeSelector.getSelectedIndex();
                loadAttractor();
                reset();
            }
        });
        attractorTypeSelector.setBounds(-WIDTH / 2f + 15, -HEIGHT / 2f + 115, 530, 25);
        stage.addActor(attractorTypeSelector);
        
        TextField.TextFieldStyle textFieldStyle = new TextField.TextFieldStyle();
        textFieldStyle.background = makeFilledRectangle(100, 30, Color.BLACK);
        textFieldStyle.cursor = makeFilledRectangle(3, 30, Color.WHITE);
        textFieldStyle.font = font;
        textFieldStyle.fontColor = Color.LIGHT_GRAY;
        textFieldStyle.focusedFontColor = Color.LIGHT_GRAY;
        final TextField textToRuleConverter = new TextField("", textFieldStyle);
        textToRuleConverter.setBlinkTime(0.57f);
        textToRuleConverter.setMessageText("Enter text here");
        textToRuleConverter.setBounds(-WIDTH / 2f + 15, -HEIGHT / 2f + 145, 530, 25);
        textToRuleConverter.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String text = textToRuleConverter.getText();
                text = text.toUpperCase();
                int cursorLast = textToRuleConverter.getCursorPosition();
                textToRuleConverter.setText(text);
                textToRuleConverter.setCursorPosition(cursorLast);
                float step = (constantsMinMax[1] - constantsMinMax[0]) / (float) (alphabet.length() - 1);
                for (int i = 0; i < min(text.length(), variables.length); i++) {
                    variables[i] = constantsMinMax[0] + alphabet.indexOf(text.charAt(i)) * step;
                }
                for (int i = 0; i < variables.length; i++) {
                    variableSliders[i].setProgrammaticChangeEvents(false);
                    variableSliders[i].setValue(variables[i]);
                    variableSliders[i].setProgrammaticChangeEvents(true);
                    variableSliderLabels[i].setText("" + formatNumber(2, variables[i]));
                }
                reset();
            }
        });
        stage.addActor(textToRuleConverter);
    }
    
    String buildShader() {
        int maxNumberOfVariables = 0;
        for (int i = 0; i < attractors.size; i++) {
            maxNumberOfVariables = max(maxNumberOfVariables, attractors.get(i).getInt("variableCount"));
        }
        
        StringBuilder fragmentShader =
                new StringBuilder("varying vec4 v_color;\n" +
                        "varying vec2 v_texCoord0;\n" +
                        "uniform sampler2D u_sampler2D_x;\n" +
                        "uniform sampler2D u_sampler2D_y;\n" +
                        "const float PI = 3.141592;\n");
        for (int i = 0; i < maxNumberOfVariables; i++) {
            fragmentShader.append("uniform float a").append(i).append(";\n");
        }
        fragmentShader.append(
                "uniform int ruleSet;\n" +
                        "const float Precision = 100000000;\n" +
                        "uniform int processY;\n" +
                        "void main() {\n" +
                        "\n" +
                        "    vec4 pixelColor_x = texture2D(u_sampler2D_x, v_texCoord0);\n" +
                        "    vec4 pixelColor_y = texture2D(u_sampler2D_y, v_texCoord0);\n" +
                        "\n" +
                        "    int valueFromPixel_x = ((int(pixelColor_x.r * 255) << 24) | (int(pixelColor_x.g * 255) << 16) | (int(pixelColor_x.b * 255) << 8) | (int(pixelColor_x.a * 255)));\n" +
                        "    int valueFromPixel_y = ((int(pixelColor_y.r * 255) << 24) | (int(pixelColor_y.g * 255) << 16) | (int(pixelColor_y.b * 255) << 8) | (int(pixelColor_y.a * 255)));\n" +
                        "\n" +
                        "    float x = valueFromPixel_x / Precision;\n" +
                        "    float y = valueFromPixel_y / Precision;\n" +
                        "\n" +
                        "    float newX;\n" +
                        "    float newY;\n" +
                        "\n" +
                        "    int outputPixel;\n" +
                        "\n" +
                        "if (processY == 0){\n" +
                        "switch (ruleSet){\n");
        
        
        for (int i = 0; i < attractors.size; i++) {
            fragmentShader.append("case (").append(i).append("):\n");
            fragmentShader.append("newX = ").append(attractors.get(i).get("rules").asStringArray()[0]).append(";\n");
            fragmentShader.append("break;\n");
        }
        
        fragmentShader.append("}\n" +
                "outputPixel = int(newX * Precision);\n" +
                "} else {\n" +
                "switch (ruleSet){\n");
        
        for (int i = 0; i < attractors.size; i++) {
            fragmentShader.append("case (").append(i).append("):\n");
            fragmentShader.append("newY = ").append(attractors.get(i).get("rules").asStringArray()[1]).append(";\n");
            fragmentShader.append("break;\n");
        }
        
        fragmentShader.append("}\n" +
                "        outputPixel = int(newY * Precision);\n" +
                "    }\n" +
                "\n" +
                "    gl_FragColor = vec4(\n" +
                "    (((outputPixel >> 24) & 0xff))/ 255.0,\n" +
                "    ((outputPixel >> 16) & 0xff) / 255.0,\n" +
                "    ((outputPixel >> 8) & 0xff) / 255.0,\n" +
                "    (((outputPixel >> 0) & 0xff))/ 255.0);\n" +
                "}");
        return fragmentShader.toString();
    }
    
    void reset() {
        clearIntensityMap();
        computedIterations = 0;
        intensityMapMaxValue = 0;
        points.clear();
        finished = false;
        updateShaderUniforms();
        fillGPUBuffersWithRandomValues();
    }
    
    void loadAttractor() {
        if (attractors.get(currentAttractor).get("defaultVariables").isString()) {
            loadVariablesFromVarSet(attractors.get(currentAttractor).getString("defaultVariables"));
        } else {
            variables = attractors.get(currentAttractor).get("defaultVariables").asFloatArray();
        }
        float[] attractorDimensionsArray = attractors.get(currentAttractor).get("dimensions").asFloatArray();
        attractorDimensions = new AttractorDimensions(attractorDimensionsArray);
        contrastSlider.setValue(attractors.get(currentAttractor).getFloat("contrast"));
        reconstructStage();
    }
    
    void loadVariablesFromVarSet(String ruleSet) {
        JsonValue rules = ruleSets.get(ruleSet);
        if (rules.get("variables").isString()) {
            float minLimit = rules.getFloat("minLimit");
            float step = rules.getFloat("step");
            variables = new float[rules.getString("variables").length()];
            for (int i = 0; i < variables.length; i++) {
                variables[i] = minLimit + alphabet.indexOf(rules.getString("variables").charAt(i)) * step;
            }
        } else {
            variables = rules.get("variables").asFloatArray();
        }
    }
    
    void fillGPUBuffersWithRandomValues() {
        for (int i = 0; i < allPixels; i++) {
            xValues_current[i] = (float) ((random() - 0.5) * 4);
            yValues_current[i] = (float) ((random() - 0.5) * 4);
        }
    }
    
    void updateShaderUniforms() {
        computeShader.bind();
        computeShader.setUniformi("ruleSet", currentAttractor);
        computeShader.setUniformi("u_sampler2D_y", 1);
        for (int i = 0; i < variables.length; i++) {
            computeShader.setUniformf("a" + i, variables[i]);
        }
    }
    
    void translateAndZoom(float amountX, float amountY, float amountZ) {
        if (finished) {
            attractorDimensions.translate(amountX, amountY, amountZ);
            updateIntensityMap(3000000);
            needIntensityMapUpdate = true;
            lastZoomTranslateTime = System.nanoTime();
        }
    }
    
    void updatePixmap() {
        
        float contrast = needIntensityMapUpdate ? zoomContrast : this.contrast;
        
        int curveFunc = 0;
        if (Math.abs(contrast - .5) < .04) curveFunc = 1;
        else if (Math.abs(contrast - 1.0) < .04)
            curveFunc = 2;
        
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                pixmap.drawPixel(x, y, colorFunc((int) (curveFunc(curveFunc, intensityMap[x][y] / (float) intensityMapMaxValue, contrast) * 768)));
            }
        }
    }
    
    void getAttractorDimensions() {
        float minX = 100000, minY = 100000;
        float maxX = -100000, maxY = -100000;
        for (int p = 0; p < points.size; p++) {
            minX = min(minX, points.get(p).x);
            minY = min(minY, points.get(p).y);
            
            maxX = max(maxX, points.get(p).x);
            maxY = max(maxY, points.get(p).y);
        }
        attractorDimensions = new AttractorDimensions(minX, minY, maxX, maxY);
    }
    
    void plotPoints(float[] pointsX, float[] pointsY) {
        for (int i = 0; i < pointsX.length; i++) {
            translateAndPlotToIntensityMap(pointsX[i], pointsY[i]);
        }
        updateIntensityMapMaxValue(false);
    }
    
    void translateAndPlotToIntensityMap(float x, float y) {
        try {
            int translatedX = (int) ((x - attractorDimensions.minX) / attractorDimensions.zoom + attractorDimensions.offsetX);
            int translatedY = (int) ((y - attractorDimensions.minY) / attractorDimensions.zoom + attractorDimensions.offsetY);
            intensityMap[translatedX][translatedY]++;
        } catch (IndexOutOfBoundsException e) {
            //ignore
        }
    }
    
    void updateIntensityMapMaxValue(boolean resetToZero) {
        if (!rendering) {
            if (resetToZero) {
                intensityMapMaxValue = 0;
            }
            for (int x = 0; x < WIDTH; x++) {
                for (int y = 0; y < HEIGHT; y++) {
                    intensityMapMaxValue = max(intensityMapMaxValue, intensityMap[x][y]);
                }
            }
        } else {
            intensityMapMaxValue = maxIntensityMapValueForRender;
        }
    }
    
    void clearIntensityMap() {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                intensityMap[x][y] = 0;
            }
        }
    }
    
    void updateIntensityMap(int points) {
        
        clearIntensityMap();
        if (points == -1) {
            points = this.points.size;
        }
        for (int p = 0; p < min(points, this.points.size); p++) {
            translateAndPlotToIntensityMap(this.points.get(p).x, this.points.get(p).y);
        }
        updateIntensityMapMaxValue(points == -1);
    }
    
    void startThreads() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    updatePixmap();
                }
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (needIntensityMapUpdate && System.nanoTime() - lastZoomTranslateTime > 1900000000L) {
                        updateIntensityMap(-1);
                        needIntensityMapUpdate = false;
                    }
                    if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) {
                        getAttractorDimensions();
                        updateIntensityMap(3000000);
                        needIntensityMapUpdate = true;
                    }
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    if (finished) {
                        
                        break;
                    }
                }
            }
        }).start();
    }
    
    void decodeFloatsFromFrameBuffer(boolean y) {
        
        float[] decodedValuesBufferArray;
        if (y) {
            decodedValuesBufferArray = yValues_current;
        } else {
            decodedValuesBufferArray = xValues_current;
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
                computePixmap_x.drawPixel(x, y, (int) (xValues_current[x + y * displayWidth] * divisionPrecision));
            }
        }
        for (int x = 0; x < displayWidth; x++) {
            for (int y = 0; y < displayHeight; y++) {
                computePixmap_y.drawPixel(x, y, (int) (yValues_current[x + y * displayWidth] * divisionPrecision));
            }
        }
        computeTexture_x.draw(computePixmap_x, 0, 0);
        computeTexture_y.draw(computePixmap_y, 0, 0);
        
        processAndDecodeData(false);
        processAndDecodeData(true);
        
        for (int i = 0; i < xValues_current.length; i++) {
            points.add(new Vector2(xValues_current[i], yValues_current[i]));
        }
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                plotPoints(xValues_current, yValues_current);
            }
        }).start();
        
        computedIterations += allPixels;
    }
    
    void render(SpriteBatch batch, OrthographicCamera camera, ShapeRenderer renderer, float delta) {
        int computedItersNow = computedIterations;
        boolean makeAScreenShotAndReset = false;
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) {
            rendering = true;
            currentFrame = 0;
            maxIntensityMapValueForRender = intensityMapMaxValue;
            variables = startFrom.clone();
            reset();
        }
        
        if (computedIterations < allIterations) {
            processOnGPU();
            progress = computedIterations / (float) allIterations;
        } else if (!finished) {
            finished = true;
            if (rendering) {
                if (currentFrame + 1 == frames) {
                    rendering = false;
                } else {
                    makeAScreenShotAndReset = true;
                }
            }
        }
        
        if (!finished) {
            renderer.begin(ShapeRenderer.ShapeType.Filled);
            renderer.setColor(Color.BLACK);
            renderer.rect(-WIDTH / 2f, -HEIGHT / 2f, WIDTH, HEIGHT);
            renderer.end();
        }
        
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        texture.draw(pixmap, 0, 0);
        batch.draw(texture, -WIDTH / 2f, -HEIGHT / 2f);
        batch.end();
        if (finished && Gdx.input.isKeyJustPressed(Input.Keys.PRINT_SCREEN)) {
            makeAScreenShot("Attractor2D" + currentAttractor + "_" + random(), pixmap, false);
        }
        if (makeAScreenShotAndReset) {
            makeAScreenShot("frame" + currentFrame, pixmap, false);
            for (int i = 0; i < variables.length; i++) {
                variables[i] += steps[i];
            }
            currentFrame++;
            reset();
        }
        if (!finished) {
            
            batch.begin();
            
            int iterationsPerTick = computedIterations - computedItersNow;
            
            if (computeIterationsPrev.size() < 10) {
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
            
            font.setColor(Color.WHITE);
            font.draw(batch,
                    "KIpS:" + (int) ((iterationsSmoothed / deltaSmoothed) / 1000)
                            + " Delta:" + (int) (deltaSmoothed * 1000) + "ms "
                            + (int) (computedIterations / (float) allIterations * 100) + "%",
                    -WIDTH / 2f + 25, HEIGHT / 2f - 25);
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
    
    public int colorFunc(int value) {
        float[] rgbMultipliers = palettes.get(currentPalette).get("rgbMultipliers").asFloatArray();
        return rgbToRGBA8888(
                capTo255((int) (value * rgbMultipliers[0])),
                capTo255((int) (value * rgbMultipliers[1])),
                capTo255((int) (value * rgbMultipliers[2])));
    }
    
}
