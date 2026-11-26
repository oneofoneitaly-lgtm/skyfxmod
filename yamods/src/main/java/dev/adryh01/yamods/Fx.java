package dev.adryh01.yamods;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiVideoSettings;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.IRenderHandler;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.Display;

import java.util.Random;

/** Modules that change the game or draw on the whole screen (not simple text lines). */
final class Fx {

    private Fx() {
    }

    // ------------------------------------------------------------ helpers

    static final IRenderHandler NO_CLOUDS = new IRenderHandler() {
        @Override
        public void render(float partialTicks, WorldClient world, Minecraft mc) {
        }
    };

    static void hideClouds(boolean hide) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.theWorld.provider == null) return;
        if (hide) mc.theWorld.provider.setCloudRenderer(NO_CLOUDS);
        else if (mc.theWorld.provider.getCloudRenderer() == NO_CLOUDS) mc.theWorld.provider.setCloudRenderer(null);
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
            if (mc.currentScreen instanceof GuiVideoSettings || mc.currentScreen instanceof GuiOptions) {
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

    // ------------------------------------------------------------ gameplay / video

    static class Fov extends GsTweak {
        final Setting fov = Setting.slider("fov_value", "Field of view", 90.0F, 30.0F, 110.0F, 1.0F);
        float orig = 70.0F;

        Fov() {
            super("fov", "FOV", "Set your field of view quickly (30 - 110).", "Utility");
            settings.add(fov);
        }

        void save() {
            orig = mc.gameSettings.fovSetting;
        }

        void apply() {
            mc.gameSettings.fovSetting = fov.v;
        }

        void restore() {
            mc.gameSettings.fovSetting = orig;
        }
    }

    static class FpsBoost extends GsTweak {
        final Setting particles = Setting.choice("particles", "Particles", 2, "All", "Decreased", "Minimal");
        final Setting clouds = Setting.bool("clouds", "Hide clouds", true);
        final Setting shadows = Setting.bool("shadows", "Hide entity shadows", true);
        final Setting bob = Setting.bool("bob", "No view bobbing", false);
        int oParticles;
        boolean oShadows, oBob;

        FpsBoost() {
            super("fpsboost", "FPS Boost", "Fewer particles, no clouds and no shadows for more FPS.", "Utility");
            settings.add(particles);
            settings.add(clouds);
            settings.add(shadows);
            settings.add(bob);
        }

        void save() {
            oParticles = mc.gameSettings.particleSetting;
            oShadows = mc.gameSettings.entityShadows;
            oBob = mc.gameSettings.viewBobbing;
        }

        void apply() {
            mc.gameSettings.particleSetting = particles.idx;
            if (shadows.b) mc.gameSettings.entityShadows = false;
            if (bob.b) mc.gameSettings.viewBobbing = false;
            hideClouds(clouds.b);
        }

        void restore() {
            mc.gameSettings.particleSetting = oParticles;
            mc.gameSettings.entityShadows = oShadows;
            mc.gameSettings.viewBobbing = oBob;
            hideClouds(false);
        }
    }

    static class NoBobbing extends GsTweak {
        boolean orig;

        NoBobbing() {
            super("nobobbing", "No Bobbing", "Steady camera while walking.", "Utility");
        }

        void save() {
            orig = mc.gameSettings.viewBobbing;
        }

        void apply() {
            mc.gameSettings.viewBobbing = false;
        }

        void restore() {
            mc.gameSettings.viewBobbing = orig;
        }
    }

    static class UnlimitedFps extends GsTweak {
        int orig = 120;

        UnlimitedFps() {
            super("unlimitedfps", "Unlimited FPS", "Removes the FPS limit.", "Utility");
        }

        void save() {
            orig = mc.gameSettings.limitFramerate;
        }

        void apply() {
            mc.gameSettings.limitFramerate = 260;
        }

        void restore() {
            mc.gameSettings.limitFramerate = orig;
        }
    }

    static class VsyncOff extends GsTweak {
        boolean orig;

        VsyncOff() {
            super("vsyncoff", "VSync Off", "Turns VSync off for lower input delay.", "Utility");
        }

        void save() {
            orig = mc.gameSettings.enableVsync;
        }

        void apply() {
            if (mc.gameSettings.enableVsync) {
                mc.gameSettings.enableVsync = false;
                Display.setVSyncEnabled(false);
            }
        }

        void restore() {
            mc.gameSettings.enableVsync = orig;
            Display.setVSyncEnabled(orig);
        }
    }

    static class NoClouds extends Module {
        NoClouds() {
            super("noclouds", "No Clouds", "Removes the clouds from the sky.", "Visual");
        }

        @Override
        public void onTick() {
            hideClouds(true);
        }

        @Override
        public void onDisable() {
            hideClouds(false);
        }
    }

    static class NoFovEffects extends Module {
        NoFovEffects() {
            super("nofoveffects", "No FOV Effects", "The view does not stretch when you sprint or have speed.", "PvP");
        }
    }

    static class NoScreenEffects extends Module {
        NoScreenEffects() {
            super("noscreeneffects", "No Screen Effects", "No wobble from nether portals.", "Visual");
        }

        @Override
        public void onTick() {
            EntityPlayerSP p = mc.thePlayer;
            p.timeInPortal = 0.0F;
            p.prevTimeInPortal = 0.0F;
        }
    }

    static class ToggleSneak extends Module {
        private boolean toggled, prev;

        ToggleSneak() {
            super("togglesneak", "Toggle Sneak", "Press sneak once to stay crouched.", "Utility");
        }

        @Override
        public void onTick() {
            if (mc.currentScreen != null) return;
            int code = mc.gameSettings.keyBindSneak.getKeyCode();
            boolean down = code > 0 && Keyboard.isKeyDown(code);
            if (down && !prev) toggled = !toggled;
            prev = down;
            net.minecraft.client.settings.KeyBinding.setKeyBindState(code, toggled);
        }

        @Override
        public void onDisable() {
            toggled = false;
            net.minecraft.client.settings.KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.getKeyCode(), false);
        }
    }

    // ------------------------------------------------------------ effects

    static class HitSparks extends Module {
        HitSparks() {
            super("hitsparks", "Hit Sparks", "Crit sparks on every player or mob you hit.", "PvP");
        }

        void spark(Entity target) {
            if (mc.thePlayer == null) return;
            mc.thePlayer.onCriticalHit(target);
            mc.thePlayer.onEnchantmentCritical(target);
        }
    }

    static class RainbowHud extends Module {
        RainbowHud() {
            super("rainbowhud", "Rainbow HUD", "HUD text slowly cycles through soft colours.", "Visual");
        }

        @Override
        public void onEnable() {
            Mods.rainbow = true;
        }

        @Override
        public void onDisable() {
            Mods.rainbow = false;
        }
    }

    static class Trail extends Module {
        final Setting density = Setting.slider("trail_density", "Density", 6.0F, 1.0F, 12.0F, 1.0F);
        private final Random rnd = new Random();

        Trail() {
            super("trail", "YA Trail", "A sparkle trail behind you while you move.", "Visual");
            settings.add(density);
        }

        @Override
        public void onTick() {
            EntityPlayerSP p = mc.thePlayer;
            double dx = p.posX - p.prevPosX, dz = p.posZ - p.prevPosZ;
            if (dx * dx + dz * dz < 0.0004D) return;
            int n = Math.max(1, (int) (density.v / 3.0F));
            for (int i = 0; i < n; i++) {
                mc.theWorld.spawnParticle(EnumParticleTypes.FIREWORKS_SPARK,
                        p.posX + (rnd.nextDouble() - 0.5D) * 0.5D, p.posY + 0.1D + rnd.nextDouble() * 0.3D,
                        p.posZ + (rnd.nextDouble() - 0.5D) * 0.5D, 0.0D, 0.0D, 0.0D);
            }
        }
    }

    // ------------------------------------------------------------ overlays

    static class Crosshair extends Module {
        static final int N = 15;
        /** Paint colours of the custom crosshair editor; index 0 = empty. */
        static final int[] PAINT = {0, 0xFFFFFF, 0x000000, 0xFF3B3B, 0xFF9F1C, 0xFFE74C, 0x4CD964, 0x2EC4F1, 0x4C7CFF, 0xFF6AD5, 0x9A9A9A, 0xA05A2C};

        final Setting mode = Setting.choice("mode", "Style", 0, "Classic", "My drawing");
        final Setting color = Setting.color("color", "Colour (classic)", 0);
        final Setting size = Setting.slider("size", "Size (classic)", 5.0F, 2.0F, 15.0F, 1.0F);
        final Setting gap = Setting.slider("gap", "Gap (classic)", 2.0F, 0.0F, 8.0F, 1.0F);
        final Setting thick = Setting.slider("thick", "Thickness (classic)", 1.0F, 1.0F, 3.0F, 1.0F);
        final Setting dot = Setting.bool("dot", "Centre dot (classic)", false);
        final Setting scale = Setting.slider("scale", "Size of my drawing", 1.0F, 1.0F, 4.0F, 1.0F);
        final Setting draw = Setting.action("draw", "Draw my crosshair", new Runnable() {
            @Override
            public void run() {
                Minecraft.getMinecraft().displayGuiScreen(new CrosshairEditor(Minecraft.getMinecraft().currentScreen));
            }
        });
        final Setting pix = Setting.hiddenText("pix", N * N);

        Crosshair() {
            super("crosshair", "Crosshair", "A custom crosshair: classic cross or your own pixel drawing. Hides the vanilla one.", "Visual");
            settings.add(mode);
            settings.add(draw);
            settings.add(scale);
            settings.add(color);
            settings.add(size);
            settings.add(gap);
            settings.add(thick);
            settings.add(dot);
            settings.add(pix);
        }

        /** The drawing as palette indexes (N * N). Plus sign when nothing was drawn yet. */
        int[] cells() {
            int[] c = new int[N * N];
            String p = pix.s;
            if (p.length() != N * N) {
                int m = N / 2;
                for (int i = 2; i <= 5; i++) {
                    c[m * N + (m - i)] = 1;
                    c[m * N + (m + i)] = 1;
                    c[(m - i) * N + m] = 1;
                    c[(m + i) * N + m] = 1;
                }
                return c;
            }
            for (int i = 0; i < c.length; i++) {
                int v = Character.digit(p.charAt(i), 16);
                c[i] = v < 0 || v >= PAINT.length ? 0 : v;
            }
            return c;
        }

        void store(int[] c) {
            StringBuilder sb = new StringBuilder();
            for (int v : c) sb.append(Character.forDigit(v, 16));
            pix.s = sb.toString();
        }

        @Override
        public void renderOverlay(int sw, int sh) {
            if (mc.gameSettings.thirdPersonView != 0 || mc.currentScreen != null) return;
            int cx = sw / 2, cy = sh / 2;
            if (mode.idx == 1) {
                int[] c = cells();
                int sc = (int) scale.v, half = N / 2;
                for (int i = 0; i < c.length; i++) {
                    if (c[i] == 0) continue;
                    int x = cx + (i % N - half) * sc - sc / 2, y = cy + (i / N - half) * sc - sc / 2;
                    Gui.drawRect(x, y, x + sc, y + sc, 0xFF000000 | PAINT[c[i]]);
                }
                return;
            }
            int s = (int) size.v, g = (int) gap.v, t = (int) thick.v;
            int c = 0xFF000000 | color.rgb();
            int h = t / 2;
            Gui.drawRect(cx - g - s, cy - h, cx - g, cy - h + t, c);
            Gui.drawRect(cx + g, cy - h, cx + g + s, cy - h + t, c);
            Gui.drawRect(cx - h, cy - g - s, cx - h + t, cy - g, c);
            Gui.drawRect(cx - h, cy + g, cx - h + t, cy + g + s, c);
            if (dot.b) Gui.drawRect(cx - h, cy - h, cx - h + t, cy - h + t, c);
        }
    }

    static class LowHealth extends Module {
        final Setting limit = Setting.slider("health_limit", "Show below (HP)", 8.0F, 2.0F, 20.0F, 1.0F);
        final Setting color = Setting.color("color", "Colour", 1);

        LowHealth() {
            super("lowhealth", "Low Health", "Red border when your health is low.", "Visual");
            settings.add(limit);
            settings.add(color);
        }

        @Override
        public void renderOverlay(int sw, int sh) {
            EntityPlayerSP p = mc.thePlayer;
            if (p.getHealth() > limit.v || p.getHealth() <= 0.0F) return;
            float pulse = 0.65F + 0.35F * (float) Math.sin(System.currentTimeMillis() / 220.0D);
            int rgb = color.rgb();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            int steps = 10;
            for (int i = 0; i < steps; i++) {
                int a = (int) (110 * pulse * (1.0F - i / (float) steps));
                int c = (a << 24) | rgb;
                int w = 3 * i, o = 3;
                Gui.drawRect(w, w, sw - w, w + o, c);
                Gui.drawRect(w, sh - w - o, sw - w, sh - w, c);
                Gui.drawRect(w, w + o, w + o, sh - w - o, c);
                Gui.drawRect(sw - w - o, w + o, sw - w, sh - w - o, c);
            }
            GlStateManager.disableBlend();
        }
    }

    // ------------------------------------------------------------ HUD boxes

    static class TargetHud extends Module {
        EntityLivingBase target;
        long seen;

        TargetHud() {
            super("targethud", "Target HUD", "Health of the player you are aiming at.", "PvP");
            asHud(0.5F, 0.58F, 0);
            moved = true;
            background.b = true;
        }

        @Override
        public void onTick() {
            MovingObjectPosition o = mc.objectMouseOver;
            if (o != null && o.typeOfHit == MovingObjectPosition.MovingObjectType.ENTITY && o.entityHit instanceof EntityLivingBase) {
                target = (EntityLivingBase) o.entityHit;
                seen = System.currentTimeMillis();
            } else if (System.currentTimeMillis() - seen > 2500L || (target != null && target.isDead)) {
                target = null;
            }
        }

        @Override
        public void renderHud(int sw, int sh, boolean editing) {
            final EntityLivingBase t = target;
            if (t == null && !editing) return;
            final String name = t == null ? "Player" : t.getName();
            final float hp = t == null ? 14.0F : t.getHealth();
            final float max = t == null ? 20.0F : Math.max(1.0F, t.getMaxHealth());
            Hud.box(this, sw, sh, 110, 30, new Hud.Painter() {
                @Override
                public void paint(Module m, FontRenderer fr) {
                    Hud.text(m, fr, name, 4, 4);
                    String v = Q.f("%.1f HP", hp);
                    Hud.text(m, fr, v, 106 - fr.getStringWidth(v), 4);
                    Gui.drawRect(4, 17, 106, 23, 0x80000000);
                    float f = Math.max(0.0F, Math.min(1.0F, hp / max));
                    int col = f > 0.6F ? 0xFF55FF55 : (f > 0.3F ? 0xFFFFAA00 : 0xFFFF5555);
                    Gui.drawRect(4, 17, 4 + (int) (102 * f), 23, col);
                }
            });
        }
    }

    static class InventoryHud extends Module {
        InventoryHud() {
            super("inventory", "Inventory", "Shows your inventory on the HUD.", "Utility");
            asHud(0.5F, 0.8F, 0);
            moved = true;
            background.b = true;
        }

        @Override
        public void renderHud(int sw, int sh, boolean editing) {
            final EntityPlayerSP p = mc.thePlayer;
            Hud.box(this, sw, sh, 164, 56, new Hud.Painter() {
                @Override
                public void paint(Module m, FontRenderer fr) {
                    RenderHelper.enableGUIStandardItemLighting();
                    GlStateManager.enableRescaleNormal();
                    for (int i = 0; i < 27; i++) {
                        ItemStack s = p.inventory.mainInventory[9 + i];
                        if (s == null) continue;
                        int x = 2 + (i % 9) * 18, y = 2 + (i / 9) * 18;
                        mc.getRenderItem().renderItemAndEffectIntoGUI(s, x, y);
                        mc.getRenderItem().renderItemOverlayIntoGUI(fr, s, x, y, null);
                    }
                    GlStateManager.disableRescaleNormal();
                    RenderHelper.disableStandardItemLighting();
                }
            });
        }
    }
}
