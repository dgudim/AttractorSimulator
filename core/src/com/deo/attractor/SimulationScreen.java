package com.deo.attractor;

import com.badlogic.gdx.Game;
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
import static com.deo.attractor.Utils.Utils.interpolate;
import static com.deo.attractor.Utils.Utils.makeAScreenShot;
import static java.lang.StrictMath.abs;
import static java.lang.StrictMath.max;
import static java.lang.StrictMath.min;
import static java.lang.StrictMath.random;

public class SimulationScreen implements Screen {
    
    private final PerspectiveCamera cam;
    private final OrthographicCamera camera;
    
    private final ScreenViewport viewport_2d;
    private final ScreenViewport viewport_3d;
    
    Stage stage;
    SpriteBatch batch;
    BitmapFont font;
    ShapeRenderer renderer;
    
    boolean recording = true;
    int frame = 0;
    
    float spread = 1;
    int palette = 1;
    int attractorType = 4;
    int numberOfPoints = 10;
    int pointsPerCurve = 1000;
    boolean settingsMode = false;
    float currentTimeStep;
    
    private final MathExpression[] simRules;
    
    private final Array<Point> curves;
    
    private final CameraInputController cameraInputController;
    
    private final ArrayList<Vector3> pointGraph;
    private final float graphScale = 1;
    private float maxUpperGraphAmplitude = 1;
    private float maxBottomGraphAmplitude = 1;
    private float max2DGraphAmplitude = 1;
    
    final String fontChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890\"!`?'.,;:()[]{}<>|/@\\^$€-%+=#_&~*ёйцукенгшщзхъэждлорпавыфячсмитьбюЁЙЦУКЕНГШЩЗХЪЭЖДЛОРПАВЫФЯЧСМИТЬБЮ";
    
    Color[][] availablePalettes = new Color[][]{
            {Color.CLEAR, Color.ORANGE, Color.CYAN, Color.CORAL},
            {Color.CLEAR, Color.valueOf("#662341"), Color.valueOf("#ffe240"), Color.FIREBRICK},
            {Color.CLEAR, Color.ORANGE, Color.RED, Color.GRAY},
            {Color.CLEAR, Color.LIME, Color.TEAL, Color.CLEAR},
            {Color.CLEAR, Color.SKY, Color.SKY, Color.CLEAR, Color.CLEAR, Color.TEAL, Color.TEAL, Color.CLEAR}};
    
    Array<Color> colors;
    
    SimulationScreen(Game game) {
        
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
        
        simRules = new MathExpression[3];
        String[] bulkArgs = new String[]{"x = 0", "y = 0", "z = 0"};
        String[] constants = new String[]{};
        float maxTimestep = 1;
        float scale = 1;
        switch (attractorType) {
            case (0):
                constants = new String[]{"a = 20", "b = 40", "c = 0.8333", "d = 0.65", "f = 0.5"};
                simRules[0] = new MathExpression("b * (y - x) + f * x * z", bulkArgs, constants);
                simRules[1] = new MathExpression("a * y - x * z", bulkArgs, constants);
                simRules[2] = new MathExpression("c * z + x * y - d * x * x", bulkArgs, constants);
                maxTimestep = 0.001f;
                scale = 0.2f;
                break;
            case (1):
                constants = new String[]{};
                simRules[0] = new MathExpression("y", bulkArgs, constants);
                simRules[1] = new MathExpression("-x + y * z", bulkArgs, constants);
                simRules[2] = new MathExpression("1 - y * y", bulkArgs, constants);
                maxTimestep = 0.05f;
                break;
            case (2):
                constants = new String[]{"a = -1.4", "b = 4"};
                simRules[0] = new MathExpression("a * x - b * y - b * z - y*y", bulkArgs, constants);
                simRules[1] = new MathExpression("a * y - b * z - b * x - z*z", bulkArgs, constants);
                simRules[2] = new MathExpression("a * z - b * x - b * y - x*x", bulkArgs, constants);
                maxTimestep = 0.005f;
                spread = 1;
                break;
            case (3):
                constants = new String[]{"a = 0.45", "b = 0.75"};
                simRules[0] = new MathExpression("y", bulkArgs, constants);
                simRules[1] = new MathExpression("(1 - z) * x - b * y", bulkArgs, constants);
                simRules[2] = new MathExpression("x * x - a * z", bulkArgs, constants);
                maxTimestep = 0.07f;
                break;
            case (4):
                constants = new String[]{"a = 0.7", "b = 3.5", "c = 0.95", "d = 0.25", "f = 0.1", "k = 0.6"};
                
                simRules[0] = new MathExpression("(z - a) * x - b * y", bulkArgs, constants);
                simRules[1] = new MathExpression("b * x + (z - a) * y", bulkArgs, constants);
                simRules[2] = new MathExpression("k + c * z - (z * z * z) / 3 - (x * x + y * y) * (1 + d * z) + f * z * x * x * x", bulkArgs, constants);
                
                maxTimestep = 0.002f;
                spread = 1;
                break;
        }
        for (int i = 0; i < constants.length; i++) {
            final String[] vals = constants[i].replace(" ", "").split("=");
            float constVal = Float.parseFloat(vals[vals.length - 1]);
            float absConstVal = abs(constVal);
            float step = absConstVal / 500f;
            final Slider parameterSlider = new Slider(constVal - absConstVal, constVal + absConstVal, step, false, sliderStyle);
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
        currentTimeStep = maxTimestep;
        
        colors = new Array<>();
        for (int i = 0; i < pointsPerCurve; i++) {
            colors.add(new Color(interpolate(i, pointsPerCurve, availablePalettes[palette])));
        }
        curves = new Array<>();
        for (int i = 0; i < numberOfPoints; i++) {
            curves.add(new Point(new Vector3(
                    (float) (random() * spread),
                    (float) (random() * spread),
                    (float) (random() * spread)),
                    simRules, maxTimestep, scale, pointsPerCurve));
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
        
        pointGraph = new ArrayList<>();
        
        cameraInputController = new CameraInputController(cam);
        Gdx.input.setInputProcessor(cameraInputController);
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    for (int i = 0; i < curves.size; i++) {
                        curves.get(i).advance();
                    }
                }
            }
        }).start();
        
    }
    
    @Override
    public void show() {
        Gdx.gl.glLineWidth(2.1f);
    }
    
    @Override
    public void render(float delta) {
        
        if (!Gdx.input.isKeyPressed(Input.Keys.H)) {
            Gdx.gl.glClearColor(0, 0, 0, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        }
        
        cameraInputController.update();
        
        renderer.setProjectionMatrix(cam.combined);
        renderer.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        for (int p = 0; p < pointsPerCurve - 1; p++) {
            renderer.setColor(colors.get(p));
            for (int i = 0; i < curves.size; i++) {
                renderer.line(curves.get(i).points.get(p), curves.get(i).points.get(p + 1));
            }
        }
        renderer.end();
        
        if (recording) {
            makeAScreenShot(frame);
            frame++;
        }
        
        if (pointGraph.size() > WIDTH * graphScale) {
            for (int i = 0; i < pointGraph.size() - 1; i++) {
                pointGraph.set(i, pointGraph.get(i + 1));
            }
            pointGraph.set(pointGraph.size() - 1, curves.get(0).points.get(curves.get(0).points.size - 1));
        } else {
            pointGraph.add(curves.get(0).points.get(curves.get(0).points.size - 1));
        }
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            for (int i = 0; i < curves.size; i++) {
                curves.get(i).resetTrail();
            }
            maxUpperGraphAmplitude = 1;
            maxBottomGraphAmplitude = 1;
            max2DGraphAmplitude = 1;
        }
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.T)) {
            for (int i = 0; i < curves.size; i++) {
                curves.get(i).reset();
            }
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
            renderer.setProjectionMatrix(camera.combined);
            renderer.begin();
            float multiplier = WIDTH / (float) numberOfPoints;
            for (int n = 0; n < curves.size - 1; n++) {
                int i = n * (int) multiplier;
                Vector3 point = curves.get(n).points.get(curves.get(n).points.size - 1);
                Vector3 nextPoint = curves.get(n + 1).points.get(curves.get(n + 1).points.size - 1);
                renderer.setColor(Color.RED);
                renderer.line(i - WIDTH / 2f, point.x / maxBottomGraphAmplitude * 15 + 30 - HEIGHT / 2f, i + multiplier - WIDTH / 2f, nextPoint.x / maxBottomGraphAmplitude * 15 + 30 - HEIGHT / 2f);
                renderer.setColor(Color.GREEN);
                renderer.line(i - WIDTH / 2f, point.y / maxBottomGraphAmplitude * 15 + 30 - HEIGHT / 2f, i + multiplier - WIDTH / 2f, nextPoint.y / maxBottomGraphAmplitude * 15 + 30 - HEIGHT / 2f);
                renderer.setColor(Color.BLUE);
                renderer.line(i - WIDTH / 2f, point.z / maxBottomGraphAmplitude * 15 + 30 - HEIGHT / 2f, i + multiplier - WIDTH / 2f, nextPoint.z / maxBottomGraphAmplitude * 15 + 30 - HEIGHT / 2f);
                maxBottomGraphAmplitude = max(maxBottomGraphAmplitude, abs(point.x));
                maxBottomGraphAmplitude = max(maxBottomGraphAmplitude, abs(point.y));
                maxBottomGraphAmplitude = max(maxBottomGraphAmplitude, abs(point.z));
            }
            float multiplier2 = 1 / graphScale;
            for (int n = 0; n < pointGraph.size() - 1; n++) {
                int i = (int) (n * multiplier2);
                renderer.setColor(Color.YELLOW);
                renderer.line(i - WIDTH / 2f, pointGraph.get(n).x / maxUpperGraphAmplitude * 15 - 30 + HEIGHT / 2f, i + multiplier2 - WIDTH / 2f, pointGraph.get(n + 1).x / maxUpperGraphAmplitude * 15 - 30 + HEIGHT / 2f);
                renderer.setColor(Color.CYAN);
                renderer.line(i - WIDTH / 2f, pointGraph.get(n).y / maxUpperGraphAmplitude * 15 - 30 + HEIGHT / 2f, i + multiplier2 - WIDTH / 2f, pointGraph.get(n + 1).y / maxUpperGraphAmplitude * 15 - 30 + HEIGHT / 2f);
                renderer.setColor(Color.MAGENTA);
                renderer.line(i - WIDTH / 2f, pointGraph.get(n).z / maxUpperGraphAmplitude * 15 - 30 + HEIGHT / 2f, i + multiplier2 - WIDTH / 2f, pointGraph.get(n + 1).z / maxUpperGraphAmplitude * 15 - 30 + HEIGHT / 2f);
                maxUpperGraphAmplitude = max(maxUpperGraphAmplitude, abs(pointGraph.get(n).x));
                maxUpperGraphAmplitude = max(maxUpperGraphAmplitude, abs(pointGraph.get(n).y));
                maxUpperGraphAmplitude = max(maxUpperGraphAmplitude, abs(pointGraph.get(n).z));
            }
            renderer.setColor(Color.YELLOW);
            render2DGraph(0, 0, WIDTH / 2f, HEIGHT / 2f, curves.get(0).points, "x");
            renderer.setColor(Color.CYAN);
            render2DGraph(0, 0, WIDTH / 2f, HEIGHT / 2f, curves.get(0).points, "y");
            renderer.setColor(Color.MAGENTA);
            render2DGraph(0, 0, WIDTH / 2f, HEIGHT / 2f, curves.get(0).points, "z");
            renderer.end();
            
            batch.begin();
            stage.draw();
            stage.act(delta);
            batch.end();
        }
    }
    
    void render2DGraph(float x, float y, float width, float height, Array<Vector3> points_orig, String ignore) {
        Array<Vector3> points = new Array<>();
        for (int i = 0; i < points_orig.size; i++) {
            points.add(new Vector3(points_orig.get(i)));
        }
        float minTargetSize = min(width, height);
        for (int i = 0; i < points.size; i++) {
            Vector3 point = points.get(i);
            max2DGraphAmplitude = max(max2DGraphAmplitude, abs(point.x));
            max2DGraphAmplitude = max(max2DGraphAmplitude, abs(point.y));
            max2DGraphAmplitude = max(max2DGraphAmplitude, abs(point.z));
        }
        for (int i = 0; i < points.size; i++) {
            points.get(i).x /= max2DGraphAmplitude;
            points.get(i).y /= max2DGraphAmplitude;
            points.get(i).z /= max2DGraphAmplitude;
        }
        for (int i = 0; i < points.size; i++) {
            Vector3 point = points.get(i);
            switch (ignore) {
                case ("x"):
                    point.y *= minTargetSize;
                    point.z *= minTargetSize;
                    break;
                case ("y"):
                    point.x *= minTargetSize;
                    point.z *= minTargetSize;
                    break;
                case ("z"):
                    point.x *= minTargetSize;
                    point.y *= minTargetSize;
                    break;
            }
        }
        for (int i = 0; i < points.size - 1; i++) {
            switch (ignore) {
                case ("x"):
                    renderer.line(x + points.get(i).y, y + points.get(i).z, x + points.get(i + 1).y, y + points.get(i + 1).z);
                    break;
                case ("y"):
                    renderer.line(x + points.get(i).x, y + points.get(i).z, x + points.get(i + 1).x, y + points.get(i + 1).z);
                    break;
                case ("z"):
                    renderer.line(x + points.get(i).x, y + points.get(i).y, x + points.get(i + 1).x, y + points.get(i + 1).y);
                    break;
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
