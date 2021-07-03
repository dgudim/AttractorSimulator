package com.deo.attractor.Attractors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import static com.deo.attractor.Launcher.HEIGHT;
import static com.deo.attractor.Launcher.WIDTH;

public class SimulationScreen2D implements Screen {
    
    Attractor2D attractor2D;
    
    SpriteBatch batch;
    SpriteBatch stageBatch;
    OrthographicCamera camera;
    ScreenViewport viewport;
    ShapeRenderer renderer;
    
    Stage stage;
    
    public SimulationScreen2D(int renderResolutionMultiplier) {
        
        WIDTH *= renderResolutionMultiplier;
        HEIGHT *= renderResolutionMultiplier;
        
        batch = new SpriteBatch();
        stageBatch = new SpriteBatch();
        camera = new OrthographicCamera(WIDTH, HEIGHT);
        viewport = new ScreenViewport(camera);
        renderer = new ShapeRenderer();
        renderer.setAutoShapeType(true);
        
        stage = new Stage(viewport, stageBatch);
        attractor2D = new Attractor2D(stage, 50, 2);
        InputMultiplexer multiplexer = new InputMultiplexer();
        AttractorController attractorController = new AttractorController(attractor2D);
        multiplexer.addProcessor(stage);
        multiplexer.addProcessor(attractorController);
        Gdx.input.setInputProcessor(multiplexer);
    }
    
    @Override
    public void show() {
    
    }
    
    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        attractor2D.render(batch, camera, renderer, delta);
        if(attractor2D.showUI){
            stage.draw();
            stage.act(delta);
        }
    }
    
    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
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

class AttractorController implements InputProcessor {
    
    Attractor2D attractor2D;
    int lastScreenX = 0, lastScreenY = 0;
    
    public AttractorController(Attractor2D attractor2D) {
        this.attractor2D = attractor2D;
    }
    
    @Override
    public boolean keyDown(int keycode) {
        return false;
    }
    
    @Override
    public boolean keyUp(int keycode) {
        return false;
    }
    
    @Override
    public boolean keyTyped(char character) {
        return false;
    }
    
    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        lastScreenX = screenX;
        lastScreenY = screenY;
        return false;
    }
    
    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }
    
    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        attractor2D.translateAndZoom((screenX - lastScreenX), (screenY - lastScreenY), 0);
        lastScreenX = screenX;
        lastScreenY = screenY;
        return false;
    }
    
    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }
    
    @Override
    public boolean scrolled(float amountX, float amountY) {
        attractor2D.translateAndZoom(0, 0, amountY / 25f);
        return false;
    }
}
