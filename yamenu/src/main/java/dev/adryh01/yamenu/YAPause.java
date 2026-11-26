package dev.adryh01.yamenu;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiShareToLan;
import net.minecraft.client.gui.achievement.GuiAchievements;
import net.minecraft.client.gui.achievement.GuiStats;

import java.io.IOException;

/** Pause menu: a slim panel on the left, the game stays visible on the right. */
final class YAPause extends YAScreen {

    YAPause() {
        super(null, "");
    }

    @Override
    public void initGui() {
        buttonList.clear();
        int bw = 150, x = 25, y = 84, g = 27;
        buttonList.add(new YButton(1, x, y, bw, 22, "Back to game"));
        buttonList.add(new YButton(2, x, y + g, bw, 22, "Options"));
        buttonList.add(new YButton(3, x, y + g * 2, bw, 22, "Achievements"));
        buttonList.add(new YButton(4, x, y + g * 3, bw, 22, "Statistics"));
        int n = 4;
        if (mc.isSingleplayer() && !mc.getIntegratedServer().getPublic()) {
            buttonList.add(new YButton(5, x, y + g * 4, bw, 22, "Open to LAN"));
            n = 5;
        }
        buttonList.add(new YButton(6, x, y + g * n + 10, bw, 22, mc.isIntegratedServerRunning() ? "Save and quit" : "Disconnect"));
    }

    @Override
    protected void actionPerformed(GuiButton b) throws IOException {
        switch (b.id) {
            case 1:
                close();
                break;
            case 2:
                mc.displayGuiScreen(new GuiOptions(this, mc.gameSettings));
                break;
            case 3:
                mc.displayGuiScreen(new GuiAchievements(this, mc.thePlayer.getStatFileWriter()));
                break;
            case 4:
                mc.displayGuiScreen(new GuiStats(this, mc.thePlayer.getStatFileWriter()));
                break;
            case 5:
                mc.displayGuiScreen(new GuiShareToLan(this));
                break;
            case 6:
                boolean local = mc.isIntegratedServerRunning();
                b.enabled = false;
                mc.theWorld.sendQuittingDisconnectingPacket();
                mc.loadWorld(null);
                // leaving a server returns to the multiplayer list, leaving a world to the title screen
                if (local) mc.displayGuiScreen(new YAMainMenu());
                else mc.displayGuiScreen(new YAMultiplayer(new YAMainMenu()));
                break;
            default:
        }
    }

    private void close() {
        mc.displayGuiScreen(null);
        mc.setIngameFocus();
    }

    @Override
    protected void keyTyped(char c, int key) throws IOException {
        if (key == 1) close();
    }

    @Override
    public void drawScreen(int mx, int my, float pt) {
        Gui.drawRect(0, 0, width, height, 0x30000000);
        Gui.drawRect(0, 0, 200, height, 0xF2F3F3F3);
        Gui.drawRect(200, 0, 201, height, UI.BORDER);
        UI.logo(75, 18, 50);
        fontRendererObj.drawString("Game menu", 25, 70, UI.SOFT);
        super.drawScreen(mx, my, pt);
    }
}
