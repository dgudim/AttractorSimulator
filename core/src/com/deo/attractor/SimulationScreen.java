package com.deo.attractor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.deo.attractor.Utils.MathExpression;

import java.util.ArrayList;
import java.util.Arrays;

import static com.badlogic.gdx.math.MathUtils.clamp;
import static com.deo.attractor.Launcher.HEIGHT;
import static com.deo.attractor.Launcher.WIDTH;
import static com.deo.attractor.Utils.Utils.fontChars;
import static com.deo.attractor.Utils.Utils.makeAScreenShot;
import static java.lang.StrictMath.abs;

public class SimulationScreen implements Screen {
    
    private final PerspectiveCamera cam;
    private final OrthographicCamera camera;
    
    private final ScreenViewport viewport_2d;
    private final ScreenViewport viewport_3d;
    
    Stage stage;
    SpriteBatch batch;
    BitmapFont font;
    ShapeRenderer renderer;
    
    boolean rendering = false;
    volatile boolean[] renderThreadsFinished;
    volatile float[] renderThreadsProgress;
    boolean recording = false;
    int frame = 0;
    
    final int renderPointsPerCurve = 100000;
    
    Attractor attractor;
    
    int palette = 1;
    int attractorType = 0;
    int numberOfCurves = 15;
    int pointsPerCurve = 1000;
    float spread = 1;
    boolean settingsMode = false;
    boolean pointRender = false;
    boolean hsvColoring = false;
    boolean speedColoring = false;
    float coloringScale = 1;
    
    private final CameraInputController cameraInputController;
    
    SimulationScreen() {
        
        renderThreadsFinished = new boolean[numberOfCurves];
        renderThreadsProgress = new float[numberOfCurves];
        
        camera = new OrthographicCamera(WIDTH, HEIGHT);
        viewport_2d = new ScreenViewport(camera);
        stage = new Stage(viewport_2d);
        batch = new SpriteBatch();
        
        TextureAtlas uiAtlas = new TextureAtlas(Gdx.files.internal("ui.atlas"));
        Skin uiTextures = new Skin();
        uiTextures.addRegions(uiAtlas);
        
        Slider.SliderStyle sliderStyle = new Slider.SliderStyle();
        sliderStyle.background = uiTextures.getDrawable("progressBarBg");
        sliderStyle.knob = uiTextures.getDrawable("progressBarKnob");
        sliderStyle.knobDown = uiTextures.getDrawable("progressBarKnob_enabled");
        sliderStyle.knobOver = uiTextures.getDrawable("progressBarKnob_over");
        sliderStyle.background.setMinHeight(63);
        sliderStyle.knob.setMinHeight(30);
        sliderStyle.knobDown.setMinHeight(30);
        sliderStyle.knobOver.setMinHeight(30);
        
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("font.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 38;
        parameter.characters = fontChars;
        font = generator.generateFont(parameter);
        generator.dispose();
        font.getData().markupEnabled = true;
        
        attractor = new Attractor(attractorType, numberOfCurves, pointsPerCurve, palette, spread);
        String[] constants = attractor.constants;
        final ArrayList<MathExpression[]> simRules = attractor.simRules;
        
        for (int i = 0; i < constants.length; i++) {
            final String[] vals = constants[i].replace(" ", "").split("=");
            float constVal = Float.parseFloat(vals[vals.length - 1]);
            float absConstVal = abs(constVal);
            float step = absConstVal / 500f;
            float max = constVal + absConstVal;
            if (vals[0].equals("t")) {
                max = constVal;
            }
            final Slider parameterSlider = new Slider(constVal - absConstVal, max, step, false, sliderStyle);
            parameterSlider.setValue(constVal);
            final Label valueText = new Label(vals[0] + ":" + constVal, new LabelStyle(font, Color.WHITE));
            
            final int finalI = i;
            parameterSlider.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    valueText.setText(vals[0] + ":" + parameterSlider.getValue());
                    for (MathExpression[] expressions : simRules) {
                        for (MathExpression expression : expressions) {
                            expression.changeConstant(finalI, parameterSlider.getValue());
                        }
                    }
                }
            });
            
            Table holder = new Table();
            holder.add(valueText);
            holder.add(parameterSlider).size(500, 25);
            holder.align(Align.right);
            holder.setBounds(0, i * 60 - HEIGHT / 2f + 25, 550, 25);
            stage.addActor(holder);
        }
        
        final Slider pointsPerCurveSlider = new Slider(1, pointsPerCurve * 10, 1, false, sliderStyle);
        pointsPerCurveSlider.setValue(pointsPerCurve);
        final Label pointPerCurveText = new Label("points per curve:" + pointsPerCurve, new LabelStyle(font, Color.WHITE));
        pointsPerCurveSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                pointsPerCurve = (int) pointsPerCurveSlider.getValue();
                pointPerCurveText.setText("points per curve:" + pointsPerCurve);
                attractor.setPointsPerCurve(pointsPerCurve);
            }
        });
        Table holder = new Table();
        holder.add(pointPerCurveText);
        holder.add(pointsPerCurveSlider).size(WIDTH - pointPerCurveText.getPrefWidth(), 25);
        holder.align(Align.right);
        holder.setBounds(-WIDTH / 2f + 15, HEIGHT / 2f - 35, WIDTH, 25);
        stage.addActor(holder);
        
        cam = new PerspectiveCamera(67, WIDTH, HEIGHT);
        cam.position.set(10f, 10f, 10f);
        cam.lookAt(0, 0, 0);
        cam.near = 0f;
        cam.far = 500f;
        cam.update();
        viewport_3d = new ScreenViewport(cam);
        
        renderer = new ShapeRenderer();
        renderer.setAutoShapeType(true);
        
        cameraInputController = new CameraInputController(cam);
    }
    
    @Override
    public void show() {
        Gdx.gl.glLineWidth(2.1f);
        Gdx.input.setInputProcessor(cameraInputController);
    }
    
    @Override
    public void render(float delta) {
        
        if (!Gdx.input.isKeyPressed(Input.Keys.H)) {
            Gdx.gl.glClearColor(0, 0, 0, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        }
        
        processButtonPresses(delta);
        
        cameraInputController.update();
        attractor.render(renderer, cam, speedColoring, hsvColoring, coloringScale, pointRender);
        
        if (rendering) {
            renderer.setProjectionMatrix(camera.combined);
            renderer.begin(ShapeRenderer.ShapeType.Filled);
            float progressBarHeight = clamp(HEIGHT / (float) numberOfCurves, 1, 35);
            for (int i = 0; i < numberOfCurves; i++) {
                renderer.setColor(Color.WHITE);
                renderer.rect(-WIDTH / 2f, -HEIGHT / 2f + (progressBarHeight + 5) * i, 340, progressBarHeight);
                renderer.setColor(Color.FOREST);
                renderer.rect(-WIDTH / 2f + 1, -HEIGHT / 2f + (progressBarHeight + 5) * i + 1, 338 * renderThreadsProgress[i], progressBarHeight - 2);
            }
            renderer.end();
            
            boolean finished = true;
            for (int i = 0; i < numberOfCurves; i++) {
                if (!renderThreadsFinished[i]) {
                    finished = false;
                    break;
                }
            }
            if (finished) {
                rendering = false;
                Arrays.fill(renderThreadsFinished, false);
                Arrays.fill(renderThreadsProgress, 0);
            }
        }
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.ALT_RIGHT)) {
            makeAScreenShot(frame);
            frame++;
        }
        
        if (recording) {
            makeAScreenShot(frame);
            frame++;
        }
        
        if (settingsMode) {
            batch.setProjectionMatrix(camera.combined);
            batch.begin();
            stage.draw();
            stage.act(delta);
            batch.end();
        }
    }
    
    void processButtonPresses(float delta) {
        if (Gdx.input.isKeyJustPressed(Input.Keys.L)) {
            if (!rendering) {
                rendering = true;
                while (!attractor.threadComputeCycleFinished) {
                    System.out.println("Thread cycle not finished, waiting");
                }
                attractor.setPointsPerCurve(renderPointsPerCurve);
                for (int i = 0; i < attractor.curves.size; i++) {
                    attractor.curves.get(i).reset();
                }
                for (int i = 0; i < attractor.curves.size; i++) {
                    final int finalI = i;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            for (int i = 0; i < renderPointsPerCurve; i++) {
                                attractor.curves.get(finalI).advance();
                                renderThreadsProgress[finalI] = i/(float)renderPointsPerCurve;
                            }
                            renderThreadsFinished[finalI] = true;
                        }
                    }).start();
                }
            }
        }
        if (Gdx.input.isKeyPressed(Input.Keys.F2)) {
            coloringScale += delta / 10f;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.F1)) {
            coloringScale -= delta / 10f;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) {
            hsvColoring = !hsvColoring;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F4)) {
            pointRender = !pointRender;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F5)) {
            speedColoring = !speedColoring;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            attractor.reset();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
            recording = !recording;
            frame = 0;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            settingsMode = !settingsMode;
            if (settingsMode) {
                Gdx.input.setInputProcessor(stage);
            } else {
                Gdx.input.setInputProcessor(cameraInputController);
            }
        }
    }
    
    @Override
    public void resize(int width, int height) {
        viewport_2d.update(width, height);
        viewport_3d.update(width, height);
        camera.position.set(0, 0, 0);
        float tempScaleH = height / (float) HEIGHT;
        float tempScaleW = width / (float) WIDTH;
        float zoom = Math.min(tempScaleH, tempScaleW);
        camera.zoom = 1 / zoom;
        camera.update();
    }
    
    @Override
    public void pause() {
    
    }
    
    @Override
    public void resume() {
    
    }
    
    @Override
    public void hide() {
    
    }
    
    @Override
    public void dispose() {
    }
}
