package dev.adryh01.skyfx;

import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.world.WorldProvider;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.IRenderHandler;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import org.lwjgl.input.Keyboard;

/**
 * SkyFX - custom skies (including void skies) with a clean menu. Client side only.
 * Made by @Adryh01.
 */
@Mod(modid = SkyFX.MODID, name = SkyFX.NAME, version = SkyFX.VERSION,
        acceptedMinecraftVersions = "[1.8.9]", clientSideOnly = true)
public class SkyFX {

    public static final String MODID = "skyfx";
    public static final String NAME = "SkyFX";
    public static final String VERSION = "1.0.0";
    public static final String AUTHOR = "@Adryh01";

    public static Sky current;
    public static boolean animations = true;
    public static boolean fogTint = true;
    public static boolean openMenu = false;

    private static Configuration cfg;
    private static KeyBinding key;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        cfg = new Configuration(e.getSuggestedConfigurationFile());
        Sky.load();
        String id = cfg.getString("sky", "general", "vanilla", "Selected sky id");
        animations = cfg.getBoolean("animations", "general", true, "Animated stars / aurora / twinkle");
        fogTint = cfg.getBoolean("fogTint", "general", true, "Tint the fog with the sky colour");
        current = Sky.byId(id);
        if (current == null) current = Sky.ALL.get(0);
        if (cfg.hasChanged()) cfg.save();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        MinecraftForge.EVENT_BUS.register(this);
        ClientCommandHandler.instance.registerCommand(new SkyCommand());
        key = new KeyBinding("Open SkyFX menu", Keyboard.KEY_K, "SkyFX");
        ClientRegistry.registerKeyBinding(key);
    }

    public static void setSky(Sky s) {
        current = s;
        save();
    }

    public static void save() {
        if (cfg == null) return;
        cfg.get("general", "sky", "vanilla").set(current == null ? "vanilla" : current.id);
        cfg.get("general", "animations", true).set(animations);
        cfg.get("general", "fogTint", true).set(fogTint);
        cfg.save();
    }

    // ---------------------------------------------------------------- events

    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getMinecraft();

        if (key != null && key.isPressed() && mc.currentScreen == null && mc.thePlayer != null) {
            openMenu = true;
        }
        // the command is sent from the chat screen, so we open the menu one tick later
        if (openMenu && mc.currentScreen == null) {
            openMenu = false;
            mc.displayGuiScreen(new SkyMenu());
        }
        if (mc.theWorld != null) apply(mc.theWorld.provider);
    }

    private static void apply(WorldProvider p) {
        boolean on = current != null && !current.vanilla;
        IRenderHandler sky = p.getSkyRenderer();
        IRenderHandler cloud = p.getCloudRenderer();
        if (on) {
            if (sky != SkyRenderer.INSTANCE) p.setSkyRenderer(SkyRenderer.INSTANCE);
            if (!current.clouds) {
                if (cloud != SkyRenderer.NO_CLOUDS) p.setCloudRenderer(SkyRenderer.NO_CLOUDS);
            } else if (cloud == SkyRenderer.NO_CLOUDS) {
                p.setCloudRenderer(null);
            }
        } else {
            if (sky == SkyRenderer.INSTANCE) p.setSkyRenderer(null);
            if (cloud == SkyRenderer.NO_CLOUDS) p.setCloudRenderer(null);
        }
    }

    @SubscribeEvent
    public void onFogColor(EntityViewRenderEvent.FogColors e) {
        Sky s = current;
        if (s == null || s.vanilla || !fogTint) return;
        Entity v = Minecraft.getMinecraft().getRenderViewEntity();
        if (v == null || v.isInsideOfMaterial(Material.water) || v.isInsideOfMaterial(Material.lava)) return;
        int fg = s.fogAt(s.blend(Sky.timeSec()));
        e.red = ((fg >> 16) & 255) / 255.0F;
        e.green = ((fg >> 8) & 255) / 255.0F;
        e.blue = (fg & 255) / 255.0F;
    }
}
