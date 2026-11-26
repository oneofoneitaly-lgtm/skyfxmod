package dev.adryh01.yamenu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiDownloadTerrain;
import net.minecraft.client.gui.GuiIngameMenu;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiOptions;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSelectWorld;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** YA MENU by @Adryh01: title screen, singleplayer and multiplayer in the YA CLIENT style. */
@Mod(modid = YAMenu.MODID, name = "YA MENU", version = "${version}", acceptedMinecraftVersions = "[1.8.9]", clientSideOnly = true)
public class YAMenu {

    public static final String MODID = "yamenu";

    /**
     * Forge's start-up splash is drawn before any mod loads, so it can only be restyled through
     * config/splash.properties. We switch it off (no Forge loading bars) so only the vanilla splash is left,
     * whose logo is replaced by assets/minecraft/textures/gui/title/mojang.png. It applies from the next start.
     */
    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        try {
            java.io.File f = new java.io.File(e.getModConfigurationDirectory(), "splash.properties");
            java.util.Properties p = new java.util.Properties();
            if (f.exists()) {
                java.io.FileInputStream in = new java.io.FileInputStream(f);
                p.load(in);
                in.close();
            }
            String[][] want = {{"enabled", "false"}, {"background", "0xFFFFFF"}, {"font", "0x0"}, {"barBackground", "0xFFFFFF"},
                {"barBorder", "0x000000"}, {"bar", "0x000000"}, {"showMemory", "false"}, {"rotate", "false"}};
            boolean changed = false;
            for (String[] kv : want) {
                if (!kv[1].equals(p.getProperty(kv[0]))) {
                    p.setProperty(kv[0], kv[1]);
                    changed = true;
                }
            }
            if (changed) {
                java.io.FileOutputStream out = new java.io.FileOutputStream(f);
                p.store(out, "Splash screen properties (YA MENU)");
                out.close();
            }
        } catch (Exception ignored) {
        }
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        MinecraftForge.EVENT_BUS.register(this);
        org.lwjgl.opengl.Display.setTitle("YA CLIENT");
    }

    private static GuiScreen parent() {
        GuiScreen cur = Minecraft.getMinecraft().currentScreen;
        return cur instanceof YAScreen ? cur : new YAMainMenu();
    }

    @SubscribeEvent
    public void onGui(GuiOpenEvent e) {
        if (e.gui == null) return;
        Class<?> c = e.gui.getClass();
        if (c == GuiMainMenu.class) e.gui = new YAMainMenu();
        else if (c == GuiIngameMenu.class) e.gui = new YAPause();
        else if (c == GuiOptions.class) e.gui = new YAOptions(Minecraft.getMinecraft().currentScreen);
        else if (c == GuiSelectWorld.class) e.gui = new YASingleplayer(parent());
        else if (c == GuiMultiplayer.class) e.gui = new YAMultiplayer(parent());
    }

    /** "Downloading terrain": only the logo and a black bar. */
    @SubscribeEvent
    public void onDraw(GuiScreenEvent.DrawScreenEvent.Pre e) {
        if (!(e.gui instanceof GuiDownloadTerrain)) return;
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution sr = new ScaledResolution(mc);
        int w = sr.getScaledWidth(), h = sr.getScaledHeight();
        Gui.drawRect(0, 0, w, h, 0xFFFFFFFF);
        UI.logo(w / 2 - 40, h / 2 - 70, 80);
        int bw = 160, x = w / 2 - bw / 2, y = h / 2 + 30;
        Gui.drawRect(x - 1, y - 1, x + bw + 1, y + 7, 0xFF111111);
        Gui.drawRect(x, y, x + bw, y + 6, 0xFFFFFFFF);
        float t = (System.currentTimeMillis() % 1400L) / 1400.0F;
        int seg = 56, px = x + (int) ((bw + seg) * t) - seg;
        Gui.drawRect(Math.max(x, px), y, Math.min(x + bw, px + seg), y + 6, 0xFF111111);
        e.setCanceled(true);
    }
}
