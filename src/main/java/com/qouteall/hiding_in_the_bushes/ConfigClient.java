package com.qouteall.hiding_in_the_bushes;


import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

public class ConfigClient {
    public static final ConfigClient instance;
    public static final ForgeConfigSpec spec;
    public final ForgeConfigSpec.BooleanValue compatibilityRenderMode;
    public final ForgeConfigSpec.BooleanValue doCheckGlError;
    public final ForgeConfigSpec.IntValue maxPortalLayer;
    public final ForgeConfigSpec.BooleanValue renderYourselfInPortal;
    
    public ConfigClient(ForgeConfigSpec.Builder builder) {
        compatibilityRenderMode = builder
            .comment("With this you can't see portal-in-portal")
            .define("compatibility_render_mode", false);
        doCheckGlError = builder
            .comment("With this the performance may drop")
            .define("check_gl_error", false);
        maxPortalLayer = builder
            .comment("Max Portal-in-portal Render Layer")
            .defineInRange("max_portal_layer", 5, 1, 10);
        renderYourselfInPortal = builder
            .comment("Render Yourself In Portal")
            .define("render_yourself_in_portal", true);
        
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

