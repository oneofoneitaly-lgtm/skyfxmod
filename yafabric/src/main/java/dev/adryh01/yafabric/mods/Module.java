package dev.adryh01.yafabric.mods;

import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;

/** Base class of every YA module on Fabric. HUD modules also get position/scale/shadow/background/colour settings. */
public abstract class Module {

    public final String id, name, desc, category;
    public boolean enabled;
    public final List<Setting> settings = new ArrayList<Setting>();

    // HUD
    public boolean hud;
    public float xf, yf;
    public int lastX, lastY, lastW, lastH;
    public Setting scale, shadow, background, color;
    public boolean moved;
    public int override;

    // menu animation state
    public float knob, hover;

    protected final MinecraftClient mc = MinecraftClient.getInstance();

    protected Module(String id, String name, String desc, String category) {
        this.id = id;
        this.name = name;
        this.desc = desc;
        this.category = category;
    }

    protected void asHud(float xf, float yf, int colorIdx) {
        this.hud = true;
        this.xf = xf;
        this.yf = yf;
        scale = Setting.slider("scale", "Scale", 1.0F, 0.6F, 2.0F, 0.1F);
        shadow = Setting.bool("shadow", "Text shadow", true);
        background = Setting.bool("background", "Background", false);
        color = Setting.color("color", "Text colour", colorIdx);
        settings.add(scale);
        settings.add(shadow);
        settings.add(background);
        settings.add(color);
    }

    public Setting setting(String key) {
        for (Setting s : settings) {
            if (s.key.equals(key)) return s;
        }
        return null;
    }

    public void setEnabled(boolean e) {
        if (e == enabled) return;
        enabled = e;
        if (e) onEnable();
        else onDisable();
    }

    public void onEnable() {
    }

    public void onDisable() {
    }

    /** Every client tick while the module is enabled. */
    public void onTick() {
    }

    /** Full-screen drawing that is not a HUD box. */
    public void renderOverlay(int sw, int sh) {
    }

    /** Draws the HUD element. editing = true inside the HUD editor (show sample data when empty). */
    public void renderHud(int sw, int sh, boolean editing) {
    }
}
