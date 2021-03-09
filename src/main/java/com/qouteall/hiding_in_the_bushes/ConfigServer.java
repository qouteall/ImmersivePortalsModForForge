package com.qouteall.hiding_in_the_bushes;

import com.qouteall.immersive_portals.Global;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

public class ConfigServer {
    public static final ConfigServer instance;
    public static final ForgeConfigSpec spec;
    public final ForgeConfigSpec.IntValue portalSearchingRange;
    public final ForgeConfigSpec.IntValue indirectLoadingRadiusCap;
    public final ForgeConfigSpec.BooleanValue activeLoadRemoteChunks;
    public final ForgeConfigSpec.BooleanValue teleportationDebug;
    public final ForgeConfigSpec.BooleanValue multiThreadedNetherPortalSearching;
    public final ForgeConfigSpec.BooleanValue looseMovementCheck;
    public final ForgeConfigSpec.BooleanValue enableAlternateDimensions;
    public final ForgeConfigSpec.EnumValue<Global.NetherPortalMode> netherPortalMode;
    public final ForgeConfigSpec.EnumValue<Global.EndPortalMode> endPortalMode;
    
    public ConfigServer(ForgeConfigSpec.Builder builder) {
        portalSearchingRange = builder
            .comment("The Range of Existing Frame Searching When Generating Nether Portal")
            .defineInRange("portal_searching_range", 128, 32, 1000);
        indirectLoadingRadiusCap = builder
            .comment("Indirect Loading Radius Cap")
            .defineInRange("indirect_loading_radius_cap", 8, 3, 20);
        activeLoadRemoteChunks = builder
            .comment("Load Remote Chunks Actively")
            .define("actively_load_remote_chunks", true);
        teleportationDebug = builder
            .comment("Teleportation Debug")
            .define("teleportation_debug", false);
        multiThreadedNetherPortalSearching = builder
            .comment("Multi Threaded Nether Portal Searching")
            .define("multi_threaded_nether_portal_searching", true);
        looseMovementCheck = builder
            .comment("Loose Server Side Anti Hack Movement Check")
            .define("loose_movement_check", false);
        enableAlternateDimensions = builder
            .comment("Enable Alternate Dimensions")
            .define("enable_alternate_dimensions", true);
        netherPortalMode = builder
            .comment("Nether Portal Mode")
            .defineEnum("nether_portal_mode", Global.NetherPortalMode.normal);
        endPortalMode = builder
            .comment("End Portal Mode")
            .defineEnum("end_portal_mode", Global.EndPortalMode.normal);
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
