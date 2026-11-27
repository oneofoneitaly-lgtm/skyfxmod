package dev.adryh01.yafabric.mods;

import dev.adryh01.yafabric.CapeNet;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;

/** Your cosmetics, one card each with a preview. Only what you own is shown. */
public class Cosmetics extends Screen {

    private static final Identifier CAPE = new Identifier("yafabric", "textures/ya_cape.png");
    private static final int PW = 300, PH = 230;

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
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer fr = mc.textRenderer;
        DrawableHelper.fill(0, 0, width, height, 0x70000000);
        int x = px(), y = py();
        UI.roundBox(x, y, x + PW, y + PH, 6, UI.BORDER, UI.BG, 1);
        fr.draw("COSMETICS", x + 14, y + 12, UI.DARK);
        fr.draw("< Back", x + PW - 50, y + 12, in(mx, my, x + PW - 54, y + 8, 46, 16) ? UI.DARK : UI.MID);
        boolean owned = mc.player != null && CapeNet.hasCape(mc.player.getUuid());
        if (!owned) {
            fr.draw("You do not own any cosmetic yet.", x + 14, y + 40, UI.MID);
            fr.draw("Get the free YA Cape on the store:", x + 14, y + 56, UI.SOFT);
            fr.draw("yaprojects.it/store", x + 14, y + 68, UI.DARK);
            return;
        }
        int cx = x + 12, cy = y + 34, cw = PW - 24, ch = 84;
        UI.roundBox(cx, cy, cx + cw, cy + ch, 4, UI.BORDER, UI.CARD, 1);
        UI.roundRect(cx + 6, cy + 6, cx + 62, cy + ch - 6, 3, 0xFF2A2A2A);
        mc.getTextureManager().bindTexture(CAPE);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        DrawableHelper.drawTexture(cx + 14, cy + 11, 16.0F, 16.0F, 40, 62, 160, 256, 1024.0F, 512.0F);
        fr.draw("YA Cape", cx + 74, cy + 12, UI.DARK);
        fr.draw("Free for every player.", cx + 74, cy + 24, UI.SOFT);
        Module m = Mods.byId("clientcape");
        if (m != null) {
            Setting mine = m.setting("mine"), others = m.setting("others");
            row(fr, cx + 74, cy + 40, cw - 84, "Show my cape", mine.b, mx, my);
            row(fr, cx + 74, cy + 60, cw - 84, "Show others' cape", others.b, mx, my);
        }
    }

    private void row(TextRenderer fr, int x, int y, int w, String t, boolean on, int mx, int my) {
        fr.draw(t, x, y + 2, UI.MID);
        UI.sw(x + w - 24, y, on ? 1.0F : 0.0F);
    }

    @Override
    protected void mouseClicked(int mx, int my, int button) {
        if (button != 0) return;
        int x = px(), y = py();
        if (in(mx, my, x + PW - 54, y + 8, 46, 16)) {
            MinecraftClient.getInstance().setScreen(new YAHub());
            return;
        }
        Module m = Mods.byId("clientcape");
        if (m == null) return;
        int cx = x + 12, cy = y + 34, cw = PW - 24;
        if (in(mx, my, cx + 74, cy + 40, cw - 84, 14)) m.setting("mine").b = !m.setting("mine").b;
        if (in(mx, my, cx + 74, cy + 60, cw - 84, 14)) m.setting("others").b = !m.setting("others").b;
        Mods.save();
    }

    @Override
    protected void keyPressed(char c, int key) {
        if (key == 1 || key == 54) MinecraftClient.getInstance().setScreen(new YAHub());
    }
}
