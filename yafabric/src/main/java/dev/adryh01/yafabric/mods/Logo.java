package dev.adryh01.yafabric.mods;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.util.Identifier;
import org.lwjgl.opengl.GL11;

/** The white YA logo (256x256). While the file is missing, a small white "YA" text is drawn instead. */
final class Logo {

    private static final Identifier TEX = new Identifier("yafabric", "textures/ya_logo_white.png");
    private static int state; // 0 = not checked, 1 = found, 2 = missing

    private Logo() {
    }

    static boolean has() {
        if (state == 0) {
            try {
                MinecraftClient.getInstance().getResourceManager().getResource(TEX).getInputStream().close();
                state = 1;
            } catch (Exception e) {
                state = 2;
            }
        }
        return state == 1;
    }

    static int width(int size) {
        return has() ? size : MinecraftClient.getInstance().textRenderer.getStringWidth("YA");
    }

    /** Draws the logo with its top-left corner at x, y in the current GL space. */
    static void draw(int x, int y, int size, float alpha) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (has()) {
            int s = Gl.save();
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glEnable(GL11.GL_TEXTURE_2D);
            mc.getTextureManager().bindTexture(TEX);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, alpha);
            DrawableHelper.drawTexture(x, y, 0.0F, 0.0F, 256, 256, size, size, 256.0F, 256.0F);
            Gl.restore(s);
        } else {
            int a = (int) (alpha * 255.0F);
            if (a > 8) mc.textRenderer.draw("YA", x, y + (size - 8) / 2, (a << 24) | 0xFFFFFF);
        }
    }
}
