package dev.adryh01.yamenu;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiLanguage;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.resources.I18n;

import java.io.IOException;

final class YAMainMenu extends YAScreen {

    YAMainMenu() {
        super(null, "");
    }

    @Override
    public void initGui() {
        buttonList.clear();
        int bw = 170, x = width / 2 - bw / 2, y = height / 2 + 8;
        buttonList.add(new YButton(1, x, y, bw, 22, I18n.format("menu.singleplayer")));
        buttonList.add(new YButton(2, x, y + 28, bw, 22, I18n.format("menu.multiplayer")));
        buttonList.add(new YButton(0, 10, height - 28, 64, 18, I18n.format("menu.options")));
        buttonList.add(new YButton(5, 80, height - 28, 64, 18, "Language"));
        buttonList.add(new YButton(4, width - 74, height - 28, 64, 18, I18n.format("menu.quit")));
    }

    @Override
    protected void actionPerformed(GuiButton b) throws IOException {
        switch (b.id) {
            case 1:
                mc.displayGuiScreen(new net.minecraft.client.gui.GuiSelectWorld(this));
                break;
            case 2:
                mc.displayGuiScreen(new net.minecraft.client.gui.GuiMultiplayer(this));
                break;
            case 0:
                mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings));
                break;
            case 5:
                mc.displayGuiScreen(new GuiLanguage(this, mc.gameSettings, mc.getLanguageManager()));
                break;
            case 4:
                mc.shutdown();
                break;
            default:
        }
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        // the title screen has no parent: Esc does nothing
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        background(false);
        UI.logo(width / 2 - 48, height / 2 - 112, 96);
        super.drawScreen(mx, my, pt);
    }
}
