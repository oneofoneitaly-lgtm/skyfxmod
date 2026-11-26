package dev.adryh01.pvpfx;

import net.minecraft.client.settings.KeyBinding;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.lwjgl.input.Keyboard;

/**
 * PvPFX - a small PvP client menu (Right Shift) with HUD and gameplay modules. Client side only.
 * Made by @Adryh01.
 */
@Mod(modid = PvPFX.MODID, name = PvPFX.NAME, version = PvPFX.VERSION,
        acceptedMinecraftVersions = "[1.8.9]", clientSideOnly = true)
public class PvPFX {

    public static final String MODID = "pvpfx";
    public static final String NAME = "PvPFX";
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
        menuKey = new KeyBinding("Open PvPFX menu", Keyboard.KEY_RSHIFT, "PvPFX");
        zoomKey = new KeyBinding("Zoom", Keyboard.KEY_C, "PvPFX");
        ClientRegistry.registerKeyBinding(menuKey);
        ClientRegistry.registerKeyBinding(zoomKey);
        MinecraftForge.EVENT_BUS.register(new Events());
    }
}
