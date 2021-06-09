package com.deo.attractor;

import com.badlogic.gdx.Game;

public final class Launcher extends Game {

    public static final int WIDTH = 1280;
    public static final int HEIGHT = 720;

    @Override
    public void create() {
        this.setScreen(new SimulationScreen(this));
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}