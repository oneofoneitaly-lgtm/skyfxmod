package dev.adryh01.yamenu;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;

abstract class YAScreen extends GuiScreen {

    protected final GuiScreen parent;
    private final String title;

    YAScreen(GuiScreen parent, String title) {
        this.parent = parent;
        this.title = title;
    }

    protected void background(boolean header) {
        Gui.drawRect(0, 0, width, height, UI.BG);
        if (header) {
            Gui.drawRect(0, 0, width, 34, UI.HEADER);
            Gui.drawRect(0, 34, width, 35, UI.BORDER);
            UI.logo(10, 4, 26);
            fontRendererObj.drawString(title, 44, 13, UI.DARK);
        }
    }

    @Override
    protected void keyTyped(char c, int key) throws java.io.IOException {
        if (key == 1) {
            mc.displayGuiScreen(parent);
            return;
        }
        super.keyTyped(c, key);
    }
}
