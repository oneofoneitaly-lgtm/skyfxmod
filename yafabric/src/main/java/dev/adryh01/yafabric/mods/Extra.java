package dev.adryh01.yafabric.mods;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

import java.util.List;

/** Second batch: camera and view modules, crosshair, target HUD, video tweaks. */
public final class Extra {

    static final Zoom ZOOM = new Zoom();
    static final Freelook FREELOOK = new Freelook();

    private Extra() {
    }

    static void register(List<Module> all) {
        all.add(ZOOM);
        all.add(FREELOOK);
        all.add(new Fov());
        all.add(new NoBobbing());
        all.add(new UnlimitedFps());
        all.add(new VsyncOff());
        all.add(new ToggleSneak());
        all.add(new RainbowHud());
        all.add(new LowHealth());
        all.add(new Crosshair());
        all.add(new TargetHud());
    }

    /** FOV multiplier used by the GameRenderer mixin (1 = unchanged). */
    public static float fovMultiplier() {
        return ZOOM.enabled && (ZOOM.zooming || ZOOM.anim > 0.0F) ? ZOOM.fovMultiplier() : 1.0F;
    }

    public static boolean crosshairHidden() {
        Module c = Mods.byId("crosshair");
        return c != null && c.enabled;
    }

    /** Called by the GameRenderer mixin right before / after a frame (Freelook swaps the player's rotation). */
    public static void frameStart() {
        if (FREELOOK.enabled) FREELOOK.renderTickStart();
    }

    public static void frameEnd() {
        FREELOOK.renderTickEnd();
    }

    static final class Zoom extends Module {
        final Setting amount = Setting.slider("amount", "Zoom amount", 4.0F, 1.5F, 10.0F, 0.5F);
        final Setting smooth = Setting.bool("smooth", "Smooth camera", false);
        float anim;
        boolean zooming, changedSmooth, prevSmooth;

        Zoom() {
            super("zoom", "Zoom", "Hold C to zoom in.", "Utility");
            settings.add(amount);
            settings.add(smooth);
        }

        long last = System.nanoTime();
        float eased;

        /** Called every frame (from the FOV mixin): time-based animation, independent of FPS. */
        float fovMultiplier() {
            long now = System.nanoTime();
            float dt = Math.min(0.1F, (now - last) / 1.0E9F);
            last = now;
            float target = zooming ? 1.0F : 0.0F;
            anim += (target - anim) * (1.0F - (float) Math.exp(-dt * 16.0F));
            if (Math.abs(target - anim) < 0.002F) anim = target;
            eased = anim * anim * (3.0F - 2.0F * anim);
            return 1.0F - eased * (1.0F - 1.0F / amount.v);
        }

        @Override
        public void onTick() {
            boolean down = Keyboard.isKeyDown(Keyboard.KEY_C) && mc.currentScreen == null;
            zooming = down;
            if (smooth.b) {
                if (down && !changedSmooth) {
                    prevSmooth = mc.options.smoothCameraEnabled;
                    mc.options.smoothCameraEnabled = true;
                    changedSmooth = true;
                } else if (!down && changedSmooth) {
                    mc.options.smoothCameraEnabled = prevSmooth;
                    changedSmooth = false;
                }
            }
        }

        @Override
        public void onDisable() {
            anim = 0.0F;
            zooming = false;
            if (changedSmooth) {
                mc.options.smoothCameraEnabled = prevSmooth;
                changedSmooth = false;
            }
        }
    }

    /** Hold Left Alt and move the mouse to look around while your character keeps facing the same way. */
    static final class Freelook extends Module {
        final Setting third = Setting.bool("third", "Third person while looking", true);
        final Setting invert = Setting.bool("invert", "Invert vertical look", false);
        float camYaw, camPitch, startPitch, realYaw, realPitch, realPrevYaw, realPrevPitch;
        boolean active, swapped, changedView;
        int prevView;

        Freelook() {
            super("freelook", "Freelook", "Hold Left Alt to look around without turning your character.", "Utility");
            settings.add(third);
            settings.add(invert);
        }

        @Override
        public void onTick() {
            ClientPlayerEntity p = mc.player;
            boolean down = p != null && mc.currentScreen == null && Keyboard.isKeyDown(Keyboard.KEY_LMENU);
            if (down && !active) {
                active = true;
                camYaw = p.yaw;
                camPitch = p.pitch;
                prevView = mc.options.perspective;
                if (third.b && prevView == 0) {
                    mc.options.perspective = 1;
                    changedView = true;
                }
            } else if (!down && active) {
                stop();
            }
        }

        private void stop() {
            active = false;
            if (changedView) {
                mc.options.perspective = prevView;
                changedView = false;
            }
        }

        @Override
        public void onDisable() {
            stop();
        }

        void renderTickStart() {
            ClientPlayerEntity p = mc.player;
            if (!active || p == null || swapped) return;
            realYaw = p.yaw;
            realPitch = p.pitch;
            realPrevYaw = p.prevYaw;
            realPrevPitch = p.prevPitch;
            p.yaw = camYaw;
            p.prevYaw = camYaw;
            p.pitch = camPitch;
            p.prevPitch = camPitch;
            startPitch = camPitch;
            swapped = true;
        }

        void renderTickEnd() {
            ClientPlayerEntity p = mc.player;
            if (!swapped || p == null) {
                swapped = false;
                return;
            }
            swapped = false;
            camYaw = p.yaw;
            float dp = p.pitch - startPitch;
            camPitch = startPitch + (invert.b ? -dp : dp);
            if (camPitch > 90.0F) camPitch = 90.0F;
            if (camPitch < -90.0F) camPitch = -90.0F;
            p.yaw = realYaw;
            p.pitch = realPitch;
            p.prevYaw = realPrevYaw;
            p.prevPitch = realPrevPitch;
        }
    }

    /** Changes a vanilla option while enabled and gives it back when disabled (or while the options screens are open). */
    abstract static class GsTweak extends Module {
        private boolean applied;

        GsTweak(String id, String name, String desc, String cat) {
            super(id, name, desc, cat);
        }

        abstract void save();

        abstract void apply();

        abstract void restore();

        @Override
        public void onTick() {
            Screen s = mc.currentScreen;
            if (s != null && s.getClass().getName().contains("Options")) {
                if (applied) {
                    restore();
                    applied = false;
                }
                return;
            }
            if (!applied) {
                save();
                applied = true;
            }
            apply();
        }

        @Override
        public void onDisable() {
            if (applied) {
                restore();
                applied = false;
            }
        }
    }

    static final class Fov extends GsTweak {
        final Setting fov = Setting.slider("fov_value", "Field of view", 90.0F, 30.0F, 110.0F, 1.0F);
        float orig = 70.0F;

        Fov() {
            super("fov", "FOV", "Set your field of view quickly (30 - 110).", "Utility");
            settings.add(fov);
        }

        void save() {
            orig = mc.options.fov;
        }

        void apply() {
            mc.options.fov = fov.v;
        }

        void restore() {
            mc.options.fov = orig;
        }
    }

    static final class NoBobbing extends GsTweak {
        boolean orig;

        NoBobbing() {
            super("nobobbing", "No Bobbing", "Steady camera while walking.", "Utility");
        }

        void save() {
            orig = mc.options.bobView;
        }

        void apply() {
            mc.options.bobView = false;
        }

        void restore() {
            mc.options.bobView = orig;
        }
    }

    static final class UnlimitedFps extends GsTweak {
        int orig = 120;

        UnlimitedFps() {
            super("unlimitedfps", "Unlimited FPS", "Removes the FPS limit.", "Utility");
        }

        void save() {
            orig = mc.options.maxFramerate;
        }

        void apply() {
            mc.options.maxFramerate = 260;
        }

        void restore() {
            mc.options.maxFramerate = orig;
        }
    }

    static final class VsyncOff extends GsTweak {
        boolean orig;

        VsyncOff() {
            super("vsyncoff", "VSync Off", "Turns VSync off for lower input delay.", "Utility");
        }

        void save() {
            orig = mc.options.vsync;
        }

        void apply() {
            if (mc.options.vsync) {
                mc.options.vsync = false;
                Display.setVSyncEnabled(false);
            }
        }

        void restore() {
            mc.options.vsync = orig;
            Display.setVSyncEnabled(orig);
        }
    }

    static final class ToggleSneak extends Module {
        private boolean toggled, prev;

        ToggleSneak() {
            super("togglesneak", "Toggle Sneak", "Press sneak once to stay crouched.", "Utility");
        }

        @Override
        public void onTick() {
            if (mc.currentScreen != null) return;
            int code = mc.options.sneakKey.getCode();
            boolean down = code > 0 && Keyboard.isKeyDown(code);
            if (down && !prev) toggled = !toggled;
            prev = down;
            KeyBinding.setKeyPressed(code, toggled);
        }

        @Override
        public void onDisable() {
            toggled = false;
            KeyBinding.setKeyPressed(mc.options.sneakKey.getCode(), false);
        }
    }

    static final class RainbowHud extends Module {
        RainbowHud() {
            super("rainbowhud", "Rainbow HUD", "HUD text slowly cycles through soft colours.", "Visual");
        }

        @Override
        public void onEnable() {
            Hud.rainbow = true;
        }

        @Override
        public void onDisable() {
            Hud.rainbow = false;
        }
    }

    static final class LowHealth extends Module {
        final Setting limit = Setting.slider("health_limit", "Show below (HP)", 8.0F, 2.0F, 20.0F, 1.0F);
        final Setting color = Setting.color("color", "Colour", 1);

        LowHealth() {
            super("lowhealth", "Low Health", "Red border when your health is low.", "Visual");
            settings.add(limit);
            settings.add(color);
        }

        @Override
        public void renderOverlay(int sw, int sh) {
            ClientPlayerEntity p = mc.player;
            if (p == null || p.getHealth() > limit.v || p.getHealth() <= 0.0F) return;
            float pulse = 0.65F + 0.35F * (float) Math.sin(System.currentTimeMillis() / 220.0D);
            int rgb = color.rgb();
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            int steps = 10;
            for (int i = 0; i < steps; i++) {
                int a = (int) (110 * pulse * (1.0F - i / (float) steps));
                int c = (a << 24) | rgb;
                int w = 3 * i, o = 3;
                DrawableHelper.fill(w, w, sw - w, w + o, c);
                DrawableHelper.fill(w, sh - w - o, sw - w, sh - w, c);
                DrawableHelper.fill(w, w + o, w + o, sh - w - o, c);
                DrawableHelper.fill(sw - w - o, w + o, sw - w, sh - w - o, c);
            }
        }
    }

    static final class Crosshair extends Module {
        final Setting colorS = Setting.color("color", "Colour", 0);
        final Setting size = Setting.slider("size", "Size", 5.0F, 2.0F, 15.0F, 1.0F);
        final Setting gap = Setting.slider("gap", "Gap", 2.0F, 0.0F, 8.0F, 1.0F);
        final Setting thick = Setting.slider("thick", "Thickness", 1.0F, 1.0F, 3.0F, 1.0F);
        final Setting dot = Setting.bool("dot", "Centre dot", false);

        Crosshair() {
            super("crosshair", "Crosshair", "A custom crosshair. Hides the vanilla one.", "Visual");
            settings.add(colorS);
            settings.add(size);
            settings.add(gap);
            settings.add(thick);
            settings.add(dot);
        }

        @Override
        public void renderOverlay(int sw, int sh) {
            if (mc.options.perspective != 0 || mc.currentScreen != null) return;
            int cx = sw / 2, cy = sh / 2;
            int s = (int) size.v, g = (int) gap.v, t = (int) thick.v;
            int c = 0xFF000000 | colorS.rgb();
            int h = t / 2;
            DrawableHelper.fill(cx - g - s, cy - h, cx - g, cy - h + t, c);
            DrawableHelper.fill(cx + g, cy - h, cx + g + s, cy - h + t, c);
            DrawableHelper.fill(cx - h, cy - g - s, cx - h + t, cy - g, c);
            DrawableHelper.fill(cx - h, cy + g, cx - h + t, cy + g + s, c);
            if (dot.b) DrawableHelper.fill(cx - h, cy - h, cx - h + t, cy - h + t, c);
        }
    }

    static final class TargetHud extends Module {
        LivingEntity target;
        long seen;

        TargetHud() {
            super("targethud", "Target HUD", "Health of the player you are aiming at.", "PvP");
            asHud(0.5F, 0.58F, 0);
            moved = true;
            background.b = true;
        }

        @Override
        public void onTick() {
            BlockHitResult o = mc.result;
            if (o != null && o.type == BlockHitResult.Type.ENTITY && o.entity instanceof LivingEntity) {
                target = (LivingEntity) o.entity;
                seen = System.currentTimeMillis();
            } else if (System.currentTimeMillis() - seen > 2500L || (target != null && target.removed)) {
                target = null;
            }
        }

        @Override
        public void renderHud(int sw, int sh, boolean editing) {
            final LivingEntity t = target;
            if (t == null && !editing) return;
            final String name = t == null ? "Player" : (t instanceof net.minecraft.entity.player.PlayerEntity ? ((net.minecraft.entity.player.PlayerEntity) t).getGameProfile().getName() : "Mob");
            final float hp = t == null ? 14.0F : t.getHealth();
            final float max = t == null ? 20.0F : Math.max(1.0F, t.getMaxHealth());
            Hud.box(this, sw, sh, 110, 30, new Hud.Painter() {
                @Override
                public void paint(Module m, TextRenderer fr) {
                    Hud.text(m, fr, name, 4, 4);
                    String v = Q.f("%.1f HP", hp);
                    Hud.text(m, fr, v, 106 - fr.getStringWidth(v), 4);
                    DrawableHelper.fill(4, 17, 106, 23, 0x80000000);
                    float f = Math.max(0.0F, Math.min(1.0F, hp / max));
                    int col = f > 0.6F ? 0xFF55FF55 : (f > 0.3F ? 0xFFFFAA00 : 0xFFFF5555);
                    DrawableHelper.fill(4, 17, 4 + (int) (102 * f), 23, col);
                }
            });
        }
    }
}
