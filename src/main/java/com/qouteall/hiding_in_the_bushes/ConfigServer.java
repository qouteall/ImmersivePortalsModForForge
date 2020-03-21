package com.qouteall.hiding_in_the_bushes;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

public class ConfigServer {
    public static final ConfigServer instance;
    public static final ForgeConfigSpec spec;
    public final ForgeConfigSpec.IntValue portalSearchingRange;
    public final ForgeConfigSpec.BooleanValue longReachInCreative;
    public final ForgeConfigSpec.BooleanValue activeLoadRemoteChunks;
    
    public ConfigServer(ForgeConfigSpec.Builder builder) {
        portalSearchingRange = builder
            .comment("The Range of Existing Frame Searching When Generating Nether Portal")
            .defineInRange("portal_searching_range", 128, 32, 1000);
        longReachInCreative = builder
            .comment("Longer Hand Reach In Creative")
            .define("long_reach_in_creative", true);
        activeLoadRemoteChunks = builder
            .comment("Load Remote Chunks Actively")
            .define("actively_load_remote_chunks", true);
    }
    
    static {
        Pair<ConfigServer, ForgeConfigSpec> pair =
            new ForgeConfigSpec.Builder().configure(ConfigServer::new);
        instance = pair.getKey();
        spec = pair.getValue();
    }
    
    public static void init() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, spec);
    }
}
