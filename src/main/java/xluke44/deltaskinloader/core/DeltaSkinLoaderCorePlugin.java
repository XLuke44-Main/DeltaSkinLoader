package xluke44.deltaskinloader.core;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

@IFMLLoadingPlugin.Name("DeltaSkinLoader")
@IFMLLoadingPlugin.MCVersion("1.12.2")
@IFMLLoadingPlugin.TransformerExclusions({"xluke44.deltaskinloader"})
public final class DeltaSkinLoaderCorePlugin implements IFMLLoadingPlugin {
    @Override
    public String[] getASMTransformerClass() {
        return new String[] {"xluke44.deltaskinloader.core.DeltaSkinLoaderTransformer"};
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
        
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
