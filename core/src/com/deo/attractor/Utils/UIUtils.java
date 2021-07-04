package com.deo.attractor.Utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Label.LabelStyle;
import com.badlogic.gdx.scenes.scene2d.ui.List;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton.TextButtonStyle;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;

import static com.badlogic.gdx.math.MathUtils.clamp;
import static com.deo.attractor.Utils.Utils.fontChars;
import static com.deo.attractor.Utils.Utils.formatNumber;
import static com.deo.attractor.Utils.Utils.makeFilledRectangle;

public class UIUtils {
    
    public final BitmapFont font;
    public Slider.SliderStyle sliderStyle;
    public TextField.TextFieldStyle textFieldStyle;
    public SelectBox.SelectBoxStyle selectBoxStyle;
    public CheckBox.CheckBoxStyle checkBoxStyle;
    public TextButtonStyle textButtonStyle;
    public LabelStyle labelStyle;
    public Button.ButtonStyle plusButtonStyle;
    
    public boolean forceUseProgrammaticEvents = false;
    
    public UIUtils() {
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("font.ttf"));
        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
        parameter.size = 31;
        parameter.characters = fontChars;
        font = generator.generateFont(parameter);
        generator.dispose();
        font.getData().markupEnabled = true;
        
        Skin uiTextures = new Skin();
        uiTextures.addRegions(new TextureAtlas(Gdx.files.internal("ui.atlas")));
        uiTextures.addRegions(new TextureAtlas(Gdx.files.internal("workshop.atlas")));
        
        sliderStyle = new Slider.SliderStyle();
        sliderStyle.background = uiTextures.getDrawable("progressBarBg");
        sliderStyle.knob = uiTextures.getDrawable("progressBarKnob");
        sliderStyle.knobDown = uiTextures.getDrawable("progressBarKnob_enabled");
        sliderStyle.knobOver = uiTextures.getDrawable("progressBarKnob_over");
        sliderStyle.background.setMinHeight(63);
        sliderStyle.knob.setMinHeight(30);
        sliderStyle.knobDown.setMinHeight(30);
        sliderStyle.knobOver.setMinHeight(30);
        
        textFieldStyle = new TextField.TextFieldStyle();
        textFieldStyle.background = makeFilledRectangle(100, 30, Color.CLEAR);
        textFieldStyle.cursor = makeFilledRectangle(3, 30, Color.WHITE);
        textFieldStyle.font = font;
        textFieldStyle.disabledFontColor = Color.DARK_GRAY;
        textFieldStyle.fontColor = Color.LIGHT_GRAY;
        textFieldStyle.focusedFontColor = Color.WHITE;
        
        checkBoxStyle = new CheckBox.CheckBoxStyle();
        checkBoxStyle.checkboxOff = uiTextures.getDrawable("checkBox_disabled");
        checkBoxStyle.checkboxOn = uiTextures.getDrawable("checkBox_enabled");
        checkBoxStyle.checkboxOver = uiTextures.getDrawable("checkBox_disabled_over");
        checkBoxStyle.checkboxOnOver = uiTextures.getDrawable("checkBox_enabled_over");
        checkBoxStyle.checkboxOff.setMinHeight(50);
        checkBoxStyle.checkboxOn.setMinHeight(50);
        checkBoxStyle.checkboxOver.setMinHeight(50);
        checkBoxStyle.checkboxOnOver.setMinHeight(50);
        checkBoxStyle.checkboxOff.setMinWidth(50);
        checkBoxStyle.checkboxOn.setMinWidth(50);
        checkBoxStyle.checkboxOver.setMinWidth(50);
        checkBoxStyle.checkboxOnOver.setMinWidth(50);
        checkBoxStyle.font = font;
        checkBoxStyle.fontColor = Color.LIGHT_GRAY;
        checkBoxStyle.overFontColor = Color.WHITE;
        checkBoxStyle.checkedFontColor = Color.WHITE;
        
        textButtonStyle = new TextButtonStyle();
        textButtonStyle.up = uiTextures.getDrawable("blank_shopButton_disabled");
        textButtonStyle.down = uiTextures.getDrawable("blank_shopButton_enabled");
        textButtonStyle.over = uiTextures.getDrawable("blank_shopButton_over");
        textButtonStyle.font = font;
        textButtonStyle.disabledFontColor = Color.valueOf("#3D4931");
        textButtonStyle.overFontColor = Color.valueOf("#3D5232");
        textButtonStyle.fontColor = Color.valueOf("#22370E");
        
        plusButtonStyle = new Button.ButtonStyle();
        plusButtonStyle.up = new TextureRegionDrawable(new Texture(Gdx.files.internal("plus_dark_gray.png")));
        plusButtonStyle.over = new TextureRegionDrawable(new Texture(Gdx.files.internal("plus_gray.png")));
        plusButtonStyle.down = new TextureRegionDrawable(new Texture(Gdx.files.internal("plus.png")));
        
        labelStyle = new Label.LabelStyle(font, Color.WHITE);
        
        TextureRegionDrawable BarBackgroundGrey = makeFilledRectangle(100, 30, Color.valueOf("#000000AA"));
        TextureRegionDrawable BarBackgroundEmpty = makeFilledRectangle(100, 30, Color.valueOf("#00000000"));
        
        selectBoxStyle = new SelectBox.SelectBoxStyle(font, Color.LIGHT_GRAY, BarBackgroundEmpty,
                new ScrollPane.ScrollPaneStyle(BarBackgroundGrey, BarBackgroundEmpty, BarBackgroundEmpty, BarBackgroundEmpty, BarBackgroundEmpty),
                new List.ListStyle(font, Color.CORAL, Color.SKY, BarBackgroundGrey));
        
        selectBoxStyle.overFontColor = Color.WHITE;
        
    }
    
    public Slider addSliderWithEditableLabel(final float[] sliderLimits, float defaultValue, TextField.TextFieldFilter textFieldFilter, final ChangeListener textFieldChangeListener, Stage stage, Group group, float x, float y, float width, float height) {
        final Slider slider = new Slider(sliderLimits[0], sliderLimits[1], 0.01f, false, sliderStyle);
        slider.setValue(defaultValue);
        final TextField textLabel = new TextField("" + formatNumber(2, defaultValue), textFieldStyle);
        textLabel.setTextFieldFilter(textFieldFilter);
        final boolean[] userInteracted = {false};
        slider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if(userInteracted[0] || forceUseProgrammaticEvents){
                    textLabel.setText("" + formatNumber(2, slider.getValue()));
                }
            }
        });
        slider.addListener(new ClickListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                userInteracted[0] = true;
                return super.touchDown(event, x, y, pointer, button);
            }
            
            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                userInteracted[0] = false;
                super.touchUp(event, x, y, pointer, button);
            }
        });
        textLabel.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if(!userInteracted[0] && !forceUseProgrammaticEvents){
                    try {
                        float value = Float.parseFloat(textLabel.getText());
                        if(value > sliderLimits[1] || value < sliderLimits[0]){
                            textLabel.setText(""+clamp(value ,sliderLimits[0], sliderLimits[1]));
                            textLabel.setCursorPosition(textLabel.getText().length());
                        }
                        slider.setValue(value);
                    } catch (Exception e) {
                        //ignore
                    }
                }
                if(!(textFieldChangeListener == null)){
                    textFieldChangeListener.changed(event, actor);
                }
            }
        });
        Table table = new Table();
        table.add(textLabel);
        table.add(slider).size(width, height);
        table.align(Align.right);
        table.setBounds(x, y, width + 50, height);
        if(!(stage == null)){
            stage.addActor(table);
        }
        if(!(group == null)){
            group.addActor(table);
        }
        return slider;
    }
    
}

