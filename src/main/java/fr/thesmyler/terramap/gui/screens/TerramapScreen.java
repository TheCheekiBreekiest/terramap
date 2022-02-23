package fr.thesmyler.terramap.gui.screens;

import java.util.*;
import java.util.function.Consumer;

import javax.annotation.Nullable;

import fr.thesmyler.terramap.gui.widgets.map.MapController;
import fr.thesmyler.terramap.util.geo.*;
import net.buildtheearth.terraplusplus.generator.EarthGeneratorSettings;
import net.minecraft.util.text.*;
import org.lwjgl.input.Keyboard;

import fr.thesmyler.smylibgui.SmyLibGui;
import fr.thesmyler.smylibgui.container.FlexibleWidgetContainer;
import fr.thesmyler.smylibgui.container.ScrollableWidgetContainer;
import fr.thesmyler.smylibgui.container.SlidingPanelWidget;
import fr.thesmyler.smylibgui.container.SlidingPanelWidget.PanelTarget;
import fr.thesmyler.smylibgui.container.WidgetContainer;
import fr.thesmyler.smylibgui.screen.BackgroundOption;
import fr.thesmyler.smylibgui.screen.MultiChoicePopupScreen;
import fr.thesmyler.smylibgui.screen.PopupScreen;
import fr.thesmyler.smylibgui.screen.Screen;
import fr.thesmyler.smylibgui.util.Color;
import fr.thesmyler.smylibgui.util.Font;
import fr.thesmyler.smylibgui.util.RenderUtil;
import fr.thesmyler.smylibgui.util.Util;
import fr.thesmyler.smylibgui.widgets.AbstractSolidWidget;
import fr.thesmyler.smylibgui.widgets.ChatWidget;
import fr.thesmyler.smylibgui.widgets.IWidget;
import fr.thesmyler.smylibgui.widgets.ScrollbarWidget;
import fr.thesmyler.smylibgui.widgets.ScrollbarWidget.ScrollbarOrientation;
import fr.thesmyler.smylibgui.widgets.WarningWidget;
import fr.thesmyler.smylibgui.widgets.buttons.AbstractButtonWidget;
import fr.thesmyler.smylibgui.widgets.buttons.TextButtonWidget;
import fr.thesmyler.smylibgui.widgets.buttons.TexturedButtonWidget;
import fr.thesmyler.smylibgui.widgets.buttons.TexturedButtonWidget.IncludedTexturedButtons;
import fr.thesmyler.smylibgui.widgets.text.TextAlignment;
import fr.thesmyler.smylibgui.widgets.text.TextFieldWidget;
import fr.thesmyler.smylibgui.widgets.text.TextWidget;
import fr.thesmyler.terramap.MapContext;
import fr.thesmyler.terramap.TerramapClientContext;
import fr.thesmyler.terramap.TerramapMod;
import fr.thesmyler.terramap.config.TerramapConfig;
import fr.thesmyler.terramap.gui.screens.config.TerramapConfigScreen;
import fr.thesmyler.terramap.gui.widgets.CircularCompassWidget;
import fr.thesmyler.terramap.gui.widgets.map.MapLayer;
import fr.thesmyler.terramap.gui.widgets.map.MapWidget;
import fr.thesmyler.terramap.gui.widgets.map.layer.RasterMapLayer;
import fr.thesmyler.terramap.gui.widgets.markers.controllers.FeatureVisibilityController;
import fr.thesmyler.terramap.gui.widgets.markers.markers.Marker;
import fr.thesmyler.terramap.gui.widgets.markers.markers.entities.MainPlayerMarker;
import fr.thesmyler.terramap.input.KeyBindings;
import fr.thesmyler.terramap.maps.raster.CachingRasterTiledMap;
import fr.thesmyler.terramap.maps.raster.IRasterTiledMap;
import fr.thesmyler.terramap.maps.raster.TiledMapProvider;
import fr.thesmyler.terramap.maps.raster.imp.UrlTiledMap;
import fr.thesmyler.terramap.util.math.Vec2dImmutable;
import net.buildtheearth.terraplusplus.projection.GeographicProjection;
import net.buildtheearth.terraplusplus.projection.OutOfProjectionBoundsException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.profiler.Profiler.Result;
import net.minecraft.util.ITabCompleter;

import static fr.thesmyler.smylibgui.SmyLibGui.getGameContext;

public class TerramapScreen extends Screen implements ITabCompleter {

    private final GuiScreen parent;

    private final ChatWidget chat = new ChatWidget(1000);

    // Main map area widgets
    private final MapWidget map;
    private final TexturedButtonWidget closeButton = new TexturedButtonWidget(50, IncludedTexturedButtons.CROSS);
    private final TexturedButtonWidget zoomInButton = new TexturedButtonWidget(50, IncludedTexturedButtons.PLUS);
    private final TexturedButtonWidget zoomOutButton = new TexturedButtonWidget(50, IncludedTexturedButtons.MINUS);
    private final TexturedButtonWidget centerButton = new TexturedButtonWidget(50, IncludedTexturedButtons.CENTER);
    private final TexturedButtonWidget styleButton = new TexturedButtonWidget(50, IncludedTexturedButtons.PAPER);
    private final CircularCompassWidget compass = new CircularCompassWidget(100, 100, 50, 100);
    private final WarningWidget offsetWarning = new WarningWidget(0, 0, 50);

    // Info panel widgets
    private TextWidget zoomText;
    private final SlidingPanelWidget infoPanel = new SlidingPanelWidget(70, 200);
    private TexturedButtonWidget panelButton = new TexturedButtonWidget(230, 5, 10, IncludedTexturedButtons.RIGHT, this::toggleInfoPanel);
    private TextWidget distortionText;
    private TextWidget debugText;
    private TextWidget playerGeoLocationText;
    private final TextFieldWidget searchBox = new TextFieldWidget(10);

    // Style panel
    private final SlidingPanelWidget stylePanel = new SlidingPanelWidget(80, 200);
    private final BackgroundStylePanelListContainer backgroundStylePanelListContainer;
    private final ScrollbarWidget styleScrollbar = new ScrollbarWidget(100, ScrollbarOrientation.VERTICAL);
    
    // Overlay panel
    private final SlidingPanelWidget overlayPanel = new SlidingPanelWidget(70, 200);
    private final OverlayList overlayList;
    private final ScrollableWidgetContainer overlayListContainer;
    
    // UI states
    private boolean f1Mode = false;
    private boolean debugMode = false;

    private final Map<String, IRasterTiledMap> backgrounds;

    public TerramapScreen(GuiScreen parent, Map<String, IRasterTiledMap> maps, TerramapScreenSavedState state) {
        super(BackgroundOption.OVERLAY);
        this.parent = parent;
        this.backgrounds = maps;
        Collection<IRasterTiledMap> tiledMaps = this.backgrounds.values();
        IRasterTiledMap bg = tiledMaps.toArray(new IRasterTiledMap[0])[0];
        this.map = new MapWidget(10, this.backgrounds.getOrDefault("osm", bg), MapContext.FULLSCREEN, TerramapConfig.CLIENT.getEffectiveTileScaling());
        this.backgroundStylePanelListContainer = new BackgroundStylePanelListContainer();
        if(state != null) this.resumeFromSavedState(TerramapClientContext.getContext().getSavedScreenState());
        this.infoPanel.setContourColor(Color.DARKER_GRAY.withAlpha(.5f));
        this.overlayPanel.setContourColor(Color.DARKER_GRAY.withAlpha(.5f));
        this.overlayList = new OverlayList(0, 0, 0, 100, this.map);
        this.overlayListContainer = new ScrollableWidgetContainer(0, 0, 10, 10, 10, this.overlayList);
        TerramapClientContext.getContext().registerForUpdates(true);
    }

    @Override
    public void initGui() {
        WidgetContainer content = this.getContent();
        content.removeAllWidgets();
        this.map.setPosition(0, 0);
        this.map.setSize(this.width, this.height);
        this.map.setTileScaling(TerramapConfig.CLIENT.getEffectiveTileScaling());

        content.addWidget(this.map);

        // Map control buttons
        this.closeButton.setX(this.width - this.closeButton.getWidth() - 5).setY(5);
        this.closeButton.setOnClick(() -> Minecraft.getMinecraft().displayGuiScreen(this.parent));
        this.closeButton.setTooltip(SmyLibGui.getTranslator().format("terramap.terramapscreen.buttons.close.tooltip"));
        this.closeButton.enable();
        content.addWidget(this.closeButton);
        this.compass.setOnClick(() -> this.map.getController().setRotation(0f, true));
        this.compass.setFadeAwayOnZero(true);
        this.compass.setTooltip(SmyLibGui.getTranslator().format("terramap.terramapscreen.buttons.compass.tooltip"));
        this.compass.setX(this.closeButton.getX());
        this.compass.setY(this.closeButton.getY() + this.closeButton.getHeight() + 5);
        this.compass.setSize(this.closeButton.getWidth());
        content.addWidget(this.compass);
        this.zoomInButton.setX(this.compass.getX()).setY(this.compass.getY() + this.compass.getHeight() + 5);
        this.zoomInButton.setOnClick(() -> {
            this.map.getController().setZoomStaticLocation(this.map.getController().getCenterLocation());
            this.map.getController().zoom(1, true);
        });
        this.zoomInButton.setTooltip(SmyLibGui.getTranslator().format("terramap.terramapscreen.buttons.zoomin.tooltip"));
        this.zoomInButton.enable();
        content.addWidget(this.zoomInButton);
        this.zoomText = new TextWidget(49, SmyLibGui.getDefaultFont());
        this.zoomText.setAnchorX(this.zoomInButton.getX() + this.zoomInButton.getWidth() / 2 + 1).setAnchorY(this.zoomInButton.getY() +  this.zoomInButton.getHeight() + 2);
        this.zoomText.setAlignment(TextAlignment.CENTER).setBackgroundColor(Color.DARKER_OVERLAY).setPadding(3);
        this.zoomText.setVisibility(!this.f1Mode);
        content.addWidget(this.zoomText);
        this.zoomOutButton.setX(this.zoomInButton.getX()).setY(this.zoomText.getY() + zoomText.getHeight() + 2);
        this.zoomOutButton.setOnClick(() -> {
            this.map.getController().setZoomStaticLocation(this.map.getController().getCenterLocation());
            this.map.getController().zoom(-1, true);
        });
        this.zoomOutButton.setTooltip(SmyLibGui.getTranslator().format("terramap.terramapscreen.buttons.zoomout.tooltip"));
        this.zoomOutButton.enable();
        content.addWidget(this.zoomOutButton);
        this.centerButton.setX(this.zoomOutButton.getX()).setY(this.zoomOutButton.getY() + this.zoomOutButton.getHeight() + 15);
        this.centerButton.setOnClick(() -> this.map.getController().track(this.map.getMainPlayerMarker()));
        this.centerButton.enable();
        this.centerButton.setTooltip(SmyLibGui.getTranslator().format("terramap.terramapscreen.buttons.track.tooltip"));
        content.addWidget(this.centerButton);
        this.styleButton.setX(this.centerButton.getX()).setY(this.centerButton.getY() + this.centerButton.getHeight() + 5);
        this.styleButton.setOnClick(this.stylePanel::open);
        this.styleButton.setTooltip(SmyLibGui.getTranslator().format("terramap.terramapscreen.buttons.style.tooltip"));
        this.styleButton.enable();
        content.addWidget(this.styleButton);
        this.offsetWarning.setTooltip(SmyLibGui.getTranslator().format("terramap.terramapscreen.warning.offset.tooltip"));
        content.addWidget(this.offsetWarning.setPosition(this.styleButton.getX(), this.styleButton.getY() + this.styleButton.getHeight() + 5));
        this.debugText = new TextWidget(49, Util.getSmallestFont());
        this.debugText.setAnchorX(3).setAnchorY(0);
        this.debugText.setAlignment(TextAlignment.RIGHT).setBackgroundColor(Color.DARKER_OVERLAY).setPadding(3);
        this.debugText.setVisibility(this.debugMode);
        content.addWidget(this.debugText);

        // Info panel
        Font infoFont = content.getFont();
        this.infoPanel.removeAllWidgets();
        this.infoPanel.setSize(250, this.height);
        this.infoPanel.setOpenX(0).setOpenY(0).setClosedX(-this.infoPanel.getWidth() + 25).setClosedY(0);
        this.panelButton.setTooltip(SmyLibGui.getTranslator().format("terramap.terramapscreen.buttons.info.tooltip"));
        this.infoPanel.addWidget(this.panelButton);
        TexturedButtonWidget openConfigButton = new TexturedButtonWidget(this.panelButton.getX(), this.panelButton.getY() + this.panelButton.getHeight() + 3, 100, IncludedTexturedButtons.WRENCH, this::openConfig);
        openConfigButton.setTooltip(SmyLibGui.getTranslator().format("terramap.terramapscreen.buttons.config.tooltip"));
        this.infoPanel.addWidget(openConfigButton);
        TexturedButtonWidget openOverlayButton = new TexturedButtonWidget(
                openConfigButton.getX(), openConfigButton.getY() + openConfigButton.getHeight() + 3, 100,
                IncludedTexturedButtons.PAPER, () -> {
                    if(this.overlayPanel.getTarget() == PanelTarget.CLOSED) {
                        this.overlayPanel.open();
                        if(this.infoPanel.getTarget() == PanelTarget.CLOSED) this.toggleInfoPanel();
                    }
                });
        this.infoPanel.addWidget(openOverlayButton);
        this.playerGeoLocationText = new TextWidget(49, infoFont);
        this.playerGeoLocationText = new TextWidget(49, infoFont);
        this.playerGeoLocationText.setAnchorX(5).setAnchorY(5).setAlignment(TextAlignment.RIGHT);
        this.infoPanel.addWidget(this.playerGeoLocationText);
        this.distortionText = new TextWidget(49, infoFont);
        this.distortionText.setAnchorX(5).setAnchorY(this.playerGeoLocationText.getAnchorY() + infoFont.height() + 5).setAlignment(TextAlignment.RIGHT);
        this.infoPanel.addWidget(this.distortionText);
        float y = this.distortionText.getY() + this.distortionText.getHeight() + 3;
        float lineHeight = 0;
        float x = 5;
        for(FeatureVisibilityController provider: this.getButtonProviders()) {
            if(!provider.showButton()) continue;
            AbstractButtonWidget button = provider.getButton();
            if(button == null) continue;
            if(x + button.getWidth() > this.infoPanel.getWidth() - 20) {
                x = 5;
                y += lineHeight + 3;
                lineHeight = 0;
            }
            lineHeight = Math.max(lineHeight, button.getHeight());
            button.setX(x);
            x += button.getWidth() + 3;
            button.setY(y);
            this.infoPanel.addWidget(button);
        }
        this.searchBox.setX(5).setY(y + lineHeight + 4).setWidth(186);
        this.searchBox.enableRightClickMenu();
        this.searchBox.setText(SmyLibGui.getTranslator().format("terramap.terramapscreen.search.wip")).disable();
        this.searchBox.setOnPressEnterCallback(this::search);
        this.infoPanel.addWidget(this.searchBox);
        TexturedButtonWidget searchButton = new TexturedButtonWidget(50, IncludedTexturedButtons.SEARCH);
        searchButton.setX(this.searchBox.getX() + this.searchBox.getWidth() + 2).setY(this.searchBox.getY() - 1);
        searchButton.setOnClick(() -> this.search(this.searchBox.getText()));
        //searchButton.enable();
        this.infoPanel.addWidget(searchButton);
        this.infoPanel.setHeight(this.searchBox.getY() + this.searchBox.getHeight() + 5);
        content.addWidget(this.infoPanel);
        
        // Overlay panel
        this.overlayPanel.removeAllWidgets();
        this.overlayPanel.cancelAllScheduled();
        this.overlayPanel.setSize(
                this.infoPanel.getWidth(),
                this.height - this.infoPanel.getHeight());
        this.overlayPanel.setClosedX(-this.overlayPanel.getWidth()).setClosedY(this.infoPanel.getHeight());
        this.overlayPanel.setOpenX(0).setOpenY(this.overlayPanel.getClosedY());
        this.overlayPanel.addWidget(new TextWidget(
                this.overlayPanel.getWidth() / 2, 7, 1,
                new TextComponentTranslation("terramap.terramapscreen.layerscreen.title"), TextAlignment.CENTER,
                content.getFont()));
        this.overlayPanel.addWidget(new TexturedButtonWidget(
                this.overlayPanel.getWidth() - 20, 5, 1,
                IncludedTexturedButtons.CROSS, this.overlayPanel::close));
        this.overlayListContainer.setPosition(5f, 25f);
        this.overlayListContainer.setSize(this.overlayPanel.getWidth() - 10, this.overlayPanel.getHeight() - 55f);
        this.overlayListContainer.setContourColor(Color.DARK_OVERLAY).setContourSize(1f);
        this.overlayList.setWidth(this.overlayListContainer.getWidth() - 15);
        this.overlayListContainer.setDoScissor(true);
        this.overlayList.setPosition(0, 0);
        this.overlayPanel.addWidget(this.overlayListContainer);
        int buttonWidth = (int) ((this.overlayListContainer.getWidth() - 10) / 3);
        this.overlayPanel.addWidget(
                new TextButtonWidget(
                        5f, this.overlayPanel.getHeight() - 25, 1,
                        buttonWidth,
                        SmyLibGui.getTranslator().format("terramap.terramapscreen.layerscreen.new"),
                        this::openInitialNewLayerSelector));
        this.overlayPanel.addWidget(
                new TextButtonWidget(
                        10f + buttonWidth, this.overlayPanel.getHeight() - 25f, 1,
                        buttonWidth,
                        SmyLibGui.getTranslator().format("terramap.terramapscreen.layerscreen.export")));
        this.overlayPanel.addWidget(
                new TextButtonWidget(
                        15f + buttonWidth*2, this.overlayPanel.getHeight() - 25f, 1,
                        buttonWidth,
                        SmyLibGui.getTranslator().format("terramap.terramapscreen.layerscreen.import")));
        content.addWidget(this.overlayPanel);

        // Style panel
        this.stylePanel.setSize(200, this.height);
        this.stylePanel.setClosedX(this.width + 1).setClosedY(0).setOpenX(this.width - this.stylePanel.getWidth()).setOpenY(0);
        this.stylePanel.setCloseOnClickOther(false);
        this.stylePanel.removeAllWidgets();
        this.styleScrollbar.setPosition(this.stylePanel.getWidth() - 15f, 0);
        this.styleScrollbar.setLength(this.height);
        this.stylePanel.addWidget(this.styleScrollbar);
        this.styleScrollbar.setViewPort(this.height / (this.backgroundStylePanelListContainer.getHeight() - 10f));
        if(this.styleScrollbar.getViewPort() >= 1) this.styleScrollbar.setProgress(0);
        this.stylePanel.addWidget(this.backgroundStylePanelListContainer);
        content.addWidget(this.stylePanel);

        if(TerramapConfig.CLIENT.chatOnMap) content.addWidget(this.chat);

        if(!TerramapClientContext.getContext().isInstalledOnServer() && TerramapClientContext.getContext().getProjection() == null && TerramapClientContext.getContext().isOnEarthWorld()) {
            StringBuilder warningBuilder = new StringBuilder();
            for(int i=1; SmyLibGui.getTranslator().hasKey("terramap.terramapscreen.projection_warning.line" + i); i++) {
                if(warningBuilder.length() > 0) warningBuilder.append('\n');
                warningBuilder.append(SmyLibGui.getTranslator().format("terramap.terramapscreen.projection_warning.line" + i));
            }
            ITextComponent c = new TextComponentString(warningBuilder.toString());
            Style style = new Style();
            style.setColor(TextFormatting.YELLOW);
            c.setStyle(style);
            TextWidget warningWidget = new TextWidget(150, 0, 1000, 300, c, TextAlignment.CENTER, Color.WHITE, true, SmyLibGui.getDefaultFont());
            warningWidget.setBackgroundColor(Color.DARKER_OVERLAY).setPadding(5).setAnchorY(this.height - warningWidget.getHeight());
            content.addWidget(warningWidget);
        }

        TerramapClientContext.getContext().setupMaps();
    }

    @Override
    public void onUpdate() {

        GeographicProjection projection = TerramapClientContext.getContext().getProjection();

        MapController controller = this.map.getController();
        this.zoomInButton.setEnabled(controller.getZoom() < controller.getMaxZoom());
        this.zoomOutButton.setEnabled(controller.getZoom() > controller.getMinZoom());
        this.zoomText.setText(new TextComponentString(GeoServices.formatZoomLevelForDisplay(controller.getZoom())));
        this.centerButton.setEnabled(!(controller.getTrackedMarker() instanceof MainPlayerMarker));

        this.compass.setAzimuth(controller.getRotation());

        GeoPointReadOnly mouseLocation = this.map.getMouseLocation();
        String formatScale = "-"; 
        String formatOrientation = "-";
        if(!WebMercatorUtil.PROJECTION_BOUNDS.contains(mouseLocation)) {
            this.distortionText.setText(new TextComponentTranslation("terramap.terramapscreen.information.distortion", "-", "-"));
        } else {
            if(projection != null) {
                try {
                    double[] dist = projection.tissot(mouseLocation.longitude(), mouseLocation.latitude());
                    formatScale = "" + GeoServices.formatGeoCoordForDisplay(Math.sqrt(Math.abs(dist[0])));
                    formatOrientation = "" + GeoServices.formatGeoCoordForDisplay(Math.toDegrees(dist[1]));
                } catch (OutOfProjectionBoundsException ignored) {
                }
                this.distortionText.setText(new TextComponentTranslation("terramap.terramapscreen.information.distortion", formatScale, formatOrientation));
            } else {
                this.distortionText.setText(new TextComponentTranslation("terramap.terramapscreen.information.distortion", "-", "-"));
            }
        }

        if(controller.isTracking()) {
            Marker marker = controller.getTrackedMarker();
            GeoPoint<?> markerLocation = marker.getLocation();
            String markerName = marker.getDisplayName().getFormattedText();
            if(markerLocation == null) {
                this.playerGeoLocationText.setText(new TextComponentTranslation("terramap.terramapscreen.information.trackedoutsidemap", markerName));
            } else {
                String trackFormatLon = GeoServices.formatGeoCoordForDisplay(markerLocation.longitude());
                String trackFormatLat = GeoServices.formatGeoCoordForDisplay(markerLocation.latitude());
                this.playerGeoLocationText.setText(new TextComponentTranslation("terramap.terramapscreen.information.tracked", markerName, trackFormatLat, trackFormatLon));
            }
        } else if(this.map.getMainPlayerMarker() != null){
            Marker marker = this.map.getMainPlayerMarker();
            GeoPoint<?> markerLocation = marker.getLocation();
            if(markerLocation == null) {
                this.playerGeoLocationText.setText(new TextComponentTranslation("terramap.terramapscreen.information.playerout"));
            } else {
                String formatedLon = GeoServices.formatGeoCoordForDisplay(markerLocation.longitude());
                String formatedLat = GeoServices.formatGeoCoordForDisplay(markerLocation.latitude());
                this.playerGeoLocationText.setText(new TextComponentTranslation("terramap.terramapscreen.information.playergeo", formatedLat, formatedLon));
            }
        } else {
            this.playerGeoLocationText.setText(new TextComponentTranslation("terramap.terramapscreen.information.noplayer"));
        }

        this.offsetWarning.setVisibility(this.map.doesBackgroundHaveRenderingOffset());

        if(this.debugMode) {
            StringBuilder debugBuilder = new StringBuilder();
            Locale locale = Locale.US;
            TerramapClientContext srv = TerramapClientContext.getContext();
            EarthGeneratorSettings generationSettings = srv.getGeneratorSettings();
            debugBuilder.append(String.format(locale, "FPS: %s", Minecraft.getDebugFPS()));
            debugBuilder.append(String.format(locale, "\nClient: %s", TerramapMod.getVersion()));
            debugBuilder.append(String.format(locale, "\nServer: %s", srv.getServerVersion()));
            debugBuilder.append(String.format(locale, "\nSledgehammer: %s", srv.getSledgehammerVersion()));
            debugBuilder.append(String.format(locale, "\nProjection: %s", generationSettings != null ? generationSettings.projection() : null));
            debugBuilder.append(String.format(locale, "\nMap id: %s", this.map.getBackgroundStyle().getId()));
            debugBuilder.append(String.format(locale, "\nMap provider: %sv%s",this.map.getBackgroundStyle().getProvider(), this.map.getBackgroundStyle().getProviderVersion()));
            if(this.map.getBackgroundStyle() instanceof CachingRasterTiledMap) {
                CachingRasterTiledMap<?> cachingMap = (CachingRasterTiledMap<?>) this.map.getBackgroundStyle();
                debugBuilder.append(String.format(locale, "\nLoaded tiles: %d/%d/%d", cachingMap.getBaseLoad(), cachingMap.getLoadedCount(), cachingMap.getMaxLoad()));
                if(cachingMap instanceof UrlTiledMap) {
                    UrlTiledMap urlMap = (UrlTiledMap) cachingMap;
                    String[] urls = urlMap.getUrlPatterns();
                    int showingIndex = (int) ((System.currentTimeMillis() / 3000) % urls.length);
                    debugBuilder.append(String.format(locale, "\nMap urls (%d) %s", urls.length, urls[showingIndex]));
                }
            }
            debugBuilder.append(String.format(locale, "\nScaling: %.2f/%s", this.map.getTileScaling(), getGameContext().getScaleFactor()));
            debugBuilder.append("\n\n");
            debugBuilder.append("Locations: ")
                    .append(TextFormatting.RED).append("center ")
                    .append(TextFormatting.BLUE).append("center target ")
                    .append(TextFormatting.GREEN).append("zoom target ")
                    .append(TextFormatting.GOLD).append("rotation target ")
                    .append(TextFormatting.RESET);
            debugBuilder.append('\n');
            this.buildProfilingResult(debugBuilder, "", "");
            this.debugText.setText(new TextComponentString(debugBuilder.toString()));
            this.debugText.setAnchorY(this.height - this.debugText.getHeight());
        }
    }

    private void buildProfilingResult(StringBuilder builder, String sectionName, String padding) {
        List<Result> results = this.map.getProfiler().getProfilingData(sectionName);
        for(Result result: results) {
            String name = result.profilerName;
            if("".equals(name) || name.equals(sectionName) || (sectionName + ".").equals(name)) continue;
            long use = Math.round(result.usePercentage);
            if("unspecified".equals(name) && use >= 100) continue;
            builder.append('\n').append(padding).append(String.format(Locale.US, "%1$s: %2$d%%", name, use));
            this.buildProfilingResult(builder, name, padding + "  ");
        }
    }

    private void toggleInfoPanel() {
        float x = this.panelButton.getX();
        float y = this.panelButton.getY();
        int z = this.panelButton.getZ();
        TexturedButtonWidget newButton;
        if(this.infoPanel.getTarget().equals(PanelTarget.OPENED)) {
            this.infoPanel.close();
            if(this.overlayPanel.getTarget() == PanelTarget.OPENED) this.overlayPanel.close();
            newButton = new TexturedButtonWidget(x, y, z, IncludedTexturedButtons.RIGHT, this::toggleInfoPanel);
        } else {
            this.infoPanel.open();
            newButton = new TexturedButtonWidget(x, y, z, IncludedTexturedButtons.LEFT, this::toggleInfoPanel);
        }
        newButton.setTooltip(this.panelButton.getTooltipText());
        this.infoPanel.removeWidget(this.panelButton);
        this.panelButton = newButton;
        this.infoPanel.addWidget(this.panelButton);
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) {
        if(this.getContent().getFocusedWidget() == null || (!this.getContent().getFocusedWidget().equals(this.searchBox) && !this.chat.isOpen())) {
            MapController controller = this.map.getController();
            if(keyCode == KeyBindings.TOGGLE_DEBUG.getKeyCode()) this.setDebugMode(!this.debugMode);
            if(keyCode == Keyboard.KEY_F1) this.setF1Mode(!this.f1Mode);
            if(keyCode == Minecraft.getMinecraft().gameSettings.keyBindForward.getKeyCode() || keyCode == Keyboard.KEY_UP) controller.moveMap(0, 30, true);
            if(keyCode == Minecraft.getMinecraft().gameSettings.keyBindBack.getKeyCode() || keyCode == Keyboard.KEY_DOWN) controller.moveMap(0, -30, true);
            if(keyCode == Minecraft.getMinecraft().gameSettings.keyBindRight.getKeyCode() || keyCode == Keyboard.KEY_RIGHT) controller.moveMap(-30, 0, true);
            if(keyCode == Minecraft.getMinecraft().gameSettings.keyBindLeft.getKeyCode() || keyCode == Keyboard.KEY_LEFT) controller.moveMap(30, 0, true);
            if(keyCode == KeyBindings.ZOOM_IN.getKeyCode()) this.zoomInButton.getOnClick().run();
            if(keyCode == KeyBindings.ZOOM_OUT.getKeyCode()) this.zoomOutButton.getOnClick().run();
            if(keyCode == KeyBindings.OPEN_MAP.getKeyCode() || keyCode == Keyboard.KEY_ESCAPE) Minecraft.getMinecraft().displayGuiScreen(this.parent);
            if(keyCode == Minecraft.getMinecraft().gameSettings.keyBindChat.getKeyCode()) {
                this.map.stopPassiveInputs();
                this.chat.setOpen(!this.chat.isOpen());
            }
        } else {
            super.keyTyped(typedChar, keyCode);
        }
    }

    public TerramapScreenSavedState saveToState() {

        MapController controller = this.map.getController();
        String tracking = null;
        Marker trackingMarker = controller.getTrackedMarker();
        if(trackingMarker != null) {
            tracking = trackingMarker.getIdentifier();
        }
        Map<String, Boolean> visibility = new HashMap<>();
        Map<String, FeatureVisibilityController> visibilityControllers = this.map.getVisibilityControllers();
        for(String key: visibilityControllers.keySet()) {
            visibility.put(key, visibilityControllers.get(key).getVisibility());
        }
        
        List<MapWidget> maps = new ArrayList<>(this.backgroundStylePanelListContainer.maps);
        maps.add(this.map); // Add it last to make sure it prevails in the final Map<String, Vec2d>
        Map<String, Vec2dImmutable> offsets = new HashMap<>();
        for(MapWidget map: maps) {
            for(MapLayer layer: map.getOverlayLayers()) {
                offsets.put(layer.getId(), layer.getRenderingOffset().getImmutable());
            }
            RasterMapLayer background = map.getBackgroundLayer();
            offsets.put(background.getId(), background.getRenderingOffset().getImmutable());
        }        

        return new TerramapScreenSavedState(
                controller.getZoom(),
                controller.getCenterLocation().getImmutable(),
                controller.getRotation(),
                this.map.getBackgroundStyle().getId(),
                this.infoPanel.getTarget().equals(PanelTarget.OPENED),
                TerramapConfig.CLIENT.saveUiState && this.debugMode,
                TerramapConfig.CLIENT.saveUiState && this.f1Mode,
                visibility,
                tracking,
                offsets
        );
    }

    public void resumeFromSavedState(TerramapScreenSavedState state) {
        MapController controller = this.map.getController();
        this.map.setBackground(this.backgrounds.getOrDefault(state.mapStyle, this.map.getBackgroundStyle()));
        controller.setZoom(state.zoomLevel, false);
        GeoPointImmutable centerLocation = new GeoPointImmutable(state.centerLongitude, state.centerLatitude);
        controller.moveLocationToCenter(centerLocation, false);
        controller.setRotation(state.rotation, false);
        this.map.restoreTracking(state.trackedMarker);
        this.infoPanel.setStateNoAnimation(state.infoPannel);
        TexturedButtonWidget newButton;
        if(this.infoPanel.getTarget().equals(PanelTarget.OPENED)) {
            float x = this.panelButton.getX();
            float y = this.panelButton.getY();
            int z = this.panelButton.getZ();
            newButton = new TexturedButtonWidget(x, y, z, IncludedTexturedButtons.LEFT, this::toggleInfoPanel);
            newButton.setTooltip(this.panelButton.getTooltipText());
            this.infoPanel.removeWidget(this.panelButton);
            this.panelButton = newButton;
        }
        this.infoPanel.addWidget(this.panelButton);
        this.setF1Mode(state.f1);
        this.setDebugMode(state.debug);
        for(FeatureVisibilityController c: this.map.getVisibilityControllers().values()) {
            if(state.visibilitySettings.containsKey(c.getSaveName())) c.setVisibility(state.visibilitySettings.get(c.getSaveName()));
        }
        if(state.layerOffsets != null) {
            List<MapLayer> layers = new ArrayList<>();
            for(MapWidget map: this.backgroundStylePanelListContainer.maps) layers.add(map.getBackgroundLayer());
            Collections.addAll(layers, this.map.getOverlayLayers());
            layers.add(this.map.getBackgroundLayer());
            for(MapLayer layer: layers) for(String id: state.layerOffsets.keySet()) if(id.equals(layer.getId())){
                layer.setRenderingOffset(state.layerOffsets.get(id));
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        /*
         * We cannot "pause the game" in single player.
         * It stops the integrated server from processing client packets, including the player movements packets.
         * But sending a tpll/tp command to a paused server actually processes the command and updates the player position.
         * That means that once the server resumes, it will try to process player movement packets from before the player was teleported,
         * and will check a MASSIVE area for collision.
         * 
         * There may be a potential DOS here?
         */
        return false;
    }

    private boolean search(String text) {
        TerramapMod.logger.info("Geo search: " + text);
        //TODO Search
        return true; // Let the search box loose focus
    }
    
    private void addMapLayer(MapLayer mapLayer) {
        //TODO Have multiple categories
        int z = Integer.MIN_VALUE + 1;
        MapLayer[] layers = this.map.getOverlayLayers();
        for(MapLayer layer: layers) {
            z = Math.max(z, layer.getZ());
        }
        mapLayer.setZ(Math.min(-1, z + 1));
        mapLayer.setUserOverlay(true);
        this.map.addOverlayLayer(mapLayer);
        this.overlayList.init();
    }
    
    private void openInitialNewLayerSelector() {
        final MapController thisController = this.map.getController();
        Map<String, Runnable> options = new HashMap<>();
        options.put("Tiled raster map ", () -> {
            FlexibleWidgetContainer container = new FlexibleWidgetContainer(0f, 0f, 0, 285f, 10f);
            container.setDoScissor(false);
            ArrayList<IRasterTiledMap> maps = new ArrayList<>(TerramapScreen.this.backgrounds.values());
            maps.sort((m1, m2) -> Integer.compare(m2.getDisplayPriority(), m1.getDisplayPriority()));
            float y = 5f;
            float x = 20f;
            PopupScreen pop = new PopupScreen(300f, 200f);
            for(IRasterTiledMap m: maps) {
                MapPreview map = new MapPreview(0, m, e -> {
                    this.addMapLayer(new RasterMapLayer(this.map, m));
                    pop.close();
                });
                MapController controller = map.getController();
                map.setPosition(x, y);
                map.setSize(125f, 75f);
                controller.moveLocationToCenter(thisController.getCenterLocation(), false);
                controller.setZoom(thisController.getZoom(), false);
                if(x == 20f) {
                    x = 155f;
                } else {
                    x = 20f;
                    y += map.getHeight() + 5f;
                }
                container.addWidget(map);
            }
            container.setHeight(y);
            ScrollableWidgetContainer scrollContainer = new ScrollableWidgetContainer(0f, 0f, 0, 300f, 200f, container);
            pop.getContent().addWidget(scrollContainer);
            pop.show();
        });
        this.getContent().scheduleBeforeNextUpdate(() -> 
            new MultiChoicePopupScreen("Choose a type for the new  layer", options).show()
        );
    }

    private class BackgroundStylePanelListContainer extends FlexibleWidgetContainer {

        final float mapWidth = 175;
        final float mapHeight = 100;
        
        final List<MapWidget> maps = new ArrayList<>();

        BackgroundStylePanelListContainer() {
            super(0, 0, 0, 0, 0);
            this.setDoScissor(false);
            IWidget lw = null;
            for(TiledMapProvider provider: TiledMapProvider.values()) {
                Throwable e = provider.getLastError();
                if(e == null) continue;
                float x = 0;
                float y = 0;
                if(lw != null) {
                    y = lw.getY() + lw.getHeight() + 5;
                }
                FailedMapLoadingNotice w = new FailedMapLoadingNotice(x, y, 50, mapWidth, mapHeight, provider, e);
                this.addWidget(w);
                lw = w;
            }
            ArrayList<IRasterTiledMap> maps = new ArrayList<>(TerramapScreen.this.backgrounds.values());
            maps.sort((m1, m2) -> Integer.compare(m2.getDisplayPriority(), m1.getDisplayPriority()));
            for(IRasterTiledMap map: maps) {
                MapWidget w = new MapPreview(50, map, m -> {
                    TerramapScreen.this.map.setBackground(m.getBackgroundStyle());
                    TerramapScreen.this.map.getBackgroundLayer().setRenderingOffset(m.getBackgroundLayer().getRenderingOffset());
                    TerramapScreen.this.stylePanel.close();
                    TerramapScreen.this.overlayList.init();
                });
                w.setWidth(mapWidth);
                w.setHeight(mapHeight);
                if(lw == null) {
                    w.setPosition(0, 0);
                } else {
                    w.setPosition(0, lw.getY() + lw.getHeight() + 5);
                }
                this.addWidget(w);
                lw = w;
                this.maps.add(w);
            }
            this.setSize(this.mapWidth, lw.getY() + lw.getHeight() + 10f);
        }

        @Override
        public void onUpdate(float mouseX, float mouseY, WidgetContainer parent) {
            MapLayer bg = TerramapScreen.this.map.getBackgroundLayer();
            final MapController thisController = TerramapScreen.this.map.getController();
            for(MapWidget map: this.maps) {
                MapController controller = map.getController();
                controller.setZoom(thisController.getZoom(), false);
                controller.moveLocationToCenter(thisController.getCenterLocation(), false);
                map.setTileScaling(TerramapScreen.this.map.getTileScaling());
                if(map.getBackgroundLayer().getId().equals(bg.getId())) {
                    map.getBackgroundLayer().setRenderingOffset(bg.getRenderingOffset());
                }
            }
            super.onUpdate(mouseX, mouseY, parent);
        }

        @Override
        public boolean onMouseWheeled(float mouseX, float mouseY, int amount, WidgetContainer parent) {
            if(TerramapScreen.this.styleScrollbar.getViewPort() < 1) {
                if(amount > 0) TerramapScreen.this.styleScrollbar.scrollBackward();
                else TerramapScreen.this.styleScrollbar.scrollForward();
            }
            return super.onMouseWheeled(mouseX, mouseY, amount, parent);
        }

        @Override
        public float getX() {
            return 5;
        }

        @Override
        public float getY() {
            return 5 - (this.getHeight() - TerramapScreen.this.height) * TerramapScreen.this.styleScrollbar.getProgress();
        }

    }

    private static class FailedMapLoadingNotice extends AbstractSolidWidget {

        private final TiledMapProvider provider;
        private final Throwable exception;

        public FailedMapLoadingNotice(float x, float y, int z, float width, float height, TiledMapProvider provider, Throwable e) {
            super(x, y, z, width, height);
            this.provider = provider;
            this.exception = e;
        }

        @Override
        public void draw(float x, float y, float mouseX, float mouseY, boolean hovered, boolean focused, WidgetContainer parent) {
            boolean wasScissor = RenderUtil.isScissorEnabled();
            RenderUtil.setScissorState(true);
            RenderUtil.pushScissorPos();
            RenderUtil.scissor(x, y, this.width, this.height);
            RenderUtil.drawRect(x, y, x + this.width, y + this.height, Color.YELLOW);
            RenderUtil.drawRect(x + 4, y + 4, x + this.width - 4, y + this.height - 4, Color.DARK_GRAY);
            parent.getFont().drawCenteredString(x + this.width / 2, y + 8, SmyLibGui.getTranslator().format("terramap.terramapscreen.mapstylefailed.title"), Color.YELLOW, false);
            parent.getFont().drawString(x + 8, y + 16 + parent.getFont().height(), SmyLibGui.getTranslator().format("terramap.terramapscreen.mapstylefailed.provider", this.provider), Color.WHITE, false);
            parent.getFont().drawSplitString(x + 8, y + 24 + parent.getFont().height()*2, SmyLibGui.getTranslator().format("terramap.terramapscreen.mapstylefailed.exception", this.exception), this.width - 16, Color.WHITE, false);
            RenderUtil.popScissorPos();
            RenderUtil.setScissorState(wasScissor);
        }

    }

    @Override
    public void onGuiClosed() {
        TerramapClientContext.getContext().setSavedScreenState(this.saveToState()); //TODO Also save if minecraft is closed from the OS
        TerramapClientContext.getContext().saveSettings();
        TerramapClientContext.getContext().registerForUpdates(false);
    }

    public void setF1Mode(boolean yesNo) {
        this.f1Mode = yesNo;
        this.infoPanel.setVisibility(!yesNo);
        this.stylePanel.setVisibility(!yesNo);
        this.closeButton.setVisibility(!yesNo);
        this.zoomInButton.setVisibility(!yesNo);
        this.zoomOutButton.setVisibility(!yesNo);
        this.styleButton.setVisibility(!yesNo);
        this.centerButton.setVisibility(!yesNo);
        if(this.zoomText != null) this.zoomText.setVisibility(!yesNo);
        this.map.setScaleVisibility(!yesNo);
    }

    public void setDebugMode(boolean yesNo) {
        this.debugMode = yesNo;
        if(this.debugText != null) this.debugText.setVisibility(yesNo);
        this.map.setDebugMode(yesNo);
    }

    private void openConfig() {
        Minecraft.getMinecraft().displayGuiScreen(new TerramapConfigScreen(this));
    }

    private Collection<FeatureVisibilityController> getButtonProviders() {
        return Collections.unmodifiableCollection(this.map.getVisibilityControllers().values());
    }

    private class MapPreview extends MapWidget {
        
        final Consumer<MapPreview> onClick;

        public MapPreview(int z, IRasterTiledMap map, Consumer<MapPreview> onClick) {
            super(z, map, MapContext.PREVIEW, TerramapScreen.this.map.getTileScaling());
            this.setInteractive(false);
            this.setRightClickMenuEnabled(false);
            this.setCopyrightVisibility(false);
            this.setScaleVisibility(false);
            this.onClick = onClick;
        }

        @Override
        public void draw(float x, float y, float mouseX, float mouseY, boolean hovered, boolean focused, WidgetContainer parent) {
            super.draw(x, y, mouseX, mouseY, hovered, focused, parent);
            Color textColor = hovered? Color.SELECTION: Color.WHITE;
            String text = this.getBackgroundStyle().getLocalizedName(SmyLibGui.getGameContext().getLanguage());
            float width = this.getWidth();
            float height = this.getHeight();
            RenderUtil.drawRect(x, y, x + width, y + 4, Color.DARK_GRAY);
            RenderUtil.drawRect(x, y + height - parent.getFont().height() - 4, x + width, y + height, Color.DARK_GRAY);
            RenderUtil.drawRect(x, y, x + 4, y + height, Color.DARK_GRAY);
            RenderUtil.drawRect(x + width - 4, y, x + width, y + height, Color.DARK_GRAY);
            parent.getFont().drawCenteredString(x + width/2, y + height - parent.getFont().height() - 2, text, textColor, true);

        }

        @Override
        public boolean onClick(float mouseX, float mouseY, int mouseButton, @Nullable WidgetContainer parent) {
            if(mouseButton == 0) {
                this.onClick.accept(this);
            }
            return false;
        }

        @Override
        public String getTooltipText() {
            return this.getBackgroundLayer().getTiledMap().getId();
        }

        @Override
        public long getTooltipDelay() {
            return 0;
        }

    }

    @Override
    public void setCompletions(String... newCompletions) {
        this.chat.setCompletions(newCompletions);
    }

}
