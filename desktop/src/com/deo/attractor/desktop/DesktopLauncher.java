package com.deo.attractor.desktop;

import com.badlogic.gdx.backends.lwjgl.LwjglApplication;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.deo.attractor.Launcher;

import static com.deo.attractor.Launcher.HEIGHT;
import static com.deo.attractor.Launcher.WIDTH;

public class DesktopLauncher {
	public static void main (String[] arg) {
		LwjglApplicationConfiguration config = new LwjglApplicationConfiguration();
		
		config.foregroundFPS = 60;
		config.width = WIDTH;
		config.height = HEIGHT;
		config.fullscreen = true;
		config.resizable = true;
		
		new LwjglApplication(new Launcher(), config);
	}
}
