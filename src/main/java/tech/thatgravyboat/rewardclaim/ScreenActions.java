package tech.thatgravyboat.rewardclaim;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;

public class ScreenActions extends Screen {

    private ScreenActions(Component title) {
        super(title);
    }

    public static void handleClick(Style style) {
        var mc = Minecraft.getInstance();
        var event = style.getClickEvent();
        if (event == null) return;
        defaultHandleClickEvent(event, mc, mc.gui.screen());
    }
}
