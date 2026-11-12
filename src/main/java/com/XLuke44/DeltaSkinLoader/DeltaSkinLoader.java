package com.XLuke44.DeltaSkinLoader;

import net.fabricmc.api.ClientModInitializer;

public final class DeltaSkinLoader implements ClientModInitializer {
    public static final String MODID = "deltaskinloader";
    public static final String NAME = "DeltaSkinLoader";
    public static final String VERSION = "1.0.0";

    @Override
    public void onInitializeClient() {
        DeltaSkinLoaderRuntime.initialize();
        DeltaSkinLoaderRuntime.registerReloadListener();
        DeltaSkinLoaderRuntime.registerWorldLifecycleListener();
    }
}
