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
    final Setting vbo = Setting.bool("vbo", "VBO rendering (faster chunks)", true);
    final Setting entities = Setting.bool("entities", "Hide far mobs and items (players always shown)", true);
    final Setting dist = Setting.slider("dist", "Max render distance (chunks)", 8.0F, 4.0F, 16.0F, 1.0F);

    private boolean applied, saved;
    private boolean fancy, vs, shadows, vb;
    private int ao, cl, pa, lim, rd;

    Boost() {
        super("boost", "YA Boost", "Switches the heavy video options to their cheap value for more FPS.", "Utility");
        settings.add(detail);
        settings.add(particles);
        settings.add(light);
        settings.add(vsync);
        settings.add(vbo);
        settings.add(dist);
        settings.add(entities);
        enabled = true;
    }

    private int ticks;
    private final java.util.WeakHashMap<net.minecraft.entity.Entity, Double> origW = new java.util.WeakHashMap<net.minecraft.entity.Entity, Double>();

    @Override
    public void onTick() {
        if (!applied) {
            applied = true;
            apply();
        } else if (++ticks % 40 == 0) {
            apply(); // keeps working if another mod or the options screen changed a setting
        }
        if (entities.b) {
            if (ticks % 20 == 0) cull(true);
        } else if (!origW.isEmpty()) {
            cull(false);
        }
    }

    /** Far items, mobs, armor stands and xp orbs are not drawn: fewer entities per frame, more FPS in busy places. */
    private void cull(boolean on) {
        try {
            if (mc.world == null) {
                origW.clear();
                return;
            }
            java.util.List<net.minecraft.entity.Entity> list = mc.world.loadedEntities;
            for (int i = 0; i < list.size(); i++) {
                net.minecraft.entity.Entity e = list.get(i);
                if (e == null || e instanceof net.minecraft.entity.player.PlayerEntity) continue;
                if (on) {
                    if (!origW.containsKey(e)) origW.put(e, e.renderDistanceMultiplier);
                    e.renderDistanceMultiplier = Math.min(origW.get(e), 0.45D);
                } else {
                    Double o = origW.remove(e);
                    if (o != null) e.renderDistanceMultiplier = o;
                }
            }
            if (!on) origW.clear();
        } catch (RuntimeException ignored) {
        }
    }

    @Override
    public void onEnable() {
        applied = true;
        apply();
    }

    @Override
    public void onDisable() {
        applied = false;
        cull(false);
        if (!saved) return;
        GameOptions g = mc.options;
        g.fancyGraphics = fancy;
        g.ao = ao;
        g.cloudMode = cl;
        g.particle = pa;
        g.entityShadows = shadows;
        g.vsync = vs;
        g.maxFramerate = lim;
        g.vbo = vb;
        g.viewDistance = rd;
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
            vb = g.vbo;
            rd = g.viewDistance;
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
        if (vbo.b && !g.vbo) {
            g.vbo = true;
            redo = true;
        }
        if (g.viewDistance > (int) dist.v) {
            g.viewDistance = (int) dist.v;
            redo = true;
        }
        if (redo) reload(g);
    }

    private void reload(GameOptions g) {
        if (mc.worldRenderer != null) mc.worldRenderer.reload();
        g.save();
    }
}
