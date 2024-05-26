package fr.thesmyler.smylibgui.event;

import net.smyler.smylib.gui.containers.WidgetContainer;
import net.minecraftforge.fml.common.eventhandler.Event;
import net.smyler.smylib.gui.screen.Screen;

/**
 * An event fired when the HUD {@link Screen} initializes.
 *
 * @author SmylerMC
 */
public class HudScreenInitEvent extends Event {

    private final WidgetContainer content;

    public HudScreenInitEvent(WidgetContainer screen) {
        this.content = screen;
    }

    public WidgetContainer getHudScreen() {
        return this.content;
    }

}
