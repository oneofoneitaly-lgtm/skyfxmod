package dev.adryh01.yafabric.mods;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.input.Mouse;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.TreeMap;

/** All modules, their persistence and the per-tick driver. */
public final class Mods {

    public static final List<Module> ALL = new ArrayList<Module>();
    private static File file;
    private static boolean mouseWas, rightWas;
    private static int saveTimer;
    private static TreeMap<String, String> lastSaved;

    private Mods() {
    }

    public static void init() {
        file = new File(MinecraftClient.getInstance().runDirectory, "config/yafabric-mods.properties");
        ALL.clear();
        HudMods.register(ALL);
        Extra.register(ALL);
        ALL.add(new Boost());
        Pvp.register(ALL);
        Fx.register(ALL);
        Panels.register(ALL);
        ALL.add(new NickTag());
        Catalog.register(ALL);
        // sensible defaults for a first start
        for (Module m : ALL) {
            if (m.id.equals("fps") || m.id.equals("cps") || m.id.equals("coords")) m.enabled = true;
        }
        load();
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                save();
            }
        }, "YA-FABRIC-save"));
    }

    public static Module byId(String id) {
        for (Module m : ALL) {
            if (m.id.equals(id)) return m;
        }
        return null;
    }

    /** Called every client tick from the HUD mixin. */
    public static void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        boolean l = Mouse.isButtonDown(0), r = Mouse.isButtonDown(1);
        if (mc.currentScreen == null) {
            if (l && !mouseWas) HudMods.click(true);
            if (r && !rightWas) HudMods.click(false);
        }
        mouseWas = l;
        rightWas = r;
        Q.tick(mc.player);
        dev.adryh01.yafabric.YaNet.tick();
        for (Module m : ALL) {
            if (m.enabled) m.onTick();
        }
        if (++saveTimer >= 600) {
            saveTimer = 0;
            save();
        }
    }

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

    public static synchronized void save() {
        if (file == null) return;
        TreeMap<String, String> now = new TreeMap<String, String>();
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
            p.store(out, "YA CLIENT (Fabric) by @Adryh01");
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
}
