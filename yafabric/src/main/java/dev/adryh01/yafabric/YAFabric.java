package dev.adryh01.yafabric;

import dev.adryh01.yafabric.mods.Mods;
import net.fabricmc.api.ClientModInitializer;

/** YA CLIENT on Legacy Fabric 1.8.9. */
public class YAFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        try {
            Mods.init();
        } catch (RuntimeException e) {
            System.out.println("[YA CLIENT] could not start the modules: " + e);
        }
        System.out.println("[YA CLIENT] Fabric build loaded (" + Mods.ALL.size() + " modules)");
    }
}
