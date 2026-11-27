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
    final Setting vbo = Setting.bool("vbo", "VBO rendering (faster chunks)", true);
    final Setting entities = Setting.bool("entities", "Hide far mobs and items (players always shown)", true);
    final Setting dist = Setting.slider("dist", "Max render distance (chunks)", 8.0F, 4.0F, 16.0F, 1.0F);

    private boolean applied;
    private boolean saved;
    private boolean fancy, vs;
    private int ao, cl, pa, lim;
    private boolean shadows;
    private boolean vb;
    private int rd;

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
            if (mc.theWorld == null) {
                origW.clear();
                return;
            }
            java.util.List<net.minecraft.entity.Entity> list = mc.theWorld.loadedEntityList;
            for (int i = 0; i < list.size(); i++) {
                net.minecraft.entity.Entity e = list.get(i);
                if (e == null || e instanceof net.minecraft.entity.player.EntityPlayer) continue;
                if (on) {
                    if (!origW.containsKey(e)) origW.put(e, e.renderDistanceWeight);
                    e.renderDistanceWeight = Math.min(origW.get(e), 0.45D);
                } else {
                    Double o = origW.remove(e);
                    if (o != null) e.renderDistanceWeight = o;
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
        GameSettings g = Minecraft.getMinecraft().gameSettings;
        g.fancyGraphics = fancy;
        g.ambientOcclusion = ao;
        g.clouds = cl;
        g.particleSetting = pa;
        g.entityShadows = shadows;
        g.enableVsync = vs;
        g.limitFramerate = lim;
        g.useVbo = vb;
        g.renderDistanceChunks = rd;
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
            vb = g.useVbo;
            rd = g.renderDistanceChunks;
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
        if (vbo.b && !g.useVbo) {
            g.useVbo = true;
            redo = true;
        }
        if (g.renderDistanceChunks > (int) dist.v) {
            g.renderDistanceChunks = (int) dist.v;
            redo = true;
        }
        if (redo) reload(g);
    }

    private void reload(GameSettings g) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.renderGlobal != null) mc.renderGlobal.loadRenderers();
        g.saveOptions();
    }
}
