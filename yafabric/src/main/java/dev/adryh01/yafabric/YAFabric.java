package dev.adryh01.yafabric;

import net.fabricmc.api.ClientModInitializer;

/** YA CLIENT on Legacy Fabric 1.8.9 (stage 1: the YA home screen). */
public class YAFabric implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        System.out.println("[YA CLIENT] Fabric build loaded");
    }
}
