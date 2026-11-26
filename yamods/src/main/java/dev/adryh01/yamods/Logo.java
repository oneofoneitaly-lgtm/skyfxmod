package dev.adryh01.yamods;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;

/**
 * The white YA logo (assets/yamods/textures/ya_logo_white.png, 256x256).
 * While that file is not in the mod yet, a small white "YA" text is drawn instead.
 */
final class Logo {

    private static final ResourceLocation TEX = new ResourceLocation("yamods", "textures/ya_logo_white.png");
    private static int state; // 0 = not checked, 1 = found, 2 = missing

    private Logo() {
    }

    static boolean has() {
        if (state == 0) {
            try {
                Minecraft.getMinecraft().getResourceManager().getResource(TEX).getInputStream().close();
                state = 1;
            } catch (Exception e) {
                state = 2;
            }
        }
        return state == 1;
    }

    static int width(int size) {
        return has() ? size : Minecraft.getMinecraft().fontRendererObj.getStringWidth("YA");
    }

    /** Draws the logo with its top-left corner at x, y in the current GL space. */
    static void draw(int x, int y, int size, float alpha) {
        Minecraft mc = Minecraft.getMinecraft();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        if (has()) {
            mc.getTextureManager().bindTexture(TEX);
            GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
            Gui.drawScaledCustomSizeModalRect(x, y, 0.0F, 0.0F, 256, 256, size, size, 256.0F, 256.0F);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        } else {
            int a = (int) (alpha * 255.0F);
            if (a > 8) mc.fontRendererObj.drawString("YA", x, y + (size - 8) / 2, (a << 24) | 0xFFFFFF);
        }
    }
}
