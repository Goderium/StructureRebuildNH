package com.Goderium.StructureRebuildNH.loader;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.gtnewhorizon.gtnhmixins.IEarlyMixinLoader;
import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.Name("StructureRebuildNH")
@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.TransformerExclusions({"com.Goderium.StructureRebuildNH.loader."})
public class EarlyLoader implements IFMLLoadingPlugin, IEarlyMixinLoader {
    public String[] getASMTransformerClass() { return new String[0]; }
    public String getModContainerClass() { return null; }
    public String getSetupClass() { return null; }
    public void injectData(Map<String, Object> data) { }
    public String getAccessTransformerClass() { return null; }
    public String getMixinConfig() { return "mixins.structurerebuildnh.early.json"; }
    public List<String> getMixins(Set<String> mods) { return Collections.singletonList("WorldMixin"); }
}
