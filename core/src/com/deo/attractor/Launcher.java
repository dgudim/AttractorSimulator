package com.deo.attractor;

import com.badlogic.gdx.Game;

public final class Launcher extends Game {
    
    public static final int WIDTH = 1920;
    public static final int HEIGHT = 1080;

    @Override
    public void create() {
        this.setScreen(new SimulationScreen());
    }

    @Override
    public void dispose() {
        super.dispose();
    }
}