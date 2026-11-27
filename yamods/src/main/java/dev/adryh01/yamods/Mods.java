package dev.adryh01.yamods;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.GuiVideoSettings;
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionEffect;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/** All modules, their persistence and the small shared state they use. */
public final class Mods {

    public static final List<Module> ALL = new ArrayList<Module>();
    private static File file;

    // shared state
    static final ArrayDeque<Long> LEFT = new ArrayDeque<Long>();
    static final ArrayDeque<Long> RIGHT = new ArrayDeque<Long>();
    static float lastReach = -1.0F;
    static final Zoom ZOOM = new Zoom();
    static final Freelook FREELOOK = new Freelook();
    static boolean rainbow;

    private Mods() {
    }

    public static void init(File configDir) {
        file = new File(configDir, "yamods.properties");
        ALL.clear();
        ALL.add(new Fps());
        ALL.add(new Cps());
        ALL.add(new Keystrokes());
        ALL.add(new Coords());
        ALL.add(new Armor());
        ALL.add(new Potions());
        ALL.add(new Ping());
        ALL.add(new Clock());
        ALL.add(new Reach());
        ALL.add(new ToggleSprint());
        ALL.add(ZOOM);
        ALL.add(FREELOOK);
        ALL.add(new ClientCape());
        ALL.add(new Boost());
        ALL.add(new Fullbright());
        ALL.add(new HideFire());
        Catalog.register(ALL);
        buildHooks();
        load();
        saveOnExit();
    }

    private static final java.util.HashMap<String, Module> BY_ID = new java.util.HashMap<String, Module>();
    /** Only the modules that really override a hook are called for it (no empty calls 114 times a frame). */
    static Module[] H_TICK = new Module[0], H_WORLD = H_TICK, H_BLOCK = H_TICK, H_CAMERA = H_TICK, H_HAND = H_TICK,
            H_CHAT = H_TICK, H_CLICK = H_TICK, H_ATTACK = H_TICK;

    private static Module[] hook(String name, Class<?>... args) {
        List<Module> out = new ArrayList<Module>();
        for (Module m : ALL) {
            try {
                if (m.getClass().getMethod(name, args).getDeclaringClass() != Module.class) out.add(m);
            } catch (Exception e) {
                out.add(m);
            }
        }
        return out.toArray(new Module[out.size()]);
    }

    static void buildHooks() {
        BY_ID.clear();
        for (Module m : ALL) BY_ID.put(m.id, m);
        H_TICK = hook("onTick");
        H_WORLD = hook("renderWorld", float.class);
        H_BLOCK = hook("drawBlock", net.minecraftforge.client.event.DrawBlockHighlightEvent.class);
        H_CAMERA = hook("camera", net.minecraftforge.client.event.EntityViewRenderEvent.CameraSetup.class);
        H_HAND = hook("renderHand", float.class);
        H_CHAT = hook("chat", net.minecraftforge.client.event.ClientChatReceivedEvent.class);
        H_CLICK = hook("leftClick");
        H_ATTACK = hook("attacked", net.minecraft.entity.Entity.class);
    }

    public static Module byId(String id) {
        return BY_ID.get(id);
    }

    // ------------------------------------------------------------ persistence

    public static void load() {
        if (file == null || !file.exists()) return;
        Properties p = new Properties();
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            p.load(in);
        } catch (IOException e) {
            return;
        } finally {
            try {
                if (in != null) in.close();
            } catch (IOException ignored) {
            }
        }
        for (Module m : ALL) {
            String en = p.getProperty(m.id + ".enabled");
            if (en != null) m.setEnabled(Boolean.parseBoolean(en));
            if (m.hud) {
                try {
                    String x = p.getProperty(m.id + ".x");
                    String y = p.getProperty(m.id + ".y");
                    if (x != null && y != null) {
                        m.xf = Math.max(0.0F, Math.min(1.0F, Float.parseFloat(x)));
                        m.yf = Math.max(0.0F, Math.min(1.0F, Float.parseFloat(y)));
                        m.moved = true;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            for (Setting s : m.settings) {
                String v = p.getProperty(m.id + "." + s.key);
                if (v != null) s.load(v);
            }
        }
    }

    private static java.util.Map<String, String> lastSaved;

    /** Writes the settings. Only touches the disk when something changed; the file is replaced in one step. */
    public static synchronized void save() {
        if (file == null) return;
        java.util.TreeMap<String, String> now = new java.util.TreeMap<String, String>();
        for (Module m : ALL) {
            now.put(m.id + ".enabled", String.valueOf(m.enabled));
            if (m.hud && m.moved) {
                now.put(m.id + ".x", String.valueOf(m.xf));
                now.put(m.id + ".y", String.valueOf(m.yf));
            }
            for (Setting s : m.settings) now.put(m.id + "." + s.key, s.save());
        }
        if (now.equals(lastSaved) && file.exists()) return;
        Properties p = new Properties();
        p.putAll(now);
        File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
        FileOutputStream out = null;
        try {
            file.getParentFile().mkdirs();
            out = new FileOutputStream(tmp);
            p.store(out, "YA MODS by @Adryh01");
            out.close();
            out = null;
            java.nio.file.Files.move(tmp.toPath(), file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            lastSaved = now;
        } catch (IOException ignored) {
        } finally {
            try {
                if (out != null) out.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static void saveOnExit() {
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                save();
            }
        }, "YA-MODS-save"));
    }

    // ------------------------------------------------------------ click tracking

    static void click(boolean left) {
        (left ? LEFT : RIGHT).addLast(System.currentTimeMillis());
        if (left) {
            net.minecraft.util.MovingObjectPosition o = Q.mc.objectMouseOver;
            if (o == null || o.typeOfHit != net.minecraft.util.MovingObjectPosition.MovingObjectType.BLOCK) Q.swing();
        }
    }

    static int cps(boolean left) {
        ArrayDeque<Long> q = left ? LEFT : RIGHT;
        long now = System.currentTimeMillis();
        while (!q.isEmpty() && now - q.peekFirst() > 1000L) q.pollFirst();
        return q.size();
    }

    // ================================================================== HUD modules

    static class Fps extends Module {
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
            int f = Minecraft.getDebugFPS();
            override = !colorize.b ? 0 : (f < warn.v ? 0xFF5555 : (f < warn.v * 1.6F ? 0xFFAA00 : 0x55FF55));
            Hud.lines(this, sw, sh, Collections.singletonList(label.b ? "FPS: " + f : String.valueOf(f)));
            override = 0;
        }
    }

    static class Cps extends Module {
        final Setting both = Setting.bool("both", "Show right click", true);

        Cps() {
            super("cps", "CPS", "Shows your clicks per second.", "PvP");
            settings.add(both);
            asHud(0.01F, 0.08F, 0);
        }

        @Override
        public void renderHud(int sw, int sh, boolean editing) {
            String t = "CPS: " + cps(true) + (both.b ? " | " + cps(false) : "");
            Hud.lines(this, sw, sh, Collections.singletonList(t));
        }
    }

    static class Keystrokes extends Module {
        static final int[] KEY_COLORS = {0x000000, 0x2E2E2E, 0xFFFFFF, 0xE21C30, 0x2F6BFF, 0x2ECC71, 0x8E44AD, 0xFF9F1C};
        static final String[] KEY_NAMES = {"Black", "Dark grey", "White", "Red", "Blue", "Green", "Purple", "Orange"};

        final Setting mouse = Setting.bool("mouse", "Show mouse buttons", true);
        final Setting cpsOn = Setting.bool("cps", "Show CPS on mouse buttons", true);
        final Setting space = Setting.bool("space", "Show space bar", true);
        final Setting extra = Setting.bool("extra", "Show sprint / sneak", false);
        final Setting arrows = Setting.choice("labels", "Movement labels", 0, "Letters", "Arrows");
        final Setting size = Setting.slider("size", "Key size", 20.0F, 14.0F, 32.0F, 1.0F);
        final Setting gap = Setting.slider("gap", "Space between keys", 2.0F, 0.0F, 6.0F, 1.0F);
        final Setting radius = Setting.slider("radius", "Rounded corners", 0.0F, 0.0F, 8.0F, 1.0F);
        final Setting idle = Setting.choice("idle", "Key colour", 0, KEY_NAMES);
        final Setting pressed = Setting.choice("pressed", "Pressed colour", 2, KEY_NAMES);
        final Setting idleA = Setting.slider("idlea", "Key opacity", 50.0F, 0.0F, 100.0F, 5.0F);
        final Setting pressedA = Setting.slider("pressa", "Pressed opacity", 80.0F, 0.0F, 100.0F, 5.0F);
        final Setting pressedText = Setting.choice("ptext", "Pressed text", 0, "Dark", "White", "Same");
        final Setting outline = Setting.bool("outline", "Outline", false);
        final Setting outlineColor = Setting.color("outlinecolor", "Outline colour", 0);
        final Setting shadow = Setting.bool("shadow", "Text shadow", false);
        final Setting fade = Setting.slider("fade", "Press animation speed", 35.0F, 10.0F, 100.0F, 5.0F);
        final float[] anim = new float[12];

        Keystrokes() {
            super("keystrokes", "Keystrokes", "WASD, mouse, space and CPS. Colours, size, corners and more.", "PvP");
            settings.add(mouse);
            settings.add(cpsOn);
            settings.add(space);
            settings.add(extra);
            settings.add(arrows);
            settings.add(size);
            settings.add(gap);
            settings.add(radius);
            settings.add(idle);
            settings.add(pressed);
            settings.add(idleA);
            settings.add(pressedA);
            settings.add(pressedText);
            settings.add(outline);
            settings.add(outlineColor);
            settings.add(shadow);
            settings.add(fade);
            asHud(0.01F, 0.45F, 0);
        }

        @Override
        public void renderHud(int sw, int sh, boolean editing) {
            final int s = (int) size.v, g = (int) gap.v;
            final int w = 3 * s + 2 * g;
            final int half = (w - g) / 2;
            final int spaceH = Math.max(10, s / 2), extraH = Math.max(12, (int) (s * 0.7F));
            final boolean showMouse = mouse.b, showSpace = space.b, showExtra = extra.b;
            int h = 2 * s + g;
            if (showMouse) h += g + s;
            if (showSpace) h += g + spaceH;
            if (showExtra) h += g + extraH;
            final boolean arr = arrows.idx == 1;
            Hud.box(this, sw, sh, w, h, (m, fr) -> {
                key(fr, 0, s + g, 0, s, s, arr ? "^" : "W", mc.gameSettings.keyBindForward.isKeyDown(), null);
                key(fr, 1, 0, s + g, s, s, arr ? "<" : "A", mc.gameSettings.keyBindLeft.isKeyDown(), null);
                key(fr, 2, s + g, s + g, s, s, arr ? "v" : "S", mc.gameSettings.keyBindBack.isKeyDown(), null);
                key(fr, 3, 2 * (s + g), s + g, s, s, arr ? ">" : "D", mc.gameSettings.keyBindRight.isKeyDown(), null);
                int y = 2 * s + 2 * g;
                if (showMouse) {
                    boolean c = cpsOn.b;
                    key(fr, 4, 0, y, half, s, "LMB", mc.gameSettings.keyBindAttack.isKeyDown(), c ? String.valueOf(cps(true)) : null);
                    key(fr, 5, half + g, y, w - half - g, s, "RMB", mc.gameSettings.keyBindUseItem.isKeyDown(), c ? String.valueOf(cps(false)) : null);
                    y += s + g;
                }
                if (showSpace) {
                    key(fr, 6, 0, y, w, spaceH, "", mc.gameSettings.keyBindJump.isKeyDown(), null);
                    Gui.drawRect(w / 2 - 12, y + spaceH / 2, w / 2 + 12, y + spaceH / 2 + 1, 0xFF000000 | color.rgb());
                    y += spaceH + g;
                }
                if (showExtra) {
                    key(fr, 7, 0, y, half, extraH, "Sprint", mc.gameSettings.keyBindSprint.isKeyDown(), null);
                    key(fr, 8, half + g, y, w - half - g, extraH, "Sneak", mc.gameSettings.keyBindSneak.isKeyDown(), null);
                }
            });
        }

        private void key(FontRenderer fr, int i, int x, int y, int w, int h, String label, boolean down, String sub) {
            anim[i] += ((down ? 1.0F : 0.0F) - anim[i]) * (fade.v / 100.0F);
            float t = anim[i];
            int rgb = lerp(KEY_COLORS[idle.idx], KEY_COLORS[pressed.idx], t);
            int a = (int) ((idleA.v + (pressedA.v - idleA.v) * t) * 2.55F);
            int r = (int) radius.v;
            if (outline.b) UI.roundRect(x, y, x + w, y + h, r, 0xFF000000 | outlineColor.rgb());
            int in = outline.b ? 1 : 0;
            UI.roundRect(x + in, y + in, x + w - in, y + h - in, Math.max(0, r - in), (a << 24) | rgb);
            if (label.length() == 0) return;
            int ptc = pressedText.idx == 0 ? 0x202020 : pressedText.idx == 1 ? 0xFFFFFF : color.rgb();
            int tc = 0xFF000000 | lerp(color.rgb(), ptc, t);
            int ty = sub == null ? y + (h - 8) / 2 : y + 2;
            draw(fr, label, x + (w - fr.getStringWidth(label)) / 2, ty, tc);
            if (sub != null) draw(fr, sub, x + (w - fr.getStringWidth(sub)) / 2, y + Math.min(h - 9, 11), tc);
        }

        private void draw(FontRenderer fr, String t, int x, int y, int c) {
            if (shadow.b) fr.drawStringWithShadow(t, x, y, c);
            else fr.drawString(t, x, y, c);
        }
    }

    static class Coords extends Module {
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
            EntityPlayerSP p = mc.thePlayer;
            if (p == null) return;
            List<String> l = new ArrayList<String>();
            if (layout.idx == 0) {
                l.add("XYZ: " + n(p.posX) + " " + n(p.posY) + " " + n(p.posZ));
            } else {
                l.add("X: " + n(p.posX));
                l.add("Y: " + n(p.posY));
                l.add("Z: " + n(p.posZ));
            }
            if (dir.b) {
                String f = p.getHorizontalFacing().getName();
                l.add("Facing: " + Character.toUpperCase(f.charAt(0)) + f.substring(1));
            }
            Hud.lines(this, sw, sh, l);
        }
    }

    static class Armor extends Module {
        final Setting dur = Setting.choice("dur", "Durability", 0, "Number", "Percent", "Off");
        final Setting held = Setting.bool("held", "Show held item", true);
        private ItemStack[] samples;

        Armor() {
            super("armor", "Armor Status", "Armor and held item with durability.", "PvP");
            settings.add(dur);
            settings.add(held);
            asHud(0.01F, 0.62F, 0);
        }

        @Override
        public void renderHud(int sw, int sh, boolean editing) {
            EntityPlayerSP p = mc.thePlayer;
            if (p == null) return;
            if (samples == null) {
                samples = new ItemStack[]{new ItemStack(Items.diamond_helmet), new ItemStack(Items.diamond_chestplate),
                        new ItemStack(Items.diamond_leggings), new ItemStack(Items.diamond_boots), new ItemStack(Items.diamond_sword)};
            }
            final List<ItemStack> items = new ArrayList<ItemStack>();
            for (int i = 3; i >= 0; i--) {
                ItemStack s = p.inventory.armorInventory[i];
                if (s == null && editing) s = samples[3 - i];
                if (s != null) items.add(s);
            }
            if (held.b) {
                ItemStack h = p.getHeldItem();
                if (h == null && editing) h = samples[4];
                if (h != null) items.add(h);
            }
            if (items.isEmpty()) return;
            Hud.box(this, sw, sh, 52, items.size() * 18, (m, fr) -> {
                RenderHelper.enableGUIStandardItemLighting();
                GlStateManager.enableRescaleNormal();
                for (int i = 0; i < items.size(); i++) {
                    ItemStack s = items.get(i);
                    int y = i * 18;
                    mc.getRenderItem().renderItemAndEffectIntoGUI(s, 0, y);
                    mc.getRenderItem().renderItemOverlayIntoGUI(fr, s, 0, y, null);
                    if (dur.idx != 2 && s.isItemStackDamageable()) {
                        int max = s.getMaxDamage();
                        int left = max - s.getItemDamage();
                        String t = dur.idx == 0 ? String.valueOf(left) : (left * 100 / Math.max(1, max)) + "%";
                        Hud.text(m, fr, t, 20, y + 5);
                    }
                }
                GlStateManager.disableRescaleNormal();
                RenderHelper.disableStandardItemLighting();
            });
        }
    }

    static class Potions extends Module {
        static final String[] ROMAN = {"", " II", " III", " IV", " V"};
        final Setting time = Setting.bool("time", "Show duration", true);

        Potions() {
            super("potions", "Potion Status", "Active effects and their time left.", "Info");
            settings.add(time);
            asHud(0.99F, 0.02F, 0);
        }

        @Override
        public void renderHud(int sw, int sh, boolean editing) {
            EntityPlayerSP p = mc.thePlayer;
            if (p == null) return;
            List<String> l = new ArrayList<String>();
            for (PotionEffect e : p.getActivePotionEffects()) {
                int amp = e.getAmplifier();
                String name = I18n.format(e.getEffectName()) + (amp < ROMAN.length ? ROMAN[amp] : " " + (amp + 1));
                l.add(time.b ? name + "  " + net.minecraft.potion.Potion.getDurationString(e) : name);
            }
            if (l.isEmpty() && editing) {
                l.add(time.b ? "Speed II  1:30" : "Speed II");
                l.add(time.b ? "Strength  0:45" : "Strength");
            }
            Hud.lines(this, sw, sh, l);
        }
    }

    static class Ping extends Module {
        Ping() {
            super("ping", "Ping", "Your latency to the server.", "Info");
            asHud(0.01F, 0.2F, 0);
        }

        @Override
        public void renderHud(int sw, int sh, boolean editing) {
            if (mc.thePlayer == null || mc.getNetHandler() == null) return;
            NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(mc.thePlayer.getUniqueID());
            int ms = info == null ? 0 : info.getResponseTime();
            Hud.lines(this, sw, sh, Collections.singletonList("Ping: " + ms + " ms"));
        }
    }

    static class Clock extends Module {
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

    static class Reach extends Module {
        final Setting dec = Setting.slider("dec", "Decimals", 2, 1, 3, 1);

        Reach() {
            super("reach", "Reach", "Distance of your last hit.", "PvP");
            settings.add(dec);
            asHud(0.01F, 0.26F, 0);
        }

        @Override
        public void renderHud(int sw, int sh, boolean editing) {
            String v = lastReach < 0 ? "-" : String.format(Locale.ROOT, "%." + (int) dec.v + "f", lastReach);
            Hud.lines(this, sw, sh, Collections.singletonList("Reach: " + v));
        }
    }

    static class ToggleSprint extends Module {
        final Setting status = Setting.bool("status", "Show status", true);

        ToggleSprint() {
            super("togglesprint", "Toggle Sprint", "Sprints automatically while you walk forward.", "Utility");
            settings.add(status);
            asHud(0.01F, 0.32F, 0);
        }

        @Override
        public void onTick() {
            EntityPlayerSP p = mc.thePlayer;
            if (p == null || mc.currentScreen != null) return;
            if (mc.gameSettings.keyBindForward.isKeyDown() && !p.isSneaking() && !p.isUsingItem()
                    && p.getFoodStats().getFoodLevel() > 6 && !p.isCollidedHorizontally) {
                p.setSprinting(true);
            }
        }

        @Override
        public void renderHud(int sw, int sh, boolean editing) {
            if (!status.b || mc.thePlayer == null) return;
            String t = mc.thePlayer.isSprinting() ? "[Sprinting (Toggled)]" : "[Walking]";
            Hud.lines(this, sw, sh, Collections.singletonList(t));
        }
    }

    // ================================================================== gameplay / visual modules

    static class Zoom extends Module {
        final Setting amount = Setting.slider("amount", "Zoom amount", 4.0F, 1.5F, 10.0F, 0.5F);
        final Setting smooth = Setting.bool("smooth", "Smooth camera", true);
        final Setting scroll = Setting.bool("scroll", "Scroll to adjust", true);
        float anim;
        boolean zooming, changedSmooth;
        boolean prevSmooth;

        Zoom() {
            super("zoom", "Zoom", "Hold the Zoom key (C by default) to zoom in.", "Utility");
            settings.add(amount);
            settings.add(smooth);
            settings.add(scroll);
        }

        float fovMultiplier() {
            return 1.0F - anim * (1.0F - 1.0F / amount.v);
        }

        @Override
        public void onTick() {
            boolean down = YAMods.zoomKey != null && YAMods.zoomKey.isKeyDown() && mc.currentScreen == null;
            zooming = down;
            anim += ((down ? 1.0F : 0.0F) - anim) * 0.35F;
            if (anim < 0.005F) anim = 0.0F;
            if (smooth.b) {
                if (down && !changedSmooth) {
                    prevSmooth = mc.gameSettings.smoothCamera;
                    mc.gameSettings.smoothCamera = true;
                    changedSmooth = true;
                } else if (!down && changedSmooth) {
                    mc.gameSettings.smoothCamera = prevSmooth;
                    changedSmooth = false;
                }
            }
        }

        @Override
        public void onDisable() {
            anim = 0.0F;
            zooming = false;
            if (changedSmooth) {
                mc.gameSettings.smoothCamera = prevSmooth;
                changedSmooth = false;
            }
        }
    }

    /** Hold a key (Left Alt) and move the mouse to look around while your character keeps facing the same way. */
    static class Freelook extends Module {
        final Setting third = Setting.bool("third", "Third person while looking", true);
        final Setting invert = Setting.bool("invert", "Invert vertical look", false);
        float camYaw, camPitch, startPitch, realYaw, realPitch, realPrevYaw, realPrevPitch;
        boolean active, swapped, changedView;
        int prevView;

        Freelook() {
            super("freelook", "Freelook", "Hold the Freelook key (Left Alt) to look around without turning your character.", "Utility");
            settings.add(third);
            settings.add(invert);
        }

        @Override
        public void onTick() {
            EntityPlayerSP p = mc.thePlayer;
            boolean down = p != null && mc.currentScreen == null && YAMods.freelookKey != null && YAMods.freelookKey.isKeyDown();
            if (down && !active) {
                active = true;
                camYaw = p.rotationYaw;
                camPitch = p.rotationPitch;
                prevView = mc.gameSettings.thirdPersonView;
                if (third.b && prevView == 0) {
                    mc.gameSettings.thirdPersonView = 1;
                    changedView = true;
                }
            } else if (!down && active) {
                stop();
            }
        }

        private void stop() {
            active = false;
            if (changedView) {
                mc.gameSettings.thirdPersonView = prevView;
                changedView = false;
            }
        }

        @Override
        public void onDisable() {
            stop();
        }

        /** Before the frame: the camera reads the player's rotation, so give it the free-look angles. */
        @Override
        public void renderTickStart() {
            EntityPlayerSP p = mc.thePlayer;
            if (!active || p == null || swapped) return;
            realYaw = p.rotationYaw;
            realPitch = p.rotationPitch;
            realPrevYaw = p.prevRotationYaw;
            realPrevPitch = p.prevRotationPitch;
            p.rotationYaw = camYaw;
            p.prevRotationYaw = camYaw;
            p.rotationPitch = camPitch;
            p.prevRotationPitch = camPitch;
            startPitch = camPitch;
            swapped = true;
        }

        /** After the frame: keep the mouse movement as camera movement and give the player back its real rotation. */
        @Override
        public void renderTickEnd() {
            EntityPlayerSP p = mc.thePlayer;
            if (!swapped || p == null) {
                swapped = false;
                return;
            }
            swapped = false;
            camYaw = p.rotationYaw;
            float dp = p.rotationPitch - startPitch;
            camPitch = startPitch + (invert.b ? -dp : dp);
            if (camPitch > 90.0F) camPitch = 90.0F;
            if (camPitch < -90.0F) camPitch = -90.0F;
            p.rotationYaw = realYaw;
            p.rotationPitch = realPitch;
            p.prevRotationYaw = realPrevYaw;
            p.prevRotationPitch = realPrevPitch;
        }
    }

    static class Fullbright extends Module {
        final Setting level = Setting.slider("level", "Brightness", 15.0F, 2.0F, 15.0F, 1.0F);
        float original = 1.0F;
        boolean applied;

        Fullbright() {
            super("fullbright", "Fullbright", "Everything is fully lit, even in caves.", "Visual");
            settings.add(level);
        }

        @Override
        public void onEnable() {
            float g = mc.gameSettings.gammaSetting;
            original = g > 1.0F ? 1.0F : g;
        }

        @Override
        public void onDisable() {
            mc.gameSettings.gammaSetting = original;
            applied = false;
        }

        @Override
        public void onTick() {
            // keep the saved options.txt clean: use the normal gamma while the video/options screens are open
            if (mc.currentScreen instanceof GuiVideoSettings || mc.currentScreen instanceof GuiOptions) {
                if (applied) {
                    mc.gameSettings.gammaSetting = original;
                    applied = false;
                }
                return;
            }
            mc.gameSettings.gammaSetting = level.v;
            applied = true;
        }
    }

    static class HideFire extends Module {
        HideFire() {
            super("nofire", "Hide Fire", "Hides the fire overlay on your screen.", "Visual");
        }
    }

    private static int lerp(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) + ((((b >> 16) & 255) - ((a >> 16) & 255)) * t));
        int g = (int) (((a >> 8) & 255) + ((((b >> 8) & 255) - ((a >> 8) & 255)) * t));
        int bl = (int) ((a & 255) + (((b & 255) - (a & 255)) * t));
        return (r << 16) | (g << 8) | bl;
    }
}
