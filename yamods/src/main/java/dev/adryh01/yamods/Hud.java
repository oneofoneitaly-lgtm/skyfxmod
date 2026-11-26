package dev.adryh01.yamods;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;

import java.util.List;

/** Shared HUD drawing: positions a module, applies scale/background and paints its content. */
public final class Hud {

    public interface Painter {
        void paint(Module m, FontRenderer fr);
    }

    private Hud() {
    }

    private static int curX, curY, colW;

    public static void renderAll(int sw, int sh, boolean editing) {
        renderAll(sw, sh, editing, false);
    }

    public static void renderAll(int sw, int sh, boolean editing, boolean debugOnly) {
        curX = 4;
        curY = 4;
        colW = 0;
        for (Module m : Mods.ALL) {
            if (!m.enabled || (debugOnly && !m.showInDebug)) continue;
            if (m.hud) m.renderHud(sw, sh, editing);
            else if (!editing) m.renderOverlay(sw, sh);
        }
    }

    static int rainbow() {
        return java.awt.Color.HSBtoRGB((System.currentTimeMillis() % 6000L) / 6000.0F, 0.45F, 1.0F) & 0xFFFFFF;
    }

    public static FontRenderer fr() {
        return Minecraft.getMinecraft().fontRendererObj;
    }

    /** Draws a string with the module's colour and shadow setting. */
    public static void text(Module m, FontRenderer fr, String s, int x, int y) {
        int c = 0xFF000000 | (m.override != 0 ? m.override : (Mods.rainbow ? rainbow() : m.color.rgb()));
        if (m.shadow.b) fr.drawStringWithShadow(s, x, y, c);
        else fr.drawString(s, x, y, c);
    }

    public static void lines(Module m, int sw, int sh, List<String> lines) {
        if (lines.isEmpty()) return;
        FontRenderer fr = fr();
        int w = 0;
        for (String l : lines) w = Math.max(w, fr.getStringWidth(l));
        final List<String> ls = lines;
        box(m, sw, sh, w + 6, ls.size() * 10 + 2, new Painter() {
            @Override
            public void paint(Module mm, FontRenderer f) {
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

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0.0F);
        GlStateManager.scale(s, s, 1.0F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        if (m.background.b) Gui.drawRect(0, 0, w, h, 0x70000000);
        p.paint(m, fr());
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
