package com.qouteall.hiding_in_the_bushes;


import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConfigClient {
    public static final ConfigClient instance;
    public static final ForgeConfigSpec spec;
    public final ForgeConfigSpec.BooleanValue compatibilityRenderMode;
    public final ForgeConfigSpec.BooleanValue doCheckGlError;
    public final ForgeConfigSpec.IntValue maxPortalLayer;
    public final ForgeConfigSpec.BooleanValue renderYourselfInPortal;
    public final ForgeConfigSpec.BooleanValue correctCrossPortalEntityRendering;
    public final ForgeConfigSpec.ConfigValue<String> renderDimensionRedirect;
    
    public final String defaultDimRedirect = "immersive_portals:alternate1->minecraft:overworld\n" +
        "immersive_portals:alternate2->minecraft:overworld\n" +
        "immersive_portals:alternate3->minecraft:overworld\n" +
        "immersive_portals:alternate4->minecraft:overworld\n" +
        "immersive_portals:alternate5->minecraft:overworld\n";
    
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
        correctCrossPortalEntityRendering = builder
            .comment("This May Decrease FPS")
            .define("correct_cross_portal_entity_rendering", true);
        renderDimensionRedirect = builder.comment(
            "See the Wiki to Know How to Configure it"
        ).define(
            "dimension_render_redirect",
            defaultDimRedirect
        );
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
    
    public static final String splitter = "->";
    
    public static Map<String, String> listToMap(List<String> redirectList) {
        Map<String, String> result = new HashMap<>();
        for (String s : redirectList) {
            int i = s.indexOf(splitter);
            if (i != -1) {
                result.put(
                    s.substring(0, i),
                    s.substring(i + 2)
                );
            }
            else {
                result.put(s, "???");
            }
        }
        return result;
    }
}

