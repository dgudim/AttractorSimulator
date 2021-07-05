package com.deo.attractor;

import com.badlogic.gdx.Game;
import com.deo.attractor.Attractors.SimulationScreen2D;

public final class Launcher extends Game {
    
    public static int WIDTH = 1920;
    public static int HEIGHT = 1080;
    
    @Override
    public void create() {
        this.setScreen(new SimulationScreen2D());
    }
    
    @Override
    public void dispose() {
        super.dispose();
    }
}