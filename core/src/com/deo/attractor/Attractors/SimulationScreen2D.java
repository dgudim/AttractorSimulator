package com.deo.attractor.Attractors;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.viewport.ScreenViewport;

import static com.deo.attractor.Launcher.HEIGHT;
import static com.deo.attractor.Launcher.WIDTH;

public class SimulationScreen2D implements Screen {
    
    Attractor2D attractor2D;
    
    SpriteBatch batch;
    OrthographicCamera camera;
    ScreenViewport viewport;
    ShapeRenderer renderer;
    
    public SimulationScreen2D() {
        
        WIDTH *= 4;
        HEIGHT *= 4;
        
        batch = new SpriteBatch();
        camera = new OrthographicCamera(WIDTH, HEIGHT);
        viewport = new ScreenViewport(camera);
        renderer = new ShapeRenderer();
        renderer.setAutoShapeType(true);
        
        attractor2D = new Attractor2D(8, 20, 5000, 4);
        attractor2D.startThreads();
    }
    
    @Override
    public void show() {
    
    }
    
    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
        attractor2D.render(batch, camera, renderer, delta);
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
