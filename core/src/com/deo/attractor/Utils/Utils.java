package com.deo.attractor.Utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.PixmapIO;
import com.badlogic.gdx.utils.BufferUtils;
import com.badlogic.gdx.utils.ScreenUtils;

import java.util.ArrayList;

import static java.lang.StrictMath.PI;
import static java.lang.StrictMath.sin;

public class Utils {
    
    public final static String fontChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890\"!`?'.,;:()[]{}<>|/@\\^$€-%+=#_&~*ёйцукенгшщзхъэждлорпавыфячсмитьбюЁЙЦУКЕНГШЩЗХЪЭЖДЛОРПАВЫФЯЧСМИТЬБЮ";
    
    public static Color[][] availablePalettes = new Color[][]{
            {Color.CLEAR, Color.ORANGE, Color.CYAN, Color.CORAL},
            {Color.CLEAR, Color.valueOf("#662341"), Color.valueOf("#ffe240"), Color.FIREBRICK},
            {Color.CLEAR, Color.ORANGE, Color.RED, Color.GRAY},
            {Color.CLEAR, Color.LIME, Color.TEAL, Color.CLEAR},
            {Color.CLEAR, Color.SKY, Color.SKY, Color.CLEAR, Color.CLEAR, Color.TEAL, Color.TEAL, Color.CLEAR}};
    
    public static int interpolate(double step, double maxValue, Color... colors) {
        step = Math.max(Math.min(step / maxValue, 1.0f), 0.0f);
        
        switch (colors.length) {
            case 0:
                throw new IllegalArgumentException("At least one color required.");
            
            case 1:
                return Color.argb8888(colors[0]);
            
            case 2:
                return mixTwoColors(colors[0], colors[1], (float) step);
            
            default:
                
                int firstColorIndex = (int) (step * (colors.length - 1));
                
                if (firstColorIndex == colors.length - 1) {
                    return Color.argb8888(colors[colors.length - 1]);
                }
                
                // stepAtFirstColorIndex will be a bit smaller than step
                float stepAtFirstColorIndex = (float) firstColorIndex
                        / (colors.length - 1);
                
                // multiply to increase values to range between 0.0f and 1.0f
                double localStep = (step - stepAtFirstColorIndex)
                        * (colors.length - 1);
                
                return mixTwoColors(colors[firstColorIndex],
                        colors[firstColorIndex + 1], (float) localStep);
        }
        
    }
    
    public static int mixTwoColors(Color color1, Color color2, float ratio) {
        return Color.rgba8888(color1.r * (1f - ratio) + color2.r * ratio, color1.g * (1f - ratio) + color2.g * ratio, color1.b * (1f - ratio) + color2.b * ratio, color1.a * (1f - ratio) + color2.a * ratio);
    }
    
    public static double curveFunc(int type, double value1, double value2) {
        switch (type) {
            case (0):
            default:
                return Math.pow(value1, value2);
            case (1):
                return Math.sqrt(value1);
            case (2):
                return value1;
        }
    }
    
    public static int capTo255(int a) {
        return Math.min(a, 255);
    }
    
    public static int colorFunc(int value, int type) {
        switch (type) {
            case (0):
            default:
                return rgbToRGBA8888(capTo255(value), capTo255(value << 1), capTo255(value << 2));
            case (1):
                return rgbToRGBA8888(capTo255(value), capTo255(value << 2), capTo255(value << 1));
            case (2):
                return rgbToRGBA8888(capTo255(value << 1), capTo255(value), capTo255(value << 2));
            case (3):
                return rgbToRGBA8888(capTo255(value << 1), capTo255(value << 2), capTo255(value));
            case (4):
                return rgbToRGBA8888(capTo255(value << 2), capTo255(value << 1), capTo255(value));
            case (5):
                return rgbToRGBA8888(capTo255(value << 2), capTo255(value), capTo255(value << 1));
            case (6):
                return rgbToRGBA8888(capTo255(value << 1), capTo255(value << 1), capTo255(value));
            case (7):
                return rgbToRGBA8888(capTo255(value << 1), capTo255(value), capTo255(value << 1));
        }
    }
    
    public static int rgbToRGBA8888(int r, int g, int b) {
        return r << 24 | g << 16 | b << 8 | 255;
    }
    
    public static void makeAScreenShot(String name) {
        byte[] pixels = ScreenUtils.getFrameBufferPixels(0, 0, Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), true);
        
        for (int i4 = 4; i4 < pixels.length; i4 += 4) {
            pixels[i4 - 1] = (byte) 255;
        }
        
        FileHandle file = Gdx.files.external("GollyRender/" + name + ".png");
        Pixmap pixmap = new Pixmap(Gdx.graphics.getBackBufferWidth(), Gdx.graphics.getBackBufferHeight(), Pixmap.Format.RGBA8888);
        BufferUtils.copy(pixels, 0, pixmap.getPixels(), pixels.length);
        PixmapIO.writePNG(file, pixmap);
        pixmap.dispose();
    }
    
    public static void makeAScreenShot(int recorderFrame) {
        makeAScreenShot("pict" + recorderFrame);
    }
    
    public static ArrayList<Float> generateSine(int sampleRate, float frequency, float duration) {
        ArrayList<Float> samples = new ArrayList<>();
        double period = PI * 2;
        for (double i = 0; i < period * duration; i += period / (double) sampleRate) {
            samples.add((float) sin(i * frequency));
        }
        return samples;
    }
    
}
