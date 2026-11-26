package dev.adryh01.pvpfx;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.opengl.GL11;

/** Small drawing helpers for the white/grey menu style (same look as SkyFX). */
public final class UI {

    public static final int BG = 0xFFF3F3F3;
    public static final int HEADER = 0xFFE9E9E9;
    public static final int CARD = 0xFFFFFFFF;
    public static final int CARD_HOVER = 0xFFFAFAFA;
    public static final int BORDER = 0xFFD6D6D6;
    public static final int LINE = 0xFFDDDDDD;
    public static final int DARK = 0xFF2E2E2E;
    public static final int MID = 0xFF6B6B6B;
    public static final int SOFT = 0xFF9A9A9A;
    public static final int ACCENT = 0xFF3A3A3A;
    public static final int CHIP = 0xFFE3E3E3;

    private UI() {
    }

    public static int lerp(int a, int b, float t) {
        t = Math.max(0.0F, Math.min(1.0F, t));
        int r = (int) (((a >> 16) & 255) + ((((b >> 16) & 255) - ((a >> 16) & 255)) * t));
        int g = (int) (((a >> 8) & 255) + ((((b >> 8) & 255) - ((a >> 8) & 255)) * t));
        int bl = (int) ((a & 255) + (((b & 255) - (a & 255)) * t));
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    public static void roundRect(int x1, int y1, int x2, int y2, int r, int color) {
        if (x2 <= x1 || y2 <= y1) return;
        r = Math.max(0, Math.min(r, Math.min((x2 - x1) / 2, (y2 - y1) / 2)));
        if (r == 0) {
            Gui.drawRect(x1, y1, x2, y2, color);
            return;
        }
        Gui.drawRect(x1, y1 + r, x2, y2 - r, color);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5D;
            int inset = (int) Math.round(r - Math.sqrt(r * r - dy * dy));
            Gui.drawRect(x1 + inset, y1 + i, x2 - inset, y1 + i + 1, color);
            Gui.drawRect(x1 + inset, y2 - i - 1, x2 - inset, y2 - i, color);
        }
    }

    public static void roundBox(int x1, int y1, int x2, int y2, int r, int border, int fill, int thick) {
        roundRect(x1, y1, x2, y2, r, border);
        roundRect(x1 + thick, y1 + thick, x2 - thick, y2 - thick, Math.max(0, r - thick), fill);
    }

    /** On/off switch, 22 x 12. knob = 0..1. */
    public static void sw(int x, int y, float knob) {
        roundRect(x, y, x + 22, y + 12, 5, lerp(0xCFCFCF, 0x3A3A3A, knob));
        int kx = x + 2 + (int) ((22 - 4 - 8) * knob);
        roundRect(kx, y + 2, kx + 8, y + 10, 3, 0xFFFFFFFF);
    }

    public static void scissor(Minecraft mc, int x1, int y1, int x2, int y2) {
        int s = new ScaledResolution(mc).getScaleFactor();
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(x1 * s, mc.displayHeight - y2 * s, Math.max(0, (x2 - x1) * s), Math.max(0, (y2 - y1) * s));
    }

    public static void scissorOff() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }
}
