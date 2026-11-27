package dev.adryh01.yafabric.mods;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import org.lwjgl.opengl.GL11;

import java.util.List;

/** Shared HUD drawing: positions a module, applies scale/background and paints its content. */
public final class Hud {

    public interface Painter {
        void paint(Module m, TextRenderer fr);
    }

    private Hud() {
    }

    private static int curX, curY, colW;
    public static boolean rainbow;

    public static void renderAll(int sw, int sh, boolean editing) {
        curX = 4;
        curY = 4;
        colW = 0;
        for (Module m : Mods.ALL) {
            if (!m.enabled) continue;
            if (m.hud) m.renderHud(sw, sh, editing);
            else if (!editing) m.renderOverlay(sw, sh);
        }
    }

    static int rainbowColor() {
        return java.awt.Color.HSBtoRGB((System.currentTimeMillis() % 6000L) / 6000.0F, 0.45F, 1.0F) & 0xFFFFFF;
    }

    public static TextRenderer fr() {
        return MinecraftClient.getInstance().textRenderer;
    }

    public static void text(Module m, TextRenderer fr, String s, int x, int y) {
        int c = 0xFF000000 | (m.override != 0 ? m.override : (rainbow ? rainbowColor() : m.color.rgb()));
        if (m.shadow.b) fr.drawWithShadow(s, x, y, c);
        else fr.draw(s, x, y, c);
    }

    public static void lines(Module m, int sw, int sh, List<String> lines) {
        if (lines.isEmpty()) return;
        TextRenderer fr = fr();
        int w = 0;
        for (String l : lines) w = Math.max(w, fr.getStringWidth(l));
        final List<String> ls = lines;
        box(m, sw, sh, w + 6, ls.size() * 10 + 2, new Painter() {
            @Override
            public void paint(Module mm, TextRenderer f) {
                for (int i = 0; i < ls.size(); i++) text(mm, f, ls.get(i), 3, 2 + i * 10);
            }
        });
    }

    /** Positions a w x h box for the module, applies its scale, draws the background and calls the painter. */
    public static void box(Module m, int sw, int sh, int w, int h, Painter p) {
        float s = m.scale.v;
        int ew = (int) Math.ceil(w * s);
        int eh = (int) Math.ceil(h * s);
        int x, y;
        if (m.moved) {
            x = (int) (m.xf * Math.max(0, sw - ew));
            y = (int) (m.yf * Math.max(0, sh - eh));
        } else {
            if (curY + eh > sh - 4 && curY > 4) {
                curX += colW + 6;
                curY = 4;
                colW = 0;
            }
            x = curX;
            y = curY;
            curY += eh + 2;
            colW = Math.max(colW, ew);
        }
        m.lastX = x;
        m.lastY = y;
        m.lastW = ew;
        m.lastH = eh;
        GL11.glPushMatrix();
        GL11.glTranslatef(x, y, 0.0F);
        GL11.glScalef(s, s, 1.0F);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        if (m.background.b) DrawableHelper.fill(0, 0, w, h, 0x70000000);
        p.paint(m, fr());
        GL11.glPopMatrix();
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
