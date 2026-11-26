package dev.adryh01.yamods;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.DrawBlockHighlightEvent;
import net.minecraftforge.client.event.EntityViewRenderEvent;

import java.util.ArrayList;
import java.util.List;

/** Base class of every YAMods module. HUD modules also get position/scale/shadow/background/colour settings. */
public abstract class Module {

    public final String id, name, desc, category;
    public boolean enabled;
    public final List<Setting> settings = new ArrayList<Setting>();

    // HUD
    public boolean hud;
    public float xf, yf; // position as a fraction of the free screen space (0..1)
    public int lastX, lastY, lastW, lastH; // bounds of the last render, in GUI pixels
    public Setting scale, shadow, background, color;
    public boolean showInDebug; // still drawn while the F3 screen is open
    public boolean moved;   // false = automatically stacked on the left, true = placed by the user
    public int override;    // 0 = use the colour setting, otherwise 0xRRGGBB for this frame

    // menu animation state
    public float knob, hover;

    protected final Minecraft mc = Minecraft.getMinecraft();

    protected Module(String id, String name, String desc, String category) {
        this.id = id;
        this.name = name;
        this.desc = desc;
        this.category = category;
    }

    /** Marks the module as a HUD element and appends the common HUD settings. */
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

    /** Called every client tick while the module is enabled. */
    public void onTick() {
    }

    /** Drawing in the 3D world (after the world is rendered). */
    public void renderWorld(float partialTicks) {
    }

    /** Return true to replace the vanilla block outline. */
    public boolean drawBlock(DrawBlockHighlightEvent e) {
        return false;
    }

    /** Camera set-up (runs before the camera rotation is applied). */
    public void camera(EntityViewRenderEvent.CameraSetup e) {
    }

    /** Runs right before the first-person hand is drawn; GL changes apply to the hand. */
    public void renderHand(float partialTicks) {
    }

    /** You attacked an entity. */
    public void attacked(Entity target) {
    }

    public void chat(ClientChatReceivedEvent e) {
    }

    /** Left mouse button pressed while no screen is open. */
    public void leftClick() {
    }

    /** Full-screen drawing that is not a HUD box (crosshair, low health border...). */
    public void renderOverlay(int sw, int sh) {
    }

    /** Draws the HUD element. 'editing' = true inside the HUD editor (show sample data when empty). */
    public void renderHud(int sw, int sh, boolean editing) {
    }
}
