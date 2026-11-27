package dev.adryh01.yafabric.mods;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;
import org.lwjgl.opengl.Display;

/** YA Boost: puts the video settings that cost the most FPS on their cheap value, once per session. */
public class Boost extends Module {

    final Setting detail = Setting.bool("detail", "Fast graphics, no clouds, no entity shadows", true);
    final Setting particles = Setting.bool("particles", "Minimal particles", true);
    final Setting light = Setting.bool("light", "Smooth lighting off", true);
    final Setting vsync = Setting.bool("vsync", "VSync off, no FPS limit", true);

    private boolean applied, saved;
    private boolean fancy, vs, shadows;
    private int ao, cl, pa, lim;

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
        GameOptions g = mc.options;
        g.fancyGraphics = fancy;
        g.ao = ao;
        g.cloudMode = cl;
        g.particle = pa;
        g.entityShadows = shadows;
        g.vsync = vs;
        g.maxFramerate = lim;
        Display.setVSyncEnabled(vs);
        reload(g);
        saved = false;
    }

    private void apply() {
        GameOptions g = mc.options;
        if (!saved) {
            fancy = g.fancyGraphics;
            ao = g.ao;
            cl = g.cloudMode;
            pa = g.particle;
            shadows = g.entityShadows;
            vs = g.vsync;
            lim = g.maxFramerate;
            saved = true;
        }
        boolean redo = false;
        if (detail.b) {
            if (g.fancyGraphics) redo = true;
            g.fancyGraphics = false;
            g.cloudMode = 0;
            g.entityShadows = false;
        }
        if (light.b) {
            if (g.ao != 0) redo = true;
            g.ao = 0;
        }
        if (particles.b) g.particle = 2;
        if (vsync.b) {
            g.vsync = false;
            g.maxFramerate = 260;
            Display.setVSyncEnabled(false);
        }
        if (redo) reload(g);
    }

    private void reload(GameOptions g) {
        if (mc.worldRenderer != null) mc.worldRenderer.reload();
        g.save();
    }
}
