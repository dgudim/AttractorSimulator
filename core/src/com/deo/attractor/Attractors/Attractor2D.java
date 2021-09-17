package com.deo.attractor.Attractors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Pixmap.Format;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.deo.attractor.Utils.AttractorDimensions;
import com.deo.attractor.Utils.UIUtils;

import java.nio.ByteBuffer;

import static com.badlogic.gdx.graphics.GL20.GL_TEXTURE0;
import static com.badlogic.gdx.graphics.GL20.GL_TEXTURE1;
import static com.badlogic.gdx.math.MathUtils.clamp;
import static com.badlogic.gdx.math.MathUtils.nanoToSec;
import static com.deo.attractor.Launcher.HEIGHT;
import static com.deo.attractor.Launcher.WIDTH;
import static com.deo.attractor.Utils.Utils.alphabet;
import static com.deo.attractor.Utils.Utils.formatNumber;
import static com.deo.attractor.Utils.Utils.makeAScreenShot;
import static com.deo.attractor.Utils.Utils.rgbToRGBA8888;
import static java.lang.Math.abs;
import static java.lang.StrictMath.max;
import static java.lang.StrictMath.min;
import static java.lang.StrictMath.random;

public class Attractor2D {
    
    private int currentAttractor = 0;
    private int currentPalette = 0;
    
    private final String[] availableAttractors_displayNames;
    private final String[] availableVarSets_displayNames;
    private final String[] availableVarSets;
    
    private AttractorDimensions attractorDimensions;
    
    private float contrast = 0.5f;
    private static final float zoomContrast = 0.35f;
    
    private final int[][] intensityMap;
    private int intensityMapMaxValue = -1;
    private int maxIntensityMapValueManual = 0;
    private boolean useManualMaxValue = false;
    
    private final float divisionPrecision = 100000000f;
    private boolean finished = false;
    boolean showUI = true;
    private boolean rendering = false;
    private final Array<Float> secondsPerFrameSmoothed = new Array<>();
    private long lastFrameTimestamp;
    private int FPS = 30;
    private int duration = 30;
    private int frames = duration * FPS;
    private int currentFrame = 0;
    private volatile long lastZoomTranslateTime;
    private volatile boolean needIntensityMapUpdate;
    private final Array<Integer> computeIterationsPrev = new Array<>();
    private final Array<Float> deltaPrev = new Array<>();
    private int computedIterations;
    private final int allIterations;
    private float iterationCutOff = 0.99f;
    private float[] rgbMultipliers;
    
    private double[] variables;
    private double[] startFrom = new double[]{};
    private double[] goTo = new double[]{};
    private double[] steps;
    
    private final Stage stage;
    private final Slider contrastSlider;
    private final Label renderInfo;
    private final Group uiGroup;
    private Slider[] variableSliders;
    private final SpriteBatch computeBatch;
    private final Pixmap computePixmap_x;
    private final Pixmap computePixmap_y;
    private final Texture computeTexture_x;
    private final Texture computeTexture_y;
    private final ShaderProgram computeShader;
    private final Texture texture;
    private final Pixmap pixmap;
    private final int displayWidth;
    private final int displayHeight;
    private final int allPixels;
    private final UIUtils uiUtils;
    
    private final float[] xValues_current;
    private final float[] yValues_current;
    private final Array<Vector2> points;
    
    private final JsonValue attractorsConfig = new JsonReader().parse(Gdx.files.internal("2DAttractors.json"));
    private final JsonValue attractors = attractorsConfig.get("Attractors");
    private final JsonValue varSets = attractorsConfig.get("VarSets");
    private final JsonValue palettes = attractorsConfig.get("Palettes");
    
    Attractor2D(Stage stage, int iterations, int pointsDivider) {
        
        uiUtils = new UIUtils();
        this.stage = stage;
        
        availableAttractors_displayNames = new String[attractors.size];
        for (int i = 0; i < availableAttractors_displayNames.length; i++) {
            availableAttractors_displayNames[i] = attractors.get(i).getString("displayName");
        }
        String[] availablePalettes_displayNames = new String[palettes.size];
        for (int i = 0; i < availablePalettes_displayNames.length; i++) {
            availablePalettes_displayNames[i] = palettes.get(i).getString("displayName");
        }
        availableVarSets = new String[varSets.size];
        availableVarSets_displayNames = new String[varSets.size];
        for (int i = 0; i < availableVarSets.length; i++) {
            availableVarSets[i] = varSets.get(i).name();
            availableVarSets_displayNames[i] = varSets.get(i).getString("displayName");
        }
        
        pixmap = new Pixmap(WIDTH, HEIGHT, Format.RGBA8888);
        texture = new Texture(pixmap);
        intensityMap = new int[WIDTH][HEIGHT];
        
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
        
        contrastSlider = new Slider(0.01f, 2, 0.01f, false, uiUtils.sliderStyle);
        contrastSlider.setValue(contrast);
        
        final Label valueText = new Label("Contrast:" + contrast, uiUtils.labelStyle);
        
        contrastSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                contrast = contrastSlider.getValue();
                valueText.setText("Contrast:" + formatNumber(2, contrast));
            }
        });
        
        uiGroup = new Group();
        
        final Slider iterationCutOffSlider = uiUtils.addSliderWithEditableLabel(new float[]{0.1f, 1}, iterationCutOff,
                new DigitFilter(false, true), null, stage, uiGroup, -WIDTH / 2f + 630, -HEIGHT / 2f + 235, 500, 25);
        rgbMultipliers = palettes.get(currentPalette).get("rgbMultipliers").asFloatArray();
        final Slider redValueSlider = uiUtils.addSliderWithEditableLabel(new float[]{0, 6}, rgbMultipliers[0],
                new DigitFilter(false, true), null, stage, uiGroup, -WIDTH / 2f + 630, -HEIGHT / 2f + 185, 500, 25);
        final Slider greenValueSlider = uiUtils.addSliderWithEditableLabel(new float[]{0, 6}, rgbMultipliers[1],
                new DigitFilter(false, true), null, stage, uiGroup, -WIDTH / 2f + 630, -HEIGHT / 2f + 135, 500, 25);
        final Slider blueValueSlider = uiUtils.addSliderWithEditableLabel(new float[]{0, 6}, rgbMultipliers[2],
                new DigitFilter(false, true), null, stage, uiGroup, -WIDTH / 2f + 630, -HEIGHT / 2f + 85, 500, 25);
        iterationCutOffSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                iterationCutOff = iterationCutOffSlider.getValue();
                reset();
            }
        });
        redValueSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                rgbMultipliers[0] = redValueSlider.getValue();
            }
        });
        greenValueSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                rgbMultipliers[1] = greenValueSlider.getValue();
            }
        });
        blueValueSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                rgbMultipliers[2] = blueValueSlider.getValue();
            }
        });
        iterationCutOffSlider.setVisible(false);
        redValueSlider.setVisible(false);
        greenValueSlider.setVisible(false);
        blueValueSlider.setVisible(false);
        Button plusButton = new Button(uiUtils.plusButtonStyle);
        plusButton.setBounds(-WIDTH / 2f + 450, -HEIGHT / 2f + 50, 35, 35);
        plusButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                iterationCutOffSlider.setVisible(!iterationCutOffSlider.isVisible());
                redValueSlider.setVisible(!redValueSlider.isVisible());
                greenValueSlider.setVisible(!greenValueSlider.isVisible());
                blueValueSlider.setVisible(!blueValueSlider.isVisible());
            }
        });
        
        final SelectBox<String> paletteSelector = new SelectBox<>(uiUtils.selectBoxStyle);
        paletteSelector.setItems(availablePalettes_displayNames);
        paletteSelector.setSelectedIndex(currentPalette);
        paletteSelector.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                currentPalette = paletteSelector.getSelectedIndex();
                loadColorPalette(redValueSlider, greenValueSlider, blueValueSlider);
            }
        });
        paletteSelector.setBounds(-WIDTH / 2f + 15, -HEIGHT / 2f + 55, 530, 25);
        
        Table contrastSliderTable = new Table();
        contrastSliderTable.add(contrastSlider).size(500, 25);
        contrastSliderTable.add(valueText);
        contrastSliderTable.align(Align.left);
        contrastSliderTable.setBounds(-WIDTH / 2f + 15, -HEIGHT / 2f + 15, 550, 25);
        
        final TextField textToRuleConverter = new TextField("", uiUtils.textFieldStyle);
        textToRuleConverter.setBlinkTime(0.57f);
        textToRuleConverter.setMessageText("Text to rule converter");
        textToRuleConverter.setTextFieldFilter(new AlphabetFilter());
        textToRuleConverter.setBounds(-WIDTH / 2f + 15, -HEIGHT / 2f + 145, 530, 25);
        textToRuleConverter.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String text = textToRuleConverter.getText();
                text = text.toUpperCase();
                int cursorLast = textToRuleConverter.getCursorPosition();
                textToRuleConverter.setText(text);
                textToRuleConverter.setCursorPosition(cursorLast);
                float[] varMinMax = attractors.get(currentAttractor).get("variableLimits").asFloatArray();
                boolean useZ = attractors.get(currentAttractor).getBoolean("useZInTextInterpretation");
                float step = (varMinMax[1] - varMinMax[0]) / (float) (alphabet.length() - 1 - (useZ ? 0 : 1));
                for (int i = 0; i < min(text.length(), variables.length); i++) {
                    if (alphabet.indexOf(text.charAt(i)) == -1) {
                        variables[i] = 0;
                    } else {
                        variables[i] = varMinMax[0] + alphabet.indexOf(text.charAt(i)) * step;
                    }
                }
                updateSliderValues();
                reset();
            }
        });
        
        final TextField manualMaxValueInput = new TextField("", uiUtils.textFieldStyle);
        manualMaxValueInput.setTextFieldFilter(new DigitFilter(false, false));
        manualMaxValueInput.setText("7500");
        manualMaxValueInput.setBounds(-WIDTH / 2f + 20, -HEIGHT / 2f + 225, 415, 50);
        manualMaxValueInput.setDisabled(true);
        manualMaxValueInput.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                try {
                    maxIntensityMapValueManual = Integer.parseInt(manualMaxValueInput.getText());
                    if (maxIntensityMapValueManual < 1 || maxIntensityMapValueManual > Integer.MAX_VALUE / 2) {
                        maxIntensityMapValueManual = clamp(Integer.parseInt(manualMaxValueInput.getText()), 1, Integer.MAX_VALUE / 2);
                        int cursorBefore = manualMaxValueInput.getCursorPosition();
                        manualMaxValueInput.setText("" + maxIntensityMapValueManual);
                        manualMaxValueInput.setCursorPosition(cursorBefore);
                    }
                } catch (NumberFormatException numberFormatException) {
                    maxIntensityMapValueManual = 1;
                }
                updateIntensityMapMaxValue(false);
            }
        });
        
        final CheckBox useManualMaxValueCheckBox = new CheckBox("Use manual maxValue", uiUtils.checkBoxStyle);
        useManualMaxValueCheckBox.setPosition(-WIDTH / 2f + 15, -HEIGHT / 2f + 175);
        useManualMaxValueCheckBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                useManualMaxValue = useManualMaxValueCheckBox.isChecked();
                manualMaxValueInput.setDisabled(!useManualMaxValue);
                if (useManualMaxValueCheckBox.isChecked()) {
                    manualMaxValueInput.setText("" + intensityMapMaxValue);
                    maxIntensityMapValueManual = intensityMapMaxValue;
                }
                updateIntensityMapMaxValue(true);
            }
        });
        
        final TextButton startRenderButton = new TextButton("Render", uiUtils.textButtonStyle);
        TextButton setAsFirstPosition = new TextButton("Set as first position", uiUtils.textButtonStyle);
        TextButton setAsSecondPosition = new TextButton("Set as second position", uiUtils.textButtonStyle);
        final TextField FPSTextField = new TextField("" + FPS, uiUtils.textFieldStyle);
        final TextField durationTextField = new TextField("" + duration, uiUtils.textFieldStyle);
        renderInfo = new Label("", uiUtils.labelStyle);
        Label FPSLabel = new Label("FPS:", uiUtils.labelStyle);
        Label durationLabel = new Label("Duration:", uiUtils.labelStyle);
        Label secondsLabel = new Label("s", uiUtils.labelStyle);
        
        FPSLabel.setAlignment(Align.right);
        FPSTextField.setAlignment(Align.left);
        durationTextField.setAlignment(Align.left);
        
        final Table renderMenuTable = new Table();
        Table FPSAndDurationTable = new Table();
        FPSAndDurationTable.add(FPSLabel).width(160);
        FPSAndDurationTable.add(FPSTextField).width(110).row();
        FPSAndDurationTable.add(durationLabel).width(160);
        FPSAndDurationTable.add(durationTextField).width(110);
        FPSAndDurationTable.add(secondsLabel).row();
        Table buttonsTable = new Table();
        buttonsTable.add(setAsFirstPosition).align(Align.center).width(445).pad(1).row();
        buttonsTable.add(setAsSecondPosition).align(Align.center).width(445).pad(1).row();
        buttonsTable.add(startRenderButton).align(Align.center).width(130).pad(1).row();
        renderMenuTable.add(FPSAndDurationTable).width(300).row();
        renderMenuTable.add(buttonsTable).width(300).row();
        renderMenuTable.add(renderInfo);
        updateRenderInfo();
        renderMenuTable.setPosition(-WIDTH / 2f + 250, 100);
        renderMenuTable.setTouchable(Touchable.disabled);
        renderMenuTable.setVisible(false);
        
        startRenderButton.setTouchable(Touchable.disabled);
        startRenderButton.setColor(Color.LIGHT_GRAY);
        
        FPSTextField.setTextFieldFilter(new DigitFilter(false, false));
        FPSTextField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                try {
                    FPS = Integer.parseInt(FPSTextField.getText());
                    if (FPS < 1 || FPS > 240) {
                        FPS = clamp(Integer.parseInt(FPSTextField.getText()), 1, 240);
                        int cursorBefore = FPSTextField.getCursorPosition();
                        FPSTextField.setText("" + FPS);
                        FPSTextField.setCursorPosition(cursorBefore);
                    }
                    frames = FPS * duration;
                    updateRenderInfo();
                } catch (NumberFormatException numberFormatException) {
                    FPS = 1;
                    frames = duration;
                }
            }
        });
        
        durationTextField.setTextFieldFilter(new DigitFilter(false, false));
        durationTextField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                try {
                    duration = Integer.parseInt(durationTextField.getText());
                    if (duration < 1 || duration > 10000) {
                        duration = clamp(Integer.parseInt(durationTextField.getText()), 1, 10000);
                        int cursorBefore = durationTextField.getCursorPosition();
                        durationTextField.setText("" + duration);
                        durationTextField.setCursorPosition(cursorBefore);
                    }
                    frames = FPS * duration;
                    updateRenderInfo();
                } catch (NumberFormatException numberFormatException) {
                    duration = 1;
                    frames = FPS;
                }
            }
        });
        
        setAsFirstPosition.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                copyVarsToRenderPosition(1, startRenderButton);
            }
        });
        
        setAsSecondPosition.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                copyVarsToRenderPosition(2, startRenderButton);
            }
        });
        
        startRenderButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                Gdx.app.postRunnable(new Runnable() {
                    @Override
                    public void run() {
                        steps = new double[startFrom.length];
                        for (int i = 0; i < startFrom.length; i++) {
                            steps[i] = (goTo[i] - startFrom[i]) / (double) frames;
                        }
                        rendering = true;
                        currentFrame = 0;
                        variables = startFrom.clone();
                        lastFrameTimestamp = System.nanoTime();
                        updateSliderValues();
                        reset();
                    }
                });
            }
        });
        
        final CheckBox renderCheckBox = new CheckBox("Show render menu", uiUtils.checkBoxStyle);
        renderCheckBox.setPosition(-WIDTH / 2f + 15, -HEIGHT / 2f + 280);
        renderCheckBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                renderMenuTable.setVisible(renderCheckBox.isChecked());
                renderMenuTable.setTouchable(renderCheckBox.isChecked() ? Touchable.enabled : Touchable.disabled);
            }
        });
        
        final SelectBox<String> attractorTypeSelector = new SelectBox<>(uiUtils.selectBoxStyle);
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
        
        uiGroup.addActor(paletteSelector);
        uiGroup.addActor(plusButton);
        uiGroup.addActor(contrastSliderTable);
        uiGroup.addActor(textToRuleConverter);
        uiGroup.addActor(attractorTypeSelector);
        uiGroup.addActor(useManualMaxValueCheckBox);
        uiGroup.addActor(manualMaxValueInput);
        uiGroup.addActor(renderCheckBox);
        uiGroup.addActor(renderMenuTable);
        
        stage.addActor(uiGroup);
        
        loadAttractor();
        updateShaderUniforms();
        startThreads();
    }
    
    void copyVarsToRenderPosition(int position, TextButton startRenderButton) {
        if (position == 1) {
            startFrom = variables.clone();
        } else {
            goTo = variables.clone();
        }
        startRenderButton.setDisabled(startFrom.length != goTo.length);
        startRenderButton.setTouchable(startFrom.length == goTo.length ? Touchable.enabled : Touchable.disabled);
        startRenderButton.setColor(startFrom.length == goTo.length ? Color.WHITE : Color.LIGHT_GRAY);
        updateRenderInfo();
    }
    
    void updateRenderInfo() {
        renderInfo.setText(
                "Current frame:" + currentFrame + "/" + frames + "\n"
                        + "Starting position:" + startFrom.length + "vars\n"
                        + "Ending position:" + goTo.length + "vars\n"
                        + "Seconds per frame:" + calculateSecondsPerFrame() + "\n"
                        + "Minutes left:" + formatNumber(2, calculateTimeLeft() / 60f) + "\n"
                        + "Biggest step:" + getBiggestParameterStep());
    }
    
    float getBiggestParameterStep() {
        double maxVal = 0;
        for (int i = 0; i < min(startFrom.length, goTo.length); i++) {
            maxVal = max(maxVal, abs((goTo[i] - startFrom[i]) / (double) frames));
        }
        return formatNumber(7, maxVal);
    }
    
    float calculateTimeLeft() {
        return calculateSecondsPerFrame() * (frames - currentFrame);
    }
    
    float calculateSecondsPerFrame() {
        float sum = 0;
        for (int i = 0; i < secondsPerFrameSmoothed.size; i++) {
            sum += secondsPerFrameSmoothed.get(i);
        }
        return formatNumber(2, sum / (float) secondsPerFrameSmoothed.size);
    }
    
    void reconstructStage() {
        
        stage.clear();
        stage.addActor(uiGroup);
        
        variableSliders = new Slider[variables.length];
        
        for (int i = 0; i < variables.length; i++) {
            final int finalI = i;
            variableSliders[i] = uiUtils.addSliderWithEditableLabel(
                    attractors.get(currentAttractor).get("variableLimits").asFloatArray(),
                    (float) variables[i], new DigitFilter(true, true), new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                            reset();
                        }
                    }, stage, null,
                    WIDTH / 2f - 555, i * 60 - HEIGHT / 2f + 25, 500, 25);
            variableSliders[i].addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (!rendering) {
                        variables[finalI] = variableSliders[finalI].getValue();
                    }
                    if (variableSliders[finalI].isDragging()) {
                        reset();
                    }
                }
            });
        }
        
        int attractorVarCount = attractors.get(currentAttractor).getInt("variableCount");
        String attractorVarSet = attractors.get(currentAttractor).get("defaultVariables").isString() ? attractors.get(currentAttractor).getString("defaultVariables") : "";
        int attractorVarSetIndex = 0;
        final Array<Integer> varSetIndexesForAttractor = new Array<>();
        for (int i = 0; i < varSets.size; i++) {
            if (varSets.get(i).getInt("variableCount") == attractorVarCount) {
                varSetIndexesForAttractor.add(i);
            }
        }
        final String[] varSetNamesForAttractor = new String[varSetIndexesForAttractor.size];
        for (int i = 0; i < varSetIndexesForAttractor.size; i++) {
            if (availableVarSets[varSetIndexesForAttractor.get(i)].equals(attractorVarSet)) {
                attractorVarSetIndex = i;
            }
            varSetNamesForAttractor[i] = availableVarSets_displayNames[varSetIndexesForAttractor.get(i)];
        }
        
        final SelectBox<String> varSetSelector = new SelectBox<>(uiUtils.selectBoxStyle);
        varSetSelector.setItems(varSetNamesForAttractor);
        varSetSelector.setSelectedIndex(attractorVarSetIndex);
        varSetSelector.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                loadVariablesFromVarSet(availableVarSets[varSetIndexesForAttractor.get(varSetSelector.getSelectedIndex())]);
                updateSliderValues();
                reset();
            }
        });
        varSetSelector.setBounds(-WIDTH / 2f + 15, -HEIGHT / 2f + 85, 530, 25);
        stage.addActor(varSetSelector);
    }
    
    void updateSliderValues() {
        for (int i = 0; i < variables.length; i++) {
            variableSliders[i].setValue((float) variables[i]);
        }
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
                        "uniform sampler2D u_sampler2D_y;\n");
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
        updateIntensityMapMaxValue(true);
        points.clear();
        finished = false;
        updateShaderUniforms();
        fillGPUBuffersWithRandomValues();
    }
    
    void loadAttractor() {
        if (attractors.get(currentAttractor).get("defaultVariables").isString()) {
            loadVariablesFromVarSet(attractors.get(currentAttractor).getString("defaultVariables"));
        } else {
            variables = attractors.get(currentAttractor).get("defaultVariables").asDoubleArray();
        }
        float[] attractorDimensionsArray = attractors.get(currentAttractor).get("dimensions").asFloatArray();
        attractorDimensions = new AttractorDimensions(attractorDimensionsArray);
        contrastSlider.setValue(attractors.get(currentAttractor).getFloat("contrast"));
        reconstructStage();
    }
    
    void loadVariablesFromVarSet(String ruleSet) {
        JsonValue rules = varSets.get(ruleSet);
        if (rules.get("variables").isString()) {
            float minLimit = rules.getFloat("minLimit");
            float step = rules.getFloat("step");
            variables = new double[rules.getString("variables").length()];
            for (int i = 0; i < variables.length; i++) {
                variables[i] = minLimit + alphabet.indexOf(rules.getString("variables").charAt(i)) * step;
            }
        } else {
            variables = rules.get("variables").asDoubleArray();
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
            computeShader.setUniformf("a" + i, (float) variables[i]);
        }
    }
    
    void translateAndZoom(float amountX, float amountY, float amountZ) {
        if (finished) {
            float translationMultiplier = (float) clamp(attractorDimensions.multiplier, 0.35, 1);
            attractorDimensions.translate(amountX * translationMultiplier, amountY * translationMultiplier, amountZ * translationMultiplier);
            updateIntensityMap(3000000);
            needIntensityMapUpdate = true;
            lastZoomTranslateTime = System.nanoTime();
        }
    }
    
    void updatePixmap() {
        
        float contrast = needIntensityMapUpdate ? zoomContrast : this.contrast;
        
        int curveFunc = 0;
        if (abs(contrast - .5) < .04) curveFunc = 1;
        else if (abs(contrast - 1.0) < .04)
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
        if (!useManualMaxValue) {
            if (resetToZero) {
                intensityMapMaxValue = 0;
            }
            for (int x = 0; x < WIDTH; x++) {
                for (int y = 0; y < HEIGHT; y++) {
                    intensityMapMaxValue = max(intensityMapMaxValue, intensityMap[x][y]);
                }
            }
        } else {
            intensityMapMaxValue = maxIntensityMapValueManual;
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
        
        if (computedIterations >= allIterations * (1 - iterationCutOff)) {
            for (int i = 0; i < xValues_current.length; i++) {
                points.add(new Vector2(xValues_current[i], yValues_current[i]));
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    plotPoints(xValues_current, yValues_current);
                }
            }).start();
        }
        
        computedIterations += allPixels;
    }
    
    void render(SpriteBatch batch, OrthographicCamera camera, ShapeRenderer renderer, float delta) {
        int computedItersNow = computedIterations;
        boolean makeAScreenShotAndReset = false;
        
        if (computedIterations < allIterations) {
            processOnGPU();
        } else if (!finished) {
            finished = true;
            if (rendering) {
                if (currentFrame == frames) {
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
        if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) {
            showUI = !showUI;
        }
        if (finished && Gdx.input.isKeyJustPressed(Input.Keys.PRINT_SCREEN)) {
            makeAScreenShot("Attractor2D" + availableAttractors_displayNames[currentAttractor] + "_" + random(), pixmap, false);
        }
        if (makeAScreenShotAndReset) {
            makeAScreenShot("frame" + currentFrame, pixmap, false);
            for (int i = 0; i < variables.length; i++) {
                variables[i] += steps[i];
            }
            currentFrame++;
            reset();
            float currentFrameTime = nanoToSec * (System.nanoTime() - lastFrameTimestamp);
            if (secondsPerFrameSmoothed.size < 10) {
                secondsPerFrameSmoothed.add(currentFrameTime);
            } else {
                for (int i = 0; i < secondsPerFrameSmoothed.size - 1; i++) {
                    secondsPerFrameSmoothed.set(i, secondsPerFrameSmoothed.get(i + 1));
                }
                secondsPerFrameSmoothed.set(secondsPerFrameSmoothed.size - 1, currentFrameTime);
            }
            lastFrameTimestamp = System.nanoTime();
            updateRenderInfo();
            updateSliderValues();
        }
        
        batch.begin();
        
        int iterationsPerTick = computedIterations - computedItersNow;
        
        if (computeIterationsPrev.size < 10) {
            computeIterationsPrev.add(iterationsPerTick);
            deltaPrev.add(delta);
        } else {
            for (int i = 0; i < computeIterationsPrev.size - 1; i++) {
                computeIterationsPrev.set(i, computeIterationsPrev.get(i + 1));
                deltaPrev.set(i, deltaPrev.get(i + 1));
            }
            computeIterationsPrev.set(computeIterationsPrev.size - 1, iterationsPerTick);
            deltaPrev.set(deltaPrev.size - 1, delta);
        }
        
        float iterationsSmoothed = 0;
        float deltaSmoothed = 0;
        for (int i = 0; i < computeIterationsPrev.size; i++) {
            iterationsSmoothed += computeIterationsPrev.get(i);
            deltaSmoothed += deltaPrev.get(i);
        }
        iterationsSmoothed /= (float) computeIterationsPrev.size;
        deltaSmoothed /= (float) deltaPrev.size;
        
        uiUtils.font.setColor(Color.WHITE);
        uiUtils.font.draw(batch,
                "KIpS:" + (int) ((iterationsSmoothed / deltaSmoothed) / 1000)
                        + " Delta:" + (int) (deltaSmoothed * 1000) + "ms "
                        + (int) (computedIterations / (float) allIterations * 100) + "%",
                -WIDTH / 2f + 25, HEIGHT / 2f - 25);
        batch.end();
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
    
    private int capTo255(int a) {
        return Math.min(a, 255);
    }
    
    private void loadColorPalette(Slider redSlider, Slider greenSlider, Slider blueSlider) {
        rgbMultipliers = palettes.get(currentPalette).get("rgbMultipliers").asFloatArray();
        redSlider.setValue(rgbMultipliers[0]);
        greenSlider.setValue(rgbMultipliers[1]);
        blueSlider.setValue(rgbMultipliers[2]);
    }
    
    private int colorFunc(int value) {
        return rgbToRGBA8888(
                capTo255((int) (value * rgbMultipliers[0])),
                capTo255((int) (value * rgbMultipliers[1])),
                capTo255((int) (value * rgbMultipliers[2])));
    }
    
}

class AlphabetFilter implements TextField.TextFieldFilter {
    @Override
    public boolean acceptChar(TextField textField, char c) {
        if (c == ' ') {
            return true;
        }
        for (int i = 0; i < alphabet.length(); i++) {
            if (alphabet.charAt(i) == Character.toUpperCase(c)) return true;
        }
        return false;
    }
}

class DigitFilter implements TextField.TextFieldFilter {
    
    private final char[] accepted;
    
    public DigitFilter(boolean acceptNegative, boolean acceptFloats) {
        accepted = new char[]{acceptNegative ? '-' : '0', acceptFloats ? '.' : '0', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'};
    }
    
    @Override
    public boolean acceptChar(TextField textField, char c) {
        for (char a : accepted)
            if (a == c) return true;
        return false;
    }
}