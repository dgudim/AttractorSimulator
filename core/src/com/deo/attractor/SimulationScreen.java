package com.deo.attractor;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.audio.AudioDevice;
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
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.deo.attractor.Utils.MathExpression;

import java.util.ArrayList;

import static com.deo.attractor.Launcher.HEIGHT;
import static com.deo.attractor.Launcher.WIDTH;
import static com.deo.attractor.Utils.Utils.fontChars;
import static com.deo.attractor.Utils.Utils.interpolate;
import static com.deo.attractor.Utils.Utils.makeAScreenShot;
import static java.lang.StrictMath.PI;
import static java.lang.StrictMath.abs;
import static java.lang.StrictMath.max;
import static java.lang.StrictMath.sin;

public class SimulationScreen implements Screen {
    
    private final PerspectiveCamera cam;
    private final OrthographicCamera camera;
    
    private final ScreenViewport viewport_2d;
    private final ScreenViewport viewport_3d;
    
    Stage stage;
    SpriteBatch batch;
    BitmapFont font;
    ShapeRenderer renderer;
    
    boolean recording = false;
    int frame = 0;
    
    Attractor attractor;
    
    int palette = 1;
    int attractorType = 0;
    int numberOfCurves = 70;
    int pointsPerCurve = 1000;
    boolean settingsMode = false;
    
    private Game game;
    
    private final CameraInputController cameraInputController;
    
    SimulationScreen(Game game) {
        
        this.game = game;
        
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
        
        attractor = new Attractor(attractorType, numberOfCurves, pointsPerCurve, palette);
        String[] constants = attractor.constants;
        final MathExpression[] simRules = attractor.simRules;
        
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
                    for (MathExpression expression : simRules) {
                        expression.changeConstant(finalI, parameterSlider.getValue());
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
        
        cameraInputController.update();
        
        attractor.render(renderer, cam, false, 0);
        
        if (recording) {
            makeAScreenShot(frame);
            frame++;
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
        
        if (settingsMode) {
            batch.setProjectionMatrix(camera.combined);
            batch.begin();
            stage.draw();
            stage.act(delta);
            batch.end();
        }
    
        if (Gdx.input.isKeyJustPressed(Input.Keys.L)) {
            game.setScreen(new RenderScreen(attractor, game, this));
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
