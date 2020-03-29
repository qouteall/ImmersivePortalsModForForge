package com.qouteall.immersive_portals.render.context_management;

import com.qouteall.immersive_portals.CHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class RenderDimensionRedirect {
    private static Map<String, String> idMap = new HashMap<>();
    private static Map<DimensionType, DimensionType> redirectMap = new HashMap<>();
    
    public static void updateIdMap(Map<String, String> redirectIdMap) {
        idMap = redirectIdMap;
    }
    
    private static void updateRedirectMap() {
        redirectMap.clear();
        idMap.forEach((key, value) -> {
            DimensionType from = DimensionType.byName(new ResourceLocation(key));
            DimensionType to = DimensionType.byName(new ResourceLocation(value));
            if (from == null) {
                CHelper.printChat("Invalid Dimension " + key);
                return;
            }
            if (to == null) {
                CHelper.printChat("Invalid Dimension " + value);
                return;
            }
            
            redirectMap.put(from, to);
        });
    }
    
    public static DimensionType getRedirectedDimension(DimensionType dimension) {
        return redirectMap.getOrDefault(dimension, dimension);
    }
    
    //avoid infinite recursion
    public static boolean hasSkylight(DimensionType dimension) {
        updateRedirectMap();
        DimensionType redirectedDimension = getRedirectedDimension(dimension);
        if (redirectedDimension == DimensionType.OVERWORLD) {
            return true;
        }
        else {
            return false;
        }
    }
}
