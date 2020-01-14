package com.qouteall.immersive_portals;


import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

public class ConfigClient {
    public static final ConfigClient instance;
    public static final ForgeConfigSpec spec;
    public final ForgeConfigSpec.BooleanValue compatibilityRenderMode;
    
    public ConfigClient(ForgeConfigSpec.Builder builder) {
        compatibilityRenderMode = builder
            .comment("With this you can't see portal-in-portal")
            .define("compatibility_render_mode", false);
    }
    
    public static boolean isInitialCompatibilityRenderMode() {
        return instance.compatibilityRenderMode.get();
    }
    
    static {
        Pair<ConfigClient, ForgeConfigSpec> pair =
            new ForgeConfigSpec.Builder().configure(ConfigClient::new);
        instance = pair.getKey();
        spec = pair.getValue();
    }
    
    public static void init() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, spec);
    }
}

