package net.smyler.smylib.gui.screen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.smyler.smylib.Identifier;
import net.smyler.smylib.gui.Font;
import net.smyler.smylib.gui.containers.FlexibleWidgetContainer;
import net.smyler.smylib.gui.containers.ScrollableWidgetContainer;
import net.smyler.smylib.gui.containers.WidgetContainer;
import net.smyler.smylib.Animation;
import net.smyler.smylib.Animation.AnimationState;
import net.smyler.smylib.Color;
import net.smyler.smylib.gui.sprites.Sprite;
import net.smyler.smylib.gui.widgets.MenuWidget;
import net.smyler.smylib.gui.widgets.SpriteWidget;
import net.smyler.smylib.gui.widgets.buttons.OptionButtonWidget;
import net.smyler.smylib.gui.widgets.buttons.TextButtonWidget;
import net.smyler.smylib.gui.widgets.buttons.SpriteButtonWidget;
import net.smyler.smylib.gui.widgets.buttons.SpriteButtonWidget.ButtonSprites;
import net.smyler.smylib.gui.widgets.buttons.ToggleButtonWidget;
import net.smyler.smylib.gui.widgets.sliders.FloatSliderWidget;
import net.smyler.smylib.gui.widgets.sliders.IntegerSliderWidget;
import net.smyler.smylib.gui.widgets.sliders.OptionSliderWidget;
import net.smyler.smylib.gui.widgets.text.TextAlignment;
import net.smyler.smylib.gui.widgets.text.TextFieldWidget;
import net.smyler.smylib.gui.widgets.text.TextWidget;
import net.smyler.smylib.json.TextJsonAdapter;
import net.smyler.smylib.text.ImmutableText;
import net.smyler.smylib.text.Text;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.lang.Math.max;
import static java.lang.Math.round;
import static java.util.Comparator.comparing;
import static net.smyler.smylib.Color.RED;
import static net.smyler.smylib.Color.WHITE;
import static net.smyler.smylib.SmyLib.getGameClient;
import static net.smyler.smylib.text.ImmutableText.ofPlainText;

public class TestScreen extends Screen {

    private final Screen parent;
    private final Animation animation;
    private int counter = 0;
    private final TextWidget fpsCounter;
    private final TextWidget focus;
    private TextWidget hovered;
    private TextWidget colored;
    private final TextFieldWidget textField;
    private TextButtonWidget testButton;
    private WidgetContainer[] subScreens;
    private int currentSubScreen = 0;

    private final SpriteButtonWidget previous;
    private final SpriteButtonWidget next;

    private final Gson textJsonParser = new GsonBuilder().registerTypeAdapter(Text.class, new TextJsonAdapter()).create();

    public TestScreen(Screen parent) {
        super(BackgroundOption.DEFAULT);
        this.parent = parent;
        this.animation = new Animation(5000); // We will use an animation to set the color of one of the displayed strings
        this.animation.start(AnimationState.CONTINUOUS_ENTER);
        this.next = new SpriteButtonWidget(10, ButtonSprites.RIGHT, this::nextPage);
        this.previous = new SpriteButtonWidget(10, ButtonSprites.LEFT, this::previousPage);

        this.fpsCounter = new TextWidget(10, ofPlainText("FPS: 0"), getGameClient().defaultFont());
        this.focus = new TextWidget(10, ofPlainText("Focused: null"), getGameClient().defaultFont());
        this.hovered = new TextWidget(10, ofPlainText("Hovered: null"), getGameClient().defaultFont());
        this.textField = new TextFieldWidget(1, "Text field", getGameClient().defaultFont());
        this.textField.setText("Write and right click");
        this.textField.setCursor(0);
    }

    @Override
    public void init() {
        super.init();

        this.removeAllWidgets(); // Remove the widgets that were already there
        this.cancelAllScheduled(); // Cancel all callbacks that were already there

        float width = this.getWidth();
        float height = this.getHeight();

        //Main screen
        WidgetContainer textScreen = new FlexibleWidgetContainer(20, 50, 1, width - 40, height - 70);
        WidgetContainer buttonScreen = new FlexibleWidgetContainer(20, 50, 1, width - 40, height - 70);
        WidgetContainer sliderScreen = new FlexibleWidgetContainer(20, 50, 1, width - 40, height - 70);
        WidgetContainer menuScreen = new FlexibleWidgetContainer(20, 50, 1, width - 40, height - 70);
        WidgetContainer jsonTextScreen = new FlexibleWidgetContainer(20, 50, 1, width - 40, height - 70);
        FlexibleWidgetContainer spriteScreenContent = new FlexibleWidgetContainer(20, 50, 1, width - 40, 10f);
        WidgetContainer spritesScreen = new ScrollableWidgetContainer(20, 50, 1, width - 40, height - 70, spriteScreenContent);
        this.subScreens = new WidgetContainer[] { textScreen, buttonScreen, sliderScreen, menuScreen, jsonTextScreen, spritesScreen};
        for(WidgetContainer container: this.subScreens) container.setDoScissor(false);

        TextWidget title = new TextWidget(width / 2f, 20, 10, ofPlainText("SmyLibGui demo test screen"), TextAlignment.CENTER, getGameClient().defaultFont());
        this.addWidget(title);
        this.addWidget(new SpriteButtonWidget(width - 20, 5, 10, ButtonSprites.CROSS, () -> getGameClient().displayScreen(this.parent)));
        this.addWidget(next.setX(width - 20).setY(height - 20));
        this.addWidget(previous.setX(5).setY(height - 20));
        this.addWidget(
                new TextButtonWidget(13, 13, 10, 100, "Reset screen",
                        () -> getGameClient().displayScreen(new TestScreen(this.parent)))
                );

        // === Text related stuff and general features examples === //
        this.hovered = new TextWidget(0, 50, 10, ofPlainText("Hovered: null"), getGameClient().defaultFont());

        TextWidget counterStr = new TextWidget(0, 100, 10, getGameClient().defaultFont());
        this.colored = new TextWidget(0, 120, 10, ofPlainText("Color animated text"), getGameClient().defaultFont());
        this.colored.setBaseColor(animation.rainbowColor());
        textScreen.addWidget(fpsCounter.setAnchorX(0).setAnchorY(10));
        textScreen.addWidget(focus.setAnchorX(0).setAnchorY(30));
        textScreen.addWidget(hovered);
        this.textField.setIsSearchBar(true);
        textScreen.addWidget(this.textField.setX(0).setY(70).setWidth(150).setOnPressEnterCallback(s -> {this.textField.setText("You pressed enter :)"); return true;}));
        textScreen.addWidget(counterStr);
        textScreen.addWidget(colored);
        Text compo = this.textJsonParser.fromJson(
                "[\"\",{\"text\":\"This is red, with a hover event\",\"color\":\"dark_red\",\"hoverEvent\":{\"action\":\"show_text\",\"contents\":\"Yup, it's red.\"}},{\"text\":\", I said it's \",\"color\":\"dark_red\"},{\"text\":\"red\",\"color\":\"red\"},{\"text\":\",\",\"color\":\"dark_red\"},{\"text\":\" \",\"color\":\"dark_green\"},{\"text\":\"and this is \",\"color\":\"dark_green\",\"hoverEvent\":{\"action\":\"show_text\",\"contents\":\"Green enough for you?\"}},{\"text\":\"green\",\"color\":\"green\",\"hoverEvent\":{\"action\":\"show_text\",\"contents\":\"Green enough for you?\"}},{\"text\":\", with another hover event\",\"color\":\"dark_green\",\"hoverEvent\":{\"action\":\"show_text\",\"contents\":\"Green enough for you?\"}},{\"text\":\".\",\"color\":\"dark_green\"},{\"text\":\" Don't you trust me?\",\"color\":\"gray\"},{\"text\":\" This is \",\"color\":\"dark_green\"},{\"text\":\"green\",\"color\":\"green\"},{\"text\":\"! \",\"color\":\"dark_green\"},{\"text\":\"And this is \",\"color\":\"dark_blue\",\"clickEvent\":{\"action\":\"open_url\",\"value\":\"https://smyler.net\"}},{\"text\":\"blue\",\"color\":\"blue\",\"clickEvent\":{\"action\":\"open_url\",\"value\":\"https://smyler.net\"}},{\"text\":\", with a \",\"color\":\"dark_blue\",\"clickEvent\":{\"action\":\"open_url\",\"value\":\"https://smyler.net\"}},{\"text\":\"click event\",\"color\":\"dark_gray\",\"clickEvent\":{\"action\":\"open_url\",\"value\":\"https://smyler.net\"}},{\"text\":\"!\",\"color\":\"dark_blue\",\"clickEvent\":{\"action\":\"open_url\",\"value\":\"https://smyler.net\"}},{\"text\":\" And finally, this is \",\"color\":\"gold\"},{\"text\":\"black\",\"color\":\"black\"},{\"text\":\" and \",\"color\":\"gold\"},{\"text\":\"white\",\"color\":\"white\"},{\"text\":\", \",\"color\":\"gold\"},{\"text\":\"and it has \",\"color\":\"light_purple\"},{\"text\":\"various\",\"bold\":true,\"color\":\"dark_purple\"},{\"text\":\" styles\",\"italic\":true,\"color\":\"light_purple\"},{\"text\":\". \",\"color\":\"light_purple\"},{\"text\":\"And finally\",\"underlined\":true,\"color\":\"light_purple\"},{\"text\":\", I bet you \",\"color\":\"light_purple\"},{\"text\":\"can't\",\"strikethrough\":true,\"color\":\"light_purple\"},{\"text\":\" read \",\"color\":\"light_purple\"},{\"text\":\"that\",\"obfuscated\":true,\"color\":\"yellow\"},{\"text\":\".\",\"color\":\"light_purple\"}]",
                Text.class
        );
        textScreen.addWidget(new TextWidget(textScreen.getWidth()/2, 140, 1, compo, TextAlignment.CENTER, getGameClient().defaultFont().withScale(2)).setMaxWidth(textScreen.getWidth()).setBackgroundColor(Color.DARK_OVERLAY).setPadding(10));

        // === Button screen: examples on how to use button widgets === //

        this.testButton = new TextButtonWidget(0, 0, 1, 150, "Click me!",
                () -> this.testButton.setText("Nice, double click me now!"),
                () -> {
                    this.testButton.setText("I'm done now :(");
                    this.testButton.disable();
                }
                );
        buttonScreen.addWidget(testButton);
        buttonScreen.addWidget(new SpriteButtonWidget(0, 30, 1, ButtonSprites.BLANK_15, null));
        buttonScreen.addWidget(new SpriteButtonWidget(30, 30, 1, ButtonSprites.CROSS, null));
        buttonScreen.addWidget(new SpriteButtonWidget(30, 30, 1, ButtonSprites.PLUS, null));
        buttonScreen.addWidget(new SpriteButtonWidget(60, 30, 1, ButtonSprites.MINUS, null));
        buttonScreen.addWidget(new SpriteButtonWidget(90, 30, 1, ButtonSprites.LEFT, null));
        buttonScreen.addWidget(new SpriteButtonWidget(120, 30, 1, ButtonSprites.UP, null));
        buttonScreen.addWidget(new SpriteButtonWidget(150, 30, 1, ButtonSprites.DOWN, null));
        buttonScreen.addWidget(new SpriteButtonWidget(180, 30, 1, ButtonSprites.RIGHT, null));
        ToggleButtonWidget tb1 = new ToggleButtonWidget(0, 60, 1, true);
        buttonScreen.addWidget(tb1);
        buttonScreen.addWidget(new ToggleButtonWidget(30, 60, 1, true, tb1::setEnabled));
        buttonScreen.addWidget(new OptionButtonWidget<>(0, 90, 2, 150, new String[]{"Option 1", "Option 2", "Option 3", "Option 4"}));


        // === Slider screen: examples on how to use slider widgets === //

        sliderScreen.addWidget(new IntegerSliderWidget(0, 0, 1, 150, 0, 100, 50));
        sliderScreen.addWidget(new FloatSliderWidget(0, 30, 1, 150, 0, 1, 0.5));
        sliderScreen.addWidget(new OptionSliderWidget<>(0, 60, 1, 150, new String[]{"Option 1", "Option 2", "Option 3", "Option 4"}));
        sliderScreen.addWidget(new IntegerSliderWidget(0, 90, 1, 150, 30, 0, 100, 50));
        sliderScreen.addWidget(new IntegerSliderWidget(0, 140, 1, 150, 10, 0, 100, 50));


        // === Menu screen: example on how to use menu widgets === //

        MenuWidget rcm = new MenuWidget(50, getGameClient().defaultFont()); //This will be used as our right click menu, the following are it's sub menus
        MenuWidget animationMenu = new MenuWidget(1, getGameClient().defaultFont());
        MenuWidget here = new MenuWidget(50, getGameClient().defaultFont());
        MenuWidget is = new MenuWidget(50, getGameClient().defaultFont());
        MenuWidget a = new MenuWidget(50, getGameClient().defaultFont());
        MenuWidget very = new MenuWidget(50, getGameClient().defaultFont());
        MenuWidget nested = new MenuWidget(50, getGameClient().defaultFont());
        animationMenu.addEntry("Show", () -> animation.start(AnimationState.ENTER));
        animationMenu.addEntry("Hide", () -> animation.start(AnimationState.LEAVE));
        animationMenu.addEntry("Flash", () -> animation.start(AnimationState.FLASH));
        animationMenu.addEntry("Continuous", () -> animation.start(AnimationState.CONTINUOUS_ENTER));
        animationMenu.addEntry("Continuous backward", () -> animation.start(AnimationState.CONTINUOUS_LEAVE));
        animationMenu.addEntry("Back and forth", () -> animation.start(AnimationState.BACK_AND_FORTH));
        animationMenu.addEntry("Stop", () -> animation.start(AnimationState.STOPPED));
        rcm.addEntry("Close", () -> getGameClient().displayScreen(this.parent));
        rcm.addEntry("Disabled Entry");
        rcm.addEntry("Here", here);
        here.addEntry("is", is);
        is.addEntry("a", a);
        a.addEntry("very", very);
        very.addEntry("nested", nested);
        nested.addEntry("menu");
        rcm.addSeparator();
        rcm.addEntry("Animation", animationMenu);
        rcm.useAsRightClick(); // Calling this tells the menu to open whenever it's parent screen is right-clicked
        menuScreen.addWidget(new TextWidget(menuScreen.getWidth() / 2, menuScreen.getHeight() / 2, 1, ofPlainText("Please right click anywhere"), TextAlignment.CENTER, getGameClient().defaultFont()));
        menuScreen.addWidget(rcm);

        // ==== JSON text parsing screen ==== //
        final TextFieldWidget inputField = new TextFieldWidget(0, 0, 0, jsonTextScreen.getWidth(), getGameClient().defaultFont());
        final TextWidget text = new TextWidget(
                jsonTextScreen.getWidth() / 2,
                (jsonTextScreen.getHeight() - inputField.getHeight()) / 2,
                0,
                ImmutableText.EMPTY,
                TextAlignment.CENTER, getGameClient().defaultFont()
        );
        jsonTextScreen.addWidget(inputField);
        jsonTextScreen.addWidget(text);

        jsonTextScreen.scheduleBeforeEachUpdate(() -> {
            try {
                Text component = this.textJsonParser.fromJson(inputField.getText(), Text.class);
                if (component == null) {
                    throw new JsonParseException("");
                }
                text.setText(component);
                text.setAnchorY((jsonTextScreen.getHeight() - inputField.getHeight() - text.getHeight()) / 2 + inputField.getHeight());
                inputField.setFocusedTextColor(WHITE);
            } catch (JsonParseException e) {
                inputField.setFocusedTextColor(RED);
            }
        });

        // ==== Sprites ==== /
        float y = 0f;
        Font font = getGameClient().defaultFont();
        List<Map.Entry<Identifier, Sprite>> sprites = getGameClient().sprites().getSprites()
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey(comparing(Identifier::toString)))
                .collect(Collectors.toList());
        for (Map.Entry<Identifier, Sprite> entry: sprites) {
            Text id = ImmutableText.ofPlainText(entry.getKey().toString());
            Sprite sprite = entry.getValue();
            float entryHeight = round(max(font.height(), sprite.height())) + 10f;
            spriteScreenContent.addWidget(new TextWidget(
                    0, y + (entryHeight - font.height()) / 2, 0,
                    id,
                    font
            ));
            SpriteWidget spriteWidget = new SpriteWidget(
                    font.computeWidth(id) + 10f, (float) (y + (entryHeight - sprite.height()) / 2), 0,
                    sprite
            );
            spriteScreenContent.addWidget(spriteWidget);
            spriteScreenContent.setWidth(max(spriteScreenContent.getWidth(), spriteWidget.getX() + spriteWidget.getWidth() + 10f));
            y += entryHeight;
        }
        spriteScreenContent.setHeight(y + 10f);
        spritesScreen.setDoScissor(true);


        // ==== Getting everything ready and setting up scheduled tasks === //

        this.addWidget(subScreens[this.currentSubScreen]); // A screen is also a widget, that allows for a lot of flexibility

        // Same as Javascript's setInterval
        this.scheduleAtIntervalBeforeUpdate(() -> counterStr.setText(ofPlainText("Scheduled callback called " + this.counter++)), 1000);
        this.scheduleBeforeEachUpdate(() -> { // Called at every update
            this.animation.update();
            this.fpsCounter.setText(ofPlainText("FPS: " + getGameClient().currentFPS()));
            this.focus.setText(ofPlainText("Focused: " + this.getFocusedWidget()));
            this.hovered.setText(ofPlainText("Hovered: " + this.getHoveredWidget()));
            this.colored.setBaseColor(animation.rainbowColor());
        });
        this.updateButtons();
    }

    private void nextPage() {
        this.removeWidget(this.subScreens[this.currentSubScreen]);
        this.currentSubScreen++;
        this.addWidget(this.subScreens[this.currentSubScreen]);
        this.updateButtons();
    }

    private void previousPage() {
        this.removeWidget(this.subScreens[this.currentSubScreen]);
        this.currentSubScreen--;
        this.addWidget(this.subScreens[this.currentSubScreen]);
        this.updateButtons();
    }

    private void updateButtons() {
        if(this.currentSubScreen <= 0) this.previous.disable();
        else this.previous.enable();
        if(this.currentSubScreen >= this.subScreens.length - 1) this.next.disable();
        else this.next.enable();
    }


}
