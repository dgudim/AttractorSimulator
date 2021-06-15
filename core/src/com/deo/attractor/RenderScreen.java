package com.deo.attractor;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import static com.badlogic.gdx.math.MathUtils.clamp;
import static com.deo.attractor.Launcher.HEIGHT;
import static com.deo.attractor.Launcher.WIDTH;
import static com.deo.attractor.Utils.Utils.makeAScreenShot;

public class RenderScreen implements Screen {
    
    private final ScreenViewport viewport;
    private final PerspectiveCamera cam;
    private final CameraInputController cameraInputController;
    private final ShapeRenderer renderer;
    
    final int points = 10000000;
    
    Attractor attractor;
    
    boolean rendering = false;
    float coloringScale = 1;
    
    private final Game game;
    private final SimulationScreen simulationScreen;
    
    RenderScreen(Attractor attractor, Game game, SimulationScreen simulationScreen) {
        
        this.game = game;
        this.simulationScreen = simulationScreen;
        this.attractor = attractor;
        
        cam = new PerspectiveCamera(67, WIDTH, HEIGHT);
        cam.position.set(10f, 10f, 10f);
        cam.lookAt(0, 0, 0);
        cam.near = 0f;
        cam.far = 500f;
        cam.update();
        viewport = new ScreenViewport(cam);
        
        renderer = new ShapeRenderer();
        renderer.setAutoShapeType(true);
        
        cameraInputController = new CameraInputController(cam);
        Gdx.input.setInputProcessor(cameraInputController);
    }
    
    @Override
    public void show() {
    
    }
    
    @Override
    public void render(float delta) {
        
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        cameraInputController.update();
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.L)) {
            rendering = true;
        }
        
        if (Gdx.input.isKeyPressed(Input.Keys.F2)) {
            coloringScale += delta / 10f;
        }
        
        if (Gdx.input.isKeyPressed(Input.Keys.F1)) {
            coloringScale -= delta / 10f;
        }
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.L)) {
            rendering = true;
        }
        
        if (!rendering) {
            attractor.render(renderer, cam, true, coloringScale);
        } else {
            Vector3 pos = attractor.curves.get(0).startingPosition;
            attractor.threadActive = false;
            while (!attractor.threadComputeCycleFinished) {
                System.out.println("Thread cycle not finished, waiting");
            }
            Gdx.gl.glClearColor(0, 0, 0, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
            renderer.setProjectionMatrix(cam.combined);
            renderer.begin(ShapeRenderer.ShapeType.Point);
            Gdx.gl.glEnable(GL20.GL_BLEND);
            Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            float dx, dy, dz;
            for (int i = 0; i < points; i++) {
                Vector3 nextPos = attractor.curves.get(0).calculateNextPosition(pos);
                dx = clamp(Math.abs(nextPos.x - pos.x) * coloringScale, 0, 1);
                dy = clamp(Math.abs(nextPos.y - pos.y) * coloringScale, 0, 1);
                dz = clamp(Math.abs(nextPos.z - pos.z) * coloringScale, 0, 1);
                renderer.setColor(new Color(1 - dx, 1 - dy, 1 - dz, (dx + dy + dz) / 3f));
                pos = nextPos;
                renderer.point(
                        pos.x * attractor.scale,
                        pos.y * attractor.scale,
                        pos.z * attractor.scale);
            }
            renderer.end();
            makeAScreenShot(0);
            rendering = false;
            attractor.threadActive = true;
            game.setScreen(simulationScreen);
            dispose();
        }
    }
    
    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
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
        renderer.dispose();
    }
}
