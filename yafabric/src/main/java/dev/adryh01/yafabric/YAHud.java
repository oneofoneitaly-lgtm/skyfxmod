package dev.adryh01.yafabric;

import dev.adryh01.yafabric.mods.Hud;
import dev.adryh01.yafabric.mods.YAHub;
import dev.adryh01.yafabric.mods.Mods;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.Window;
import org.lwjgl.input.Keyboard;

/** In-game drawing of the HUD modules and the Right Shift key that opens the YA menu. */
public final class YAHud {

    private static boolean shiftWas;

    private YAHud() {
    }

    public static void tick() {
        try {
            Mods.tick();
        } catch (RuntimeException e) {
            System.out.println("[YA CLIENT] tick error: " + e);
        }
    }

    public static void render() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        boolean sh = Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        if (sh && !shiftWas && mc.currentScreen == null) {
            dev.adryh01.yafabric.mods.UI.markShiftOpen();
            mc.setScreen(new YAHub());
        }
        shiftWas = sh;
        if (mc.options.hudHidden) return;
        try {
            if (win == null || winW != mc.width || winH != mc.height || winScale != mc.options.guiScale) {
                win = new Window(mc);
                winW = mc.width;
                winH = mc.height;
                winScale = mc.options.guiScale;
            }
            Hud.renderAll((int) win.getScaledWidth(), (int) win.getScaledHeight(), false);
        } catch (RuntimeException e) {
            System.out.println("[YA CLIENT] hud error: " + e);
        }
    }

    private static Window win;
    private static int winW, winH, winScale;
}
