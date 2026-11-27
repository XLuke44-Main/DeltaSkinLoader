package xluke44.deltaskinloader;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
@Mod(
    modid = "deltaskinloader",
    name = "DeltaSkinLoader",
    version = "1.0.2",
    acceptedMinecraftVersions = "[1.12.2]",
    dependencies = "required-after:forge@[14.23.5.2859,);",
    clientSideOnly = true
)
public final class DeltaSkinLoader {

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        DeltaSkinLoaderRuntime.initialize(event.getModConfigurationDirectory().toPath());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        DeltaSkinLoaderRuntime.registerReloadListener();
        DeltaSkinLoaderRuntime.registerWorldLifecycleListener();
    }
}
