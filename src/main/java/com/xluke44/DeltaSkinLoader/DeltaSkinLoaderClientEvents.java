package com.xluke44.DeltaSkinLoader;

import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.loading.FMLPaths;

@Mod.EventBusSubscriber(
        modid = DeltaSkinLoader.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class DeltaSkinLoaderClientEvents {

    private DeltaSkinLoaderClientEvents() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            DeltaSkinLoaderRuntime.initialize(FMLPaths.CONFIGDIR.get());
            DeltaSkinLoaderRuntime.registerWorldLifecycleListener();
        });
    }

    @SubscribeEvent
    public static void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener)
                resourceManager -> DeltaSkinLoaderRuntime.reloadActiveTextures());
    }
}
