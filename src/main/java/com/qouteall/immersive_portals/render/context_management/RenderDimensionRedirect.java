package com.qouteall.immersive_portals.render.context_management;

import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.ModMain;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class RenderDimensionRedirect {
    private static Map<String, String> idMap = new HashMap<>();
    
    //null indicates no shader
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
                ModMain.clientTaskList.addTask(()->{
                    CHelper.printChat("Invalid Dimension " + key);
                    return true;
                });
                return;
            }
            if (to == null) {
                if (!value.equals("vanilla")) {
                    ModMain.clientTaskList.addTask(()->{
                        CHelper.printChat("Invalid Dimension " + value);
                        return true;
                    });
                    return;
                }
            }
            
            redirectMap.put(from, to);
        });
    }
    
    public static boolean isNoShader(DimensionType dimension) {
        if (redirectMap.containsKey(dimension)) {
            DimensionType r = redirectMap.get(dimension);
            if (r == null) {
                return true;
            }
        }
        return false;
    }
    
    public static DimensionType getRedirectedDimension(DimensionType dimension) {
        if (redirectMap.containsKey(dimension)) {
            DimensionType r = redirectMap.get(dimension);
            if (r == null) {
                return dimension;
            }
            return r;
        }
        else {
            return dimension;
        }
    }
    
    public static boolean hasSkylight(Dimension dimension) {
        updateRedirectMap();
        DimensionType redirectedDimension = getRedirectedDimension(dimension.getType());
        if (redirectedDimension == dimension.getType()) {
            return dimension.hasSkyLight();
        }
        
        //if it's redirected, it's probably redirected to a vanilla dimension
        if (redirectedDimension == DimensionType.OVERWORLD) {
            return true;
        }
        else {
            return false;
        }
    }
}
