package dev.adryh01.yamods;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.lwjgl.input.Keyboard;

/**
 * YA MODS - PvP client menu (Right Shift) with every YA Client module that exists in 1.8.9. Client side only.
 * Made by @Adryh01.
 */
@Mod(modid = YAMods.MODID, name = YAMods.NAME, version = YAMods.VERSION,
        acceptedMinecraftVersions = "[1.8.9]", clientSideOnly = true)
public class YAMods {

    public static final String MODID = "yamods";
    public static final String NAME = "YA MODS";
    public static final String VERSION = "1.0.0";
    public static final String AUTHOR = "@Adryh01";

    public static KeyBinding menuKey;
    public static KeyBinding zoomKey;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        Mods.init(e.getSuggestedConfigurationFile().getParentFile());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent e) {
        menuKey = new KeyBinding("Open YA MODS menu", Keyboard.KEY_RSHIFT, "YA MODS");
        zoomKey = new KeyBinding("Zoom", Keyboard.KEY_C, "YA MODS");
        ClientRegistry.registerKeyBinding(menuKey);
        ClientRegistry.registerKeyBinding(zoomKey);
        MinecraftForge.EVENT_BUS.register(new Events());
    }
}
