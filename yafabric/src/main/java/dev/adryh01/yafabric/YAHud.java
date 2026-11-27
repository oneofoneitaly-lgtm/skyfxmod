package dev.adryh01.yafabric;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;

/** Stage 2: the first YA HUD modules on Fabric (FPS and coordinates). */
public final class YAHud {

    private static long windowStart = System.currentTimeMillis();
    private static int frames, fps;

    private YAHud() {
    }

    public static void render() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        frames++;
        long now = System.currentTimeMillis();
        if (now - windowStart >= 1000L) {
            fps = frames;
            frames = 0;
            windowStart = now;
        }
        TextRenderer fr = mc.textRenderer;
        line(fr, "FPS: " + fps, 4, 4);
        line(fr, "XYZ: " + (int) Math.floor(mc.player.x) + " " + (int) Math.floor(mc.player.y) + " " + (int) Math.floor(mc.player.z), 4, 16);
    }

    private static void line(TextRenderer fr, String s, int x, int y) {
        DrawableHelper.fill(x - 2, y - 2, x + fr.getStringWidth(s) + 2, y + 10, 0x70000000);
        fr.draw(s, x, y, 0xFFFFFFFF);
    }
}
