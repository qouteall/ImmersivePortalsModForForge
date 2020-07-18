package com.qouteall.immersive_portals.render.context_management;

import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.dimension_sync.DimId;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class RenderDimensionRedirect {
    private static Map<String, String> idMap = new HashMap<>();
    
    //null indicates no shader
    private static Map<RegistryKey<World>, RegistryKey<World>> redirectMap = new HashMap<>();
    
    public static void updateIdMap(Map<String, String> redirectIdMap) {
        idMap = redirectIdMap;
    }
    
    private static void updateRedirectMap() {
        redirectMap.clear();
        idMap.forEach((key, value) -> {
            RegistryKey<World> from = DimId.idToKey(new ResourceLocation(key));
            RegistryKey<World> to = DimId.idToKey(new ResourceLocation(value));
            if (from == null) {
                ModMain.clientTaskList.addTask(() -> {
                    CHelper.printChat("Invalid Dimension " + key);
                    return true;
                });
                return;
            }
            if (to == null) {
                if (!value.equals("vanilla")) {
                    ModMain.clientTaskList.addTask(() -> {
                        CHelper.printChat("Invalid Dimension " + value);
                        return true;
                    });
                    return;
                }
            }
            
            redirectMap.put(from, to);
        });
    }
    
    public static boolean isNoShader(RegistryKey<World> dimension) {
        if (redirectMap.containsKey(dimension)) {
            RegistryKey<World> r = redirectMap.get(dimension);
            if (r == null) {
                return true;
            }
        }
        return false;
    }
    
    public static RegistryKey<World> getRedirectedDimension(RegistryKey<World> dimension) {
        if (redirectMap.containsKey(dimension)) {
            RegistryKey<World> r = redirectMap.get(dimension);
            if (r == null) {
                return dimension;
            }
            return r;
        }
        else {
            return dimension;
        }
    }
    
    public static boolean hasSkylight(ClientWorld world) {
        updateRedirectMap();
        RegistryKey<World> redirectedDimension = getRedirectedDimension(world.func_234923_W_());
        if (redirectedDimension == world.func_234923_W_()) {
            return world.func_230315_m_().hasSkyLight();
        }
        
        //if it's redirected, it's probably redirected to a vanilla dimension
        if (redirectedDimension == World.field_234918_g_) {
            return true;
        }
        else {
            return false;
        }
    }
}
