package dev.adryh01.yafabric.mods;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.ClientPlayerEntity;
import net.minecraft.item.ItemStack;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/** The classic HUD modules: FPS, CPS, coordinates, ping, clock, potions, armor, keystrokes. */
final class HudMods {

    static final ArrayDeque<Long> LEFT = new ArrayDeque<Long>(), RIGHT = new ArrayDeque<Long>();

    private HudMods() {
    }

    static int cps(boolean left) {
        ArrayDeque<Long> q = left ? LEFT : RIGHT;
        long now = System.currentTimeMillis();
        while (!q.isEmpty() && now - q.peekFirst() > 1000L) q.pollFirst();
        return q.size();
    }

    static void click(boolean left) {
        (left ? LEFT : RIGHT).addLast(System.currentTimeMillis());
    }

    static void register(List<Module> all) {
        all.add(new Fps());
        all.add(new Cps());
        all.add(new Keystrokes());
        all.add(new Coords());
        all.add(new Armor());
        all.add(new Potions());
        all.add(new Ping());
        all.add(new Clock());
        all.add(new Fullbright());
        all.add(new ClientCape());
    }

    static final class Fps extends Module {
        final Setting label = Setting.bool("label", "Show label", true);
        final Setting colorize = Setting.bool("colorize", "Colour by value", false);
        final Setting warn = Setting.slider("warn", "Red below", 60.0F, 20.0F, 144.0F, 5.0F);

        Fps() {
            super("fps", "FPS", "Shows your frames per second.", "Info");
            settings.add(label);
            settings.add(colorize);
            settings.add(warn);
            asHud(0.01F, 0.02F, 0);
        }

        @Override
        public void renderHud(int sw, int sh, boolean editing) {
            int f = MinecraftClient.getCurrentFps();
            override = !colorize.b ? 0 : (f < warn.v ? 0xFF5555 : (f < warn.v * 1.6F ? 0xFFAA00 : 0x55FF55));
            Hud.lines(this, sw, sh, Collections.singletonList(label.b ? "FPS: " + f : String.valueOf(f)));
            override = 0;
        }
    }

    static final class Cps extends Module {
        final Setting both = Setting.bool("both", "Show right click", true);

        Cps() {
            super("cps", "CPS", "Shows your clicks per second.", "PvP");
            settings.add(both);
            asHud(0.01F, 0.08F, 0);
        }

        @Override
        public void renderHud(int sw, int sh, boolean editing) {
            Hud.lines(this, sw, sh, Collections.singletonList("CPS: " + cps(true) + (both.b ? " | " + cps(false) : "")));
        }
    }

    static final class Coords extends Module {
        final Setting layout = Setting.choice("layout", "Layout", 0, "Compact", "Lines");
        final Setting dir = Setting.bool("dir", "Show direction", true);
        final Setting dec = Setting.slider("dec", "Decimals", 0, 0, 2, 1);

        Coords() {
            super("coords", "Coordinates", "Your X, Y, Z and facing direction.", "Info");
            settings.add(layout);
            settings.add(dir);
            settings.add(dec);
            asHud(0.01F, 0.14F, 0);
        }

        private String n(double v) {
            int d = (int) dec.v;
            return d == 0 ? String.valueOf((int) Math.floor(v)) : String.format(Locale.ROOT, "%." + d + "f", v);
        }

        @Override
        public void renderHud(int sw, int sh, boolean editing) {
            ClientPlayerEntity p = mc.player;
            if (p == null) return;
            List<String> l = new ArrayList<String>();
            if (layout.idx == 0) {
                l.add("XYZ: " + n(p.x) + " " + n(p.y) + " " + n(p.z));
            } else {
                l.add("X: " + n(p.x));
                l.add("Y: " + n(p.y));
                l.add("Z: " + n(p.z));
            }
            if (dir.b) l.add("Facing: " + Q.compass(p.yaw));
            Hud.lines(this, sw, sh, l);
        }
    }

    static final class Ping extends Module {
        Ping() {
            super("ping", "Ping", "Your latency to the server.", "Info");
            asHud(0.01F, 0.2F, 0);
        }

        @Override
        public void renderHud(int sw, int sh, boolean editing) {
            Hud.lines(this, sw, sh, Collections.singletonList("Ping: " + Q.ping() + " ms"));
        }
    }

    static final class Clock extends Module {
        final Setting h24 = Setting.bool("h24", "24-hour clock", true);
        final Setting sec = Setting.bool("sec", "Show seconds", false);

        Clock() {
            super("clock", "Clock", "The real time.", "Info");
            settings.add(h24);
            settings.add(sec);
            asHud(0.99F, 0.92F, 0);
        }

        @Override
        public void renderHud(int sw, int sh, boolean editing) {
            String f = h24.b ? (sec.b ? "HH:mm:ss" : "HH:mm") : (sec.b ? "hh:mm:ss a" : "hh:mm a");
            Hud.lines(this, sw, sh, Collections.singletonList(new SimpleDateFormat(f, Locale.ROOT).format(new Date())));
        }
    }

    static final class Potions extends Module {
        static final String[] ROMAN = {"", " II", " III", " IV", " V"};
        final Setting time = Setting.bool("time", "Show duration", true);

        Potions() {
            super("potions", "Potion Status", "Active effects and their time left.", "Info");
            settings.add(time);
            asHud(0.99F, 0.02F, 0);
        }

        @Override
        public void renderHud(int sw, int sh, boolean editing) {
            ClientPlayerEntity p = mc.player;
            if (p == null) return;
            List<String> l = new ArrayList<String>();
            for (StatusEffectInstance e : p.getStatusEffectInstances()) {
                int amp = e.getAmplifier();
                StatusEffect se = StatusEffect.STATUS_EFFECTS[e.getEffectId()];
                String name = (se == null ? "Effect" : I18n.translate(se.getTranslationKey())) + (amp < ROMAN.length ? ROMAN[amp] : " " + (amp + 1));
                int secs = e.getDuration() / 20;
                l.add(time.b ? name + "  " + (secs / 60) + ":" + (secs % 60 < 10 ? "0" : "") + (secs % 60) : name);
            }
            if (l.isEmpty() && editing) {
                l.add(time.b ? "Speed II  1:30" : "Speed II");
                l.add(time.b ? "Strength  0:45" : "Strength");
            }
            Hud.lines(this, sw, sh, l);
        }
    }

    static final class Armor extends Module {
        final Setting dur = Setting.choice("dur", "Durability", 0, "Number", "Percent", "Off");
        final Setting held = Setting.bool("held", "Show held item", true);

        Armor() {
            super("armor", "Armor Status", "Armor and held item with durability.", "PvP");
            settings.add(dur);
            settings.add(held);
            asHud(0.01F, 0.62F, 0);
        }

        @Override
        public void renderHud(int sw, int sh, boolean editing) {
            ClientPlayerEntity p = mc.player;
            if (p == null) return;
            final List<ItemStack> items = new ArrayList<ItemStack>();
            for (int i = 3; i >= 0; i--) {
                ItemStack s = p.inventory.armor[i];
                if (s != null) items.add(s);
            }
            if (held.b) {
                ItemStack h = p.getMainHandStack();
                if (h != null) items.add(h);
            }
            if (items.isEmpty()) return;
            Hud.box(this, sw, sh, 52, items.size() * 18, (m, fr) -> {
                DiffuseLighting.enable();
                for (int i = 0; i < items.size(); i++) {
                    ItemStack s = items.get(i);
                    int y = i * 18;
                    mc.getItemRenderer().renderInGuiWithOverrides(s, 0, y);
                    mc.getItemRenderer().renderGuiItemOverlay(fr, s, 0, y);
                    if (dur.idx != 2 && s.isDamageable()) {
                        int max = s.getMaxDamage();
                        int left = max - s.getDamage();
                        String t = dur.idx == 0 ? String.valueOf(left) : (left * 100 / Math.max(1, max)) + "%";
                        Hud.text(m, fr, t, 20, y + 5);
                    }
                }
                DiffuseLighting.disable();
            });
        }
    }

    static final class Keystrokes extends Module {
        static final int[] KEY_COLORS = {0x000000, 0x2E2E2E, 0xFFFFFF, 0xE21C30, 0x2F6BFF, 0x2ECC71, 0x8E44AD, 0xFF9F1C};
        static final String[] KEY_NAMES = {"Black", "Dark grey", "White", "Red", "Blue", "Green", "Purple", "Orange"};
        final Setting mouse = Setting.bool("mouse", "Show mouse buttons", true);
        final Setting cpsOn = Setting.bool("cps", "Show CPS on mouse buttons", true);
        final Setting space = Setting.bool("space", "Show space bar", true);
        final Setting size = Setting.slider("size", "Key size", 20.0F, 14.0F, 32.0F, 1.0F);
        final Setting gap = Setting.slider("gap", "Space between keys", 2.0F, 0.0F, 6.0F, 1.0F);
        final Setting radius = Setting.slider("radius", "Rounded corners", 0.0F, 0.0F, 8.0F, 1.0F);
        final Setting idle = Setting.choice("idle", "Key colour", 0, KEY_NAMES);
        final Setting pressed = Setting.choice("pressed", "Pressed colour", 2, KEY_NAMES);
        final Setting idleA = Setting.slider("idlea", "Key opacity", 50.0F, 0.0F, 100.0F, 5.0F);
        final Setting pressedA = Setting.slider("pressa", "Pressed opacity", 80.0F, 0.0F, 100.0F, 5.0F);
        final float[] anim = new float[12];

        Keystrokes() {
            super("keystrokes", "Keystrokes", "WASD, mouse, space and CPS. Colours, size and corners.", "PvP");
            settings.add(mouse);
            settings.add(cpsOn);
            settings.add(space);
            settings.add(size);
            settings.add(gap);
            settings.add(radius);
            settings.add(idle);
            settings.add(pressed);
            settings.add(idleA);
            settings.add(pressedA);
            asHud(0.01F, 0.45F, 0);
        }

        @Override
        public void renderHud(int sw, int sh, boolean editing) {
            final int s = (int) size.v, g = (int) gap.v;
            final int w = 3 * s + 2 * g;
            final int half = (w - g) / 2;
            final int spaceH = Math.max(10, s / 2);
            final boolean showMouse = mouse.b, showSpace = space.b;
            int h = 2 * s + g;
            if (showMouse) h += g + s;
            if (showSpace) h += g + spaceH;
            Hud.box(this, sw, sh, w, h, (m, fr) -> {
                key(fr, 0, s + g, 0, s, s, "W", mc.options.forwardKey.isPressed(), null);
                key(fr, 1, 0, s + g, s, s, "A", mc.options.leftKey.isPressed(), null);
                key(fr, 2, s + g, s + g, s, s, "S", mc.options.backKey.isPressed(), null);
                key(fr, 3, 2 * (s + g), s + g, s, s, "D", mc.options.rightKey.isPressed(), null);
                int y = 2 * s + 2 * g;
                if (showMouse) {
                    boolean c = cpsOn.b;
                    key(fr, 4, 0, y, half, s, "LMB", mc.options.attackKey.isPressed(), c ? String.valueOf(cps(true)) : null);
                    key(fr, 5, half + g, y, w - half - g, s, "RMB", mc.options.useKey.isPressed(), c ? String.valueOf(cps(false)) : null);
                    y += s + g;
                }
                if (showSpace) {
                    key(fr, 6, 0, y, w, spaceH, "", mc.options.jumpKey.isPressed(), null);
                    DrawableHelper.fill(w / 2 - 12, y + spaceH / 2, w / 2 + 12, y + spaceH / 2 + 1, 0xFF000000 | color.rgb());
                }
            });
        }

        private void key(TextRenderer fr, int i, int x, int y, int w, int h, String label, boolean down, String sub) {
            anim[i] += ((down ? 1.0F : 0.0F) - anim[i]) * 0.35F;
            float t = anim[i];
            int rgb = UI.lerp(KEY_COLORS[idle.idx], KEY_COLORS[pressed.idx], t) & 0xFFFFFF;
            int a = (int) ((idleA.v + (pressedA.v - idleA.v) * t) * 2.55F);
            UI.roundRect(x, y, x + w, y + h, (int) radius.v, (a << 24) | rgb);
            if (label.length() == 0) return;
            int tc = 0xFF000000 | UI.lerp(color.rgb(), 0x202020, t);
            int ty = sub == null ? y + (h - 8) / 2 : y + 2;
            fr.drawWithShadow(label, x + (w - fr.getStringWidth(label)) / 2, ty, tc);
            if (sub != null) fr.drawWithShadow(sub, x + (w - fr.getStringWidth(sub)) / 2, y + Math.min(h - 9, 11), tc);
        }
    }

    static final class Fullbright extends Module {
        private float saved = 1.0F;

        Fullbright() {
            super("fullbright", "Fullbright", "See in the dark.", "Visual");
        }

        @Override
        public void onEnable() {
            saved = mc.options.gamma;
        }

        @Override
        public void onTick() {
            mc.options.gamma = 16.0F;
        }

        @Override
        public void onDisable() {
            mc.options.gamma = Math.min(saved, 1.0F);
        }
    }

    /** The free YA Cape (the mixin reads the two switches). */
    static final class ClientCape extends Module {
        ClientCape() {
            super("clientcape", "YA Cape", "The free YA CLIENT cape (claim it on the store).", "Visual");
            settings.add(Setting.bool("mine", "Show my cape", true));
            settings.add(Setting.bool("others", "Show others' cape", true));
            enabled = true;
        }
    }
}
