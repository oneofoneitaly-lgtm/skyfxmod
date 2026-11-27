package dev.adryh01.yamenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

final class UI {

    static final int BG = 0xFFF3F3F3, HEADER = 0xFFE9E9E9, CARD = 0xFFFFFFFF, BORDER = 0xFFD6D6D6;
    static final int DARK = 0xFF2E2E2E, MID = 0xFF6B6B6B, SOFT = 0xFF9A9A9A, ACCENT = 0xFF3A3A3A, RED = 0xFFE21C30;
    private static final ResourceLocation LOGO = new ResourceLocation("yamenu", "textures/ya_logo.png");

    private UI() {
    }

    static int lerp(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) + ((((b >> 16) & 255) - ((a >> 16) & 255)) * t));
        int g = (int) (((a >> 8) & 255) + ((((b >> 8) & 255) - ((a >> 8) & 255)) * t));
        int bl = (int) ((a & 255) + (((b & 255) - (a & 255)) * t));
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    static void roundRect(int x1, int y1, int x2, int y2, int r, int color) {
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

    static void box(int x1, int y1, int x2, int y2, int r, int border, int fill) {
        roundRect(x1, y1, x2, y2, r, border);
        roundRect(x1 + 1, y1 + 1, x2 - 1, y2 - 1, Math.max(0, r - 1), fill);
    }

    static void logo(int x, int y, int size) {
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        Minecraft.getMinecraft().getTextureManager().bindTexture(LOGO);
        Gui.drawScaledCustomSizeModalRect(x, y, 0.0F, 0.0F, 256, 256, size, size, 256.0F, 256.0F);
    }

    static void logoA(int x, int y, int size, float alpha) {
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
        Minecraft.getMinecraft().getTextureManager().bindTexture(LOGO);
        Gui.drawScaledCustomSizeModalRect(x, y, 0.0F, 0.0F, 256, 256, size, size, 256.0F, 256.0F);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    static void scaledText(String s, int cx, int y, float scale, int color) {
        Minecraft mc = Minecraft.getMinecraft();
        GlStateManager.pushMatrix();
        GlStateManager.translate(cx, y, 0);
        GlStateManager.scale(scale, scale, 1.0F);
        mc.fontRendererObj.drawString(s, -mc.fontRendererObj.getStringWidth(s) / 2, 0, color);
        GlStateManager.popMatrix();
    }
}
