package dev.adryh01.yafabric.mods;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;

/** Right Shift hub: MODS or COSMETICS. */
public class YAHub extends Screen {

    private static final int PW = 230, PH = 112;

    private int px() {
        return (width - PW) / 2;
    }

    private int py() {
        return (height - PH) / 2;
    }

    private static boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean shouldPauseGame() {
        return false;
    }

    @Override
    public void render(int mx, int my, float delta) {
        TextRenderer fr = MinecraftClient.getInstance().textRenderer;
        DrawableHelper.fill(0, 0, width, height, 0x70000000);
        int x = px(), y = py();
        UI.roundBox(x, y, x + PW, y + PH, 6, UI.BORDER, UI.BG, 1);
        fr.draw("YA CLIENT", x + 14, y + 12, UI.DARK);
        button(fr, x + 12, y + 32, PW - 24, 30, "MODS", true, in(mx, my, x + 12, y + 32, PW - 24, 30));
        button(fr, x + 12, y + 68, PW - 24, 30, "COSMETICS", false, in(mx, my, x + 12, y + 68, PW - 24, 30));
    }

    private void button(TextRenderer fr, int x, int y, int w, int h, String t, boolean dark, boolean hv) {
        UI.roundBox(x, y, x + w, y + h, 4, dark ? UI.DARK : UI.BORDER, dark ? (hv ? 0xFF000000 : UI.DARK) : (hv ? UI.HEADER : UI.CARD), 1);
        fr.draw(t, x + (w - fr.getStringWidth(t)) / 2, y + (h - 8) / 2, dark ? 0xFFFFFFFF : UI.DARK);
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) {
        if (button != 0) return;
        int x = px(), y = py();
        if (in(mx, my, x + 12, y + 32, PW - 24, 30)) MinecraftClient.getInstance().setScreen(new ModMenu());
        else if (in(mx, my, x + 12, y + 68, PW - 24, 30)) MinecraftClient.getInstance().setScreen(new Cosmetics());
    }

    @Override
    protected void keyPressed(char c, int key) {
        if (key == 1 || key == 54) MinecraftClient.getInstance().setScreen(null);
    }
}
