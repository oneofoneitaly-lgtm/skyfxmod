package dev.adryh01.yamods;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import org.lwjgl.opengl.Display;

/** YA Boost: puts the video settings that cost the most FPS on their cheap value, once per session. */
public class Boost extends Module {

    final Setting detail = Setting.bool("detail", "Fast graphics, no clouds, no entity shadows", true);
    final Setting particles = Setting.bool("particles", "Minimal particles", true);
    final Setting light = Setting.bool("light", "Smooth lighting off", true);
    final Setting vsync = Setting.bool("vsync", "VSync off, no FPS limit", true);

    private boolean applied;
    private boolean saved;
    private boolean fancy, vs;
    private int ao, cl, pa, lim;
    private boolean shadows;

    Boost() {
        super("boost", "YA Boost", "Switches the heavy video options to their cheap value for more FPS.", "Utility");
        settings.add(detail);
        settings.add(particles);
        settings.add(light);
        settings.add(vsync);
        enabled = true;
    }

    @Override
    public void onTick() {
        if (applied) return;
        applied = true;
        apply();
    }

    @Override
    public void onEnable() {
        applied = true;
        apply();
    }

    @Override
    public void onDisable() {
        applied = false;
        if (!saved) return;
        GameSettings g = Minecraft.getMinecraft().gameSettings;
        g.fancyGraphics = fancy;
        g.ambientOcclusion = ao;
        g.clouds = cl;
        g.particleSetting = pa;
        g.entityShadows = shadows;
        g.enableVsync = vs;
        g.limitFramerate = lim;
        Display.setVSyncEnabled(vs);
        reload(g);
        saved = false;
    }

    private void apply() {
        Minecraft mc = Minecraft.getMinecraft();
        GameSettings g = mc.gameSettings;
        if (!saved) {
            fancy = g.fancyGraphics;
            ao = g.ambientOcclusion;
            cl = g.clouds;
            pa = g.particleSetting;
            shadows = g.entityShadows;
            vs = g.enableVsync;
            lim = g.limitFramerate;
            saved = true;
        }
        boolean redo = false;
        if (detail.b) {
            if (g.fancyGraphics) redo = true;
            g.fancyGraphics = false;
            g.clouds = 0;
            g.entityShadows = false;
        }
        if (light.b) {
            if (g.ambientOcclusion != 0) redo = true;
            g.ambientOcclusion = 0;
        }
        if (particles.b) g.particleSetting = 2;
        if (vsync.b) {
            g.enableVsync = false;
            g.limitFramerate = 260;
            Display.setVSyncEnabled(false);
        }
        if (redo) reload(g);
    }

    private void reload(GameSettings g) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.renderGlobal != null) mc.renderGlobal.loadRenderers();
        g.saveOptions();
    }
}
