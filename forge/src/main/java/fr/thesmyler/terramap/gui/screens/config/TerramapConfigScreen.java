package fr.thesmyler.terramap.gui.screens.config;

import java.awt.Desktop;
import java.io.IOException;
import java.util.Set;

import net.smyler.smylib.game.GameClient;
import net.smyler.smylib.game.Key;
import net.smyler.smylib.game.Translator;
import org.jetbrains.annotations.Nullable;

import net.smyler.smylib.gui.containers.FlexibleWidgetContainer;
import net.smyler.smylib.gui.containers.WidgetContainer;
import net.smyler.smylib.gui.screen.BackgroundOption;
import net.smyler.smylib.gui.screen.Screen;
import net.smyler.smylib.gui.widgets.buttons.TextButtonWidget;
import net.smyler.smylib.gui.widgets.buttons.SpriteButtonWidget;
import net.smyler.smylib.gui.widgets.buttons.SpriteButtonWidget.ButtonSprites;
import net.smyler.smylib.gui.widgets.buttons.ToggleButtonWidget;
import net.smyler.smylib.gui.widgets.sliders.IntegerSliderWidget;
import net.smyler.smylib.gui.widgets.sliders.OptionSliderWidget;
import net.smyler.smylib.gui.widgets.text.TextAlignment;
import net.smyler.smylib.gui.widgets.text.TextFieldWidget;
import net.smyler.smylib.gui.widgets.text.TextWidget;
import fr.thesmyler.terramap.TerramapClientContext;
import fr.thesmyler.terramap.TerramapMod;
import fr.thesmyler.terramap.TerramapConfig;
import fr.thesmyler.terramap.maps.raster.MapStylesLibrary;

import static net.smyler.smylib.SmyLib.getGameClient;
import static net.smyler.smylib.text.ImmutableText.ofPlainText;
import static net.smyler.smylib.text.ImmutableText.ofTranslation;

public class TerramapConfigScreen extends Screen {

    private final Screen parent;
    private FlexibleWidgetContainer[] pages;
    private String[] titles;
    private TextWidget title;
    private int currentSubScreen = 0;
    private final SpriteButtonWidget next = new SpriteButtonWidget(10, ButtonSprites.RIGHT, this::nextPage);
    private final SpriteButtonWidget previous = new SpriteButtonWidget(10, ButtonSprites.LEFT, this::previousPage);
    private final ToggleButtonWidget unlockZoomToggle = new ToggleButtonWidget(10, false);
    private final ToggleButtonWidget saveUIStateToggle = new ToggleButtonWidget(10, false);
    private final ToggleButtonWidget showChatOnMapToggle = new ToggleButtonWidget(10, false);
    private final OptionSliderWidget<TileScalingOption> tileScalingSlider = new OptionSliderWidget<>(10, TileScalingOption.values());
    private final IntegerSliderWidget doubleClickDelaySlider = new IntegerSliderWidget(10, TerramapConfig.CLIENT.DOUBLE_CLICK_DELAY_MIN, TerramapConfig.CLIENT.DOUBLE_CLICK_DELAY_MAX, TerramapConfig.CLIENT.DOUBLE_CLICK_DELAY_DEFAULT);
    private final IntegerSliderWidget maxLoadedTilesSlider = new IntegerSliderWidget(10, TerramapConfig.CLIENT.TILE_LOAD_MIN, TerramapConfig.CLIENT.TILE_LOAD_MAX, TerramapConfig.CLIENT.TILE_LOAD_DEFAULT);
    private final IntegerSliderWidget lowZoomLevelSlider = new IntegerSliderWidget(10, TerramapConfig.CLIENT.LOW_ZOOM_LEVEL_MIN, TerramapConfig.CLIENT.LOW_ZOOM_LEVEL_MAX, TerramapConfig.CLIENT.LOW_ZOOM_LEVEL_DEFAULT);
    private final ToggleButtonWidget debugMapStylesToggle = new ToggleButtonWidget(10, false);
    private TextButtonWidget reloadMapStylesButton;
    private final TextFieldWidget tpCommandField;
    private final TextWidget pageText;

    public TerramapConfigScreen(Screen parent) {
        super(BackgroundOption.DEFAULT);
        this.parent = parent;
        this.pageText = new TextWidget(10, getGameClient().defaultFont());
        this.tpCommandField = new TextFieldWidget(10, getGameClient().defaultFont()).setWidth(200);
        this.reset();
    }

    @Override
    public void init() {
        GameClient game = getGameClient();
        Translator translator = game.translator();

        float width = this.getWidth();
        float height = this.getHeight();

        int inter = 9;
        WidgetContainer content = this.getContent();
        content.removeAllWidgets(); //Remove the widgets that were already there
        content.cancelAllScheduled(); //Cancel all callbacks that were already there
        this.title = new TextWidget(width / 2f, 10, 5, ofTranslation("terramap.configmenu.title"), TextAlignment.CENTER, game.defaultFont());
        content.addWidget(this.title);
        TextButtonWidget save = new TextButtonWidget(
                width / 2f + 30, height - 30, 10,
                100,
                translator.format("terramap.configmenu.save"),
                this::saveAndClose);
        TextButtonWidget cancel = new TextButtonWidget(
                width / 2f - 130, save.getY(), save.getZ(),
                save.getWidth(),
                translator.format("terramap.configmenu.cancel"),
                this::close);
        TextButtonWidget reset = new TextButtonWidget(
                width / 2f - 25, save.getY(), save.getZ(),
                50,
                translator.format("terramap.configmenu.reset"),
                this::reset);
        content.addWidget(save.setTooltip(translator.format("terramap.configmenu.save.tooltip")));
        content.addWidget(cancel.setTooltip(translator.format("terramap.configmenu.cancel")));
        content.addWidget(reset.setTooltip(translator.format("terramap.configmenu.reset")));
        content.addWidget(this.next.setX(save.getX() + save.getWidth() + 5).setY(save.getY() + 2));
        content.addWidget(this.previous.setX(cancel.getX() - 20).setY(this.next.getY()));
        FlexibleWidgetContainer mapConfigScreen = new FlexibleWidgetContainer(20, 20, 1, width - 40, height - 75);
        FlexibleWidgetContainer mapStylesConfigScreen = new FlexibleWidgetContainer(20, 20, 1, width - 40, height - 75);
        FlexibleWidgetContainer otherConfigScreen = new FlexibleWidgetContainer(20, 20, 1, width - 40, height - 75);
        this.pages = new FlexibleWidgetContainer[] {
                mapConfigScreen,
                mapStylesConfigScreen,
                otherConfigScreen
        };
        this.titles = new String[] {
                translator.format("terramap.configmenu.title.mapsettings"),
                translator.format("terramap.configmenu.title.mapstyles"),
                translator.format("terramap.configmenu.title.other")
        };

        // Map settings
        TextWidget unlockZoomText = new TextWidget(10, ofTranslation("terramap.configmenu.unlockzoom"), TextAlignment.RIGHT, game.defaultFont());
        unlockZoomText.setAnchorX((mapConfigScreen.getWidth() - unlockZoomText.getWidth() - this.unlockZoomToggle.getWidth()) / 2f - 71f).setAnchorY(height / 4f - 30);
        mapConfigScreen.addWidget(unlockZoomText);
        this.unlockZoomToggle.setTooltip(translator.format("terramap.configmenu.unlockzoom.tooltip"));
        mapConfigScreen.addWidget(this.unlockZoomToggle.setX(unlockZoomText.getX() + unlockZoomText.getWidth() + 5).setY(unlockZoomText.getAnchorY() - 4));
        TextWidget saveUIStateText = new TextWidget(10, ofTranslation("terramap.configmenu.saveui"), TextAlignment.RIGHT, game.defaultFont());
        this.saveUIStateToggle.setTooltip(translator.format("terramap.configmenu.saveui.tooltip"));
        saveUIStateText.setAnchorX((mapConfigScreen.getWidth() - saveUIStateText.getWidth() - this.saveUIStateToggle.getWidth()) / 2f + 64f).setAnchorY(unlockZoomText.getAnchorY());
        mapConfigScreen.addWidget(saveUIStateText);
        mapConfigScreen.addWidget(this.saveUIStateToggle.setX(saveUIStateText.getX() + saveUIStateText.getWidth() + 5).setY(saveUIStateText.getAnchorY() - 4));
        TextWidget chatOnMapText = new TextWidget(10, ofTranslation("terramap.configmenu.chatonmap"), TextAlignment.RIGHT, game.defaultFont());
        chatOnMapText.setAnchorX(unlockZoomText.getAnchorX()).setAnchorY(unlockZoomText.getAnchorY() + unlockZoomText.getFont().height() + 10);
        mapConfigScreen.addWidget(chatOnMapText);
        this.showChatOnMapToggle.setTooltip(translator.format("terramap.configmenu.chatonmap.tooltip"));
        mapConfigScreen.addWidget(
                this.showChatOnMapToggle
                        .setX(chatOnMapText.getX() + chatOnMapText.getWidth() + 9).setY(chatOnMapText.getAnchorY() - 4));
        mapConfigScreen.addWidget(
                this.tileScalingSlider
                        .setX(mapConfigScreen.getWidth()/2 - 130).setY(this.showChatOnMapToggle.getY() + this.unlockZoomToggle.getHeight() + inter)
                        .setWidth(125)
                        .setDisplayPrefix(translator.format("terramap.configmenu.tilescaling")));
        mapConfigScreen.addWidget(
                this.doubleClickDelaySlider
                    .setX(mapConfigScreen.getWidth()/2 + 5).setY(this.tileScalingSlider.getY())
                        .setWidth(this.tileScalingSlider.getWidth())
                        .setDisplayPrefix(translator.format("terramap.configmenu.doubleclick")));
        this.maxLoadedTilesSlider.setTooltip(translator.format("terramap.configmenu.tilecache.tooltip"));
        mapConfigScreen.addWidget(
                this.maxLoadedTilesSlider
                        .setX(mapConfigScreen.getWidth()/2 - 130).setY(this.doubleClickDelaySlider.getY() + this.doubleClickDelaySlider.getHeight() + inter)
                        .setWidth(125)
                        .setDisplayPrefix(translator.format("terramap.configmenu.tilecache")));
        this.lowZoomLevelSlider.setTooltip(translator.format("terramap.configmenu.lowzoom.tooltip"));
        mapConfigScreen.addWidget(this.lowZoomLevelSlider
                .setX(mapConfigScreen.getWidth()/2 + 5).setY(this.maxLoadedTilesSlider.getY())
                .setWidth(this.maxLoadedTilesSlider.getWidth())
                .setDisplayPrefix(translator.format("terramap.configmenu.lowzoom")));
        TextButtonWidget hudButton = new TextButtonWidget(
                mapConfigScreen.getWidth() / 2 - 100, this.lowZoomLevelSlider.getY() + this.lowZoomLevelSlider.getHeight() + inter, 10,
                200,
                translator.format("terramap.configmenu.configureminimap"), () -> getGameClient().displayScreen(new HudConfigScreen()));
        hudButton.setTooltip(translator.format("terramap.configmenu.configureminimap.tooltip"));
        mapConfigScreen.addWidget(hudButton);

        // Map styles
        TextWidget debugMapStylesText = new TextWidget(10, ofTranslation("terramap.configmenu.debugmapstyles"), game.defaultFont());
        mapStylesConfigScreen.addWidget(debugMapStylesText.setAnchorX((mapStylesConfigScreen.getWidth() - debugMapStylesToggle.getWidth() - debugMapStylesText.getWidth() - 3) / 2).setAnchorY(mapStylesConfigScreen.getHeight() / 4 - 30));
        debugMapStylesToggle.setTooltip(translator.format("terramap.configmenu.debugmapstyles.tooltip"));
        mapStylesConfigScreen.addWidget(debugMapStylesToggle.setX(debugMapStylesText.getX() + debugMapStylesText.getWidth() + 3).setY(debugMapStylesText.getY() - 4));
        Set<String> baseIDs = MapStylesLibrary.getBaseMaps().keySet();
        Set<String> userIDs = MapStylesLibrary.getUserMaps().keySet();
        Set<String> serverIDs = TerramapClientContext.getContext().getServerMapStyles().keySet();
        Set<String> proxyIDs = TerramapClientContext.getContext().getProxyMapStyles().keySet();
        Set<String> resolved = TerramapClientContext.getContext().getMapStyles().keySet();
        TextWidget baseText = new TextWidget(mapStylesConfigScreen.getWidth() / 2, 40, 10, ofTranslation("terramap.configmenu.mapstyles.base", baseIDs.size(), String.join(", ", baseIDs)), TextAlignment.CENTER, getGameClient().defaultFont());
        TextWidget proxyText = new TextWidget(mapStylesConfigScreen.getWidth() / 2, 57, 10, ofTranslation("terramap.configmenu.mapstyles.proxy", proxyIDs.size(), String.join(", ", proxyIDs)), TextAlignment.CENTER, getGameClient().defaultFont());
        TextWidget serverText = new TextWidget(mapStylesConfigScreen.getWidth() / 2, 74, 10, ofTranslation("terramap.configmenu.mapstyles.server", serverIDs.size(), String.join(", ", serverIDs)), TextAlignment.CENTER, getGameClient().defaultFont());
        TextWidget userText = new TextWidget( mapStylesConfigScreen.getWidth() / 2, 91, 10, ofTranslation("terramap.configmenu.mapstyles.custom", userIDs.size(), String.join(", ", userIDs)),TextAlignment.CENTER, getGameClient().defaultFont());
        TextWidget effectiveText = new TextWidget(mapStylesConfigScreen.getWidth() / 2, 108, 10, ofTranslation("terramap.configmenu.mapstyles.effective", resolved.size(), String.join(", ", resolved)), TextAlignment.CENTER, getGameClient().defaultFont());
        mapStylesConfigScreen.addWidget(baseText.setMaxWidth(mapConfigScreen.getWidth()).setAnchorY(debugMapStylesToggle.getY() + debugMapStylesToggle.getHeight() + 10));
        mapStylesConfigScreen.addWidget(proxyText.setMaxWidth(mapConfigScreen.getWidth()).setAnchorY(baseText.getY() + baseText.getHeight() + inter));
        mapStylesConfigScreen.addWidget(serverText.setMaxWidth(mapConfigScreen.getWidth()).setAnchorY(proxyText.getY() + proxyText.getHeight() + inter));
        mapStylesConfigScreen.addWidget(userText.setMaxWidth(mapConfigScreen.getWidth()).setAnchorY(serverText.getY() + serverText.getHeight() + inter));
        mapStylesConfigScreen.addWidget(effectiveText.setMaxWidth(mapConfigScreen.getWidth()).setAnchorY(userText.getY() + userText.getHeight() + inter));
        this.reloadMapStylesButton = new TextButtonWidget(mapStylesConfigScreen.getWidth() / 2 - 153, (effectiveText.getY() + effectiveText.getHeight() + mapStylesConfigScreen.getHeight()) / 2 - 10, 10, 150, translator.format("terramap.configmenu.mapstyles.reload"), () -> {MapStylesLibrary.reload(); TerramapConfigScreen.this.init();});
        mapStylesConfigScreen.addWidget(this.reloadMapStylesButton);
        mapStylesConfigScreen.addWidget(new TextButtonWidget(this.reloadMapStylesButton.getX() + this.reloadMapStylesButton.getWidth() + 3, this.reloadMapStylesButton.getY(), 10, 150, translator.format("terramap.configmenu.mapstyles.open"), () ->  {
            try {
                Desktop.getDesktop().open(MapStylesLibrary.getFile());
            } catch (IOException e) {
                TerramapMod.logger.error("Failed to open map style config file: ");
                TerramapMod.logger.catching(e);
            }
        }));

        // Other config screen
        TextWidget tpCommandText = new TextWidget(10, ofTranslation("terramap.configmenu.teleportcmd"), TextAlignment.RIGHT, getGameClient().defaultFont());
        otherConfigScreen.addWidget(tpCommandText.setAnchorX((otherConfigScreen.getWidth() - this.tpCommandField.getWidth() - tpCommandText.getWidth()) / 2).setAnchorY(60));
        otherConfigScreen.addWidget(this.tpCommandField.setX(tpCommandText.getX() + tpCommandText.getWidth() + inter).setY(tpCommandText.getY() - 7));

        // Footer
        this.getContent().addWidget(this.pages[this.currentSubScreen]);
        this.title.setText(ofPlainText(this.titles[this.currentSubScreen]));
        this.getContent().addWidget(this.pageText.setAnchorX(width / 2f).setAnchorY(height - 45f).setAlignment(TextAlignment.CENTER));
        this.updateButtons();
    }

    private void nextPage() {
        this.getContent().removeWidget(this.pages[this.currentSubScreen]);
        this.currentSubScreen++;
        this.getContent().addWidget(this.pages[this.currentSubScreen]);
        this.title.setText(ofPlainText(this.titles[currentSubScreen]));
        this.updateButtons();
    }

    private void previousPage() {
        this.getContent().removeWidget(this.pages[this.currentSubScreen]);
        this.currentSubScreen--;
        this.getContent().addWidget(this.pages[this.currentSubScreen]);
        this.title.setText(ofPlainText(this.titles[currentSubScreen]));
        this.updateButtons();
    }

    private void updateButtons() {
        if(this.currentSubScreen <= 0) this.previous.disable();
        else this.previous.enable();
        if(this.currentSubScreen >= this.pages.length - 1) this.next.disable();
        else this.next.enable();
        this.pageText.setText(ofTranslation("terramap.configmenu.pagenumber", this.currentSubScreen + 1, this.pages.length));
    }

    private void saveAndClose() {
        TerramapConfig.CLIENT.tileScaling = this.tileScalingSlider.getCurrentOption().value;
        TerramapConfig.CLIENT.unlockZoom = this.unlockZoomToggle.getState();
        TerramapConfig.CLIENT.saveUiState = this.saveUIStateToggle.getState();
        TerramapConfig.CLIENT.chatOnMap = this.showChatOnMapToggle.getState();
        TerramapConfig.CLIENT.doubleClickDelay = (int) this.doubleClickDelaySlider.getValue();
        TerramapConfig.CLIENT.maxTileLoad = (int) this.maxLoadedTilesSlider.getValue();
        TerramapConfig.CLIENT.lowZoomLevel = (int) this.lowZoomLevelSlider.getValue();
        TerramapConfig.tpllcmd = this.tpCommandField.getText();
        TerramapConfig.enableDebugMaps = this.debugMapStylesToggle.getState();
        TerramapConfig.sync();
        this.close();
    }

    private void close() {
        getGameClient().displayScreen(this.parent);
    }

    private void reset() {
        this.tileScalingSlider.setCurrentOption(TileScalingOption.getFromValue(TerramapConfig.CLIENT.tileScaling));
        this.unlockZoomToggle.setState(TerramapConfig.CLIENT.unlockZoom);
        this.saveUIStateToggle.setState(TerramapConfig.CLIENT.saveUiState);
        this.showChatOnMapToggle.setState(TerramapConfig.CLIENT.chatOnMap);
        this.doubleClickDelaySlider.setValue(TerramapConfig.CLIENT.doubleClickDelay);
        this.maxLoadedTilesSlider.setValue(TerramapConfig.CLIENT.maxTileLoad);
        this.lowZoomLevelSlider.setValue(TerramapConfig.CLIENT.lowZoomLevel);
        this.tpCommandField.setText(TerramapConfig.tpllcmd);
        this.debugMapStylesToggle.setState(TerramapConfig.enableDebugMaps);
    }

    @Override
    public void onKeyTyped(char typedChar, Key key, @Nullable WidgetContainer parent) {
        if (key == Key.KEY_ESCAPE) {
            getGameClient().displayScreen(new ConfirmScreen());
        } else {
            super.onKeyTyped(typedChar, key, parent);
        }
    }

    private class ConfirmScreen extends Screen {

        public ConfirmScreen() {
            super(BackgroundOption.DEFAULT);
        }

        @Override
        public void init() {
            Translator translator = getGameClient().translator();
            WidgetContainer content = this.getContent();
            content.removeAllWidgets();
            content.cancelAllScheduled();
            TextWidget text = new TextWidget(content.getWidth() / 2, content.getHeight() / 2 - 20, 10, ofTranslation("terramap.configmenu.asksave.prompt"), TextAlignment.CENTER, getGameClient().defaultFont());
            content.addWidget(text);
            content.addWidget(new TextButtonWidget(
                    content.getWidth() / 2 - 125, text.getY() + text.getHeight() + 15, 10,
                    80,
                    translator.format("terramap.configmenu.asksave.deny"),
                    TerramapConfigScreen.this::close));
            content.addWidget(new TextButtonWidget(
                    content.getWidth() / 2 - 40, text.getY() + text.getHeight() + 15, 10,
                    80,
                    translator.format("terramap.configmenu.asksave.cancel"),
                    () -> getGameClient().displayScreen(TerramapConfigScreen.this)));
            content.addWidget(new TextButtonWidget(
                    content.getWidth() / 2 + 45, text.getY() + text.getHeight() + 15, 10,
                    80,
                    translator.format("terramap.configmenu.asksave.confirm"),
                    TerramapConfigScreen.this::saveAndClose));
        }
    }

    protected enum TileScalingOption {

        AUTO(0), POINT5(0.5), ONE(1), TWO(2), FOUR(4), HEIGHT(8);

        final double value;

        TileScalingOption(double v) {
            this.value = v;
        }

        static TileScalingOption getFromValue(double val) {
            for(TileScalingOption o: TileScalingOption.values()) {
                if(o.value == val) return o;
            }
            return AUTO;
        }

        @Override
        public String toString() {
            if(this == AUTO) {
                return getGameClient().translator().format("terramap.configmenu.tilescaling.auto");
            }
            return String.valueOf(this.value);
        }
    }

}
