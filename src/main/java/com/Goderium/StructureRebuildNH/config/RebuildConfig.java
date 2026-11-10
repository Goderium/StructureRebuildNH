package com.Goderium.StructureRebuildNH.config;

import java.io.File;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import net.minecraftforge.common.config.Configuration;

public final class RebuildConfig {
    public static boolean enabled = true, protectTileEntities = false;
    public static int maxVolume = 4000000;
    public static boolean writeDebugLogs = true, logBlockDecisions = true;
    public static File logDirectory;
    public static Set<String> protectedMods = new HashSet<String>();
    public static Set<String> protectedBlocks = new HashSet<String>();
    public static Set<String> excludedControllers = new HashSet<String>();
    public static void load(File file) {
        Configuration cfg = new Configuration(file);
        cfg.load();
        enabled = cfg.getBoolean("enabled", "general", true,
            "Allow creative sneaking blueprint rebuild ONLY with an explicit rebuild=1 subchannel. Without that channel, native placement is unchanged. False disables rebuilding for every blueprint.");
        protectTileEntities = cfg.getBoolean("protectAllTileEntities", "protection", false,
            "Optional protection of every block entity. False allows ordinary structure block entities to clear. Whitelists, inventory/fluid handlers and GT controllers/hatches remain protected.");
        maxVolume = cfg.getInt("maxVisitedBlocks", "general", 4000000, 1, 50000000,
            "Maximum volume of the enclosing cuboid of all selected pieces, including gaps. Abort BEFORE clearing if exceeded, out of height, or any chunk is unloaded.");
        logDirectory = new File(file.getAbsoluteFile().getParentFile().getParentFile(), "logs/structurerebuildnh");
        writeDebugLogs = cfg.getBoolean("writeDebugLogs", "logging", true,
            "Write one UTF-8 report per rebuild in logs/structurerebuildnh: target, configuration, geometry, formation, phases, counts and exceptions.");
        logBlockDecisions = cfg.getBoolean("logBlockDecisions", "logging", true,
            "Include every non-air clear/protect decision and native placement write/result. Large rebuilds produce large files. False keeps summaries and machine/geometry diagnostics.");
        protectedMods = lower(cfg.getStringList("modWhitelist", "protection", new String[] {
            "appliedenergistics2", "ae2fc", "thaumicenergistics", "ae2wct", "betterp2p", "neenergistics",
            "extracells", "ae2stuff", "ae2extras", "ae2utilities", "avaritiaddons", "IronChest", "JABBA",
            "StorageDrawers", "StorageDrawersNatura", "StorageDrawersBop", "StorageDrawersForestry",
            "StorageDrawersErebus", "StorageDrawersMisc", "EnderStorage", "irontanks", "Backpack", "adventurebackpack"
        }, "Mod IDs/registry namespaces, case-insensitive. Protects ALL blocks, including non-container blocks. Defaults include installed GTNH 2.9.0 storage/AE mods and optional AE addons."));
        protectedBlocks = lower(cfg.getStringList("blockWhitelist", "protection", new String[] {
            "minecraft:chest", "minecraft:trapped_chest", "minecraft:ender_chest", "minecraft:hopper",
            "minecraft:dispenser", "minecraft:dropper", "minecraft:furnace", "minecraft:lit_furnace", "minecraft:brewing_stand"
        }, "registry:block or registry:block@metadata. Protected ONLY during pre-clearing; native blueprint placement may replace it."));
        excludedControllers = lower(cfg.getStringList("excludedControllerClasses", "general", new String[0],
            "Exact Java class names whose native blueprint behaviour should be retained without rebuilding."));
        if (cfg.hasChanged()) cfg.save();
    }
    private static Set<String> lower(String[] values) {
        Set<String> result = new HashSet<String>();
        for (String value : values) if (!value.trim().isEmpty()) result.add(value.trim().toLowerCase(Locale.ROOT));
        return result;
    }
}
