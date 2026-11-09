package com.Goderium.StructureRebuildNH;

import com.Goderium.StructureRebuildNH.config.RebuildConfig;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = "structurerebuildnh", name = "StructureRebuildNH", version = "0.1.0",
    dependencies = "required-after:gregtech;required-after:structurelib", acceptableRemoteVersions = "*")
public class StructureRebuildNH {
    public static final Logger LOG = LogManager.getLogger("StructureRebuildNH");
    @Mod.EventHandler public void preInit(FMLPreInitializationEvent event) {
        RebuildConfig.load(event.getSuggestedConfigurationFile());
    }
}
