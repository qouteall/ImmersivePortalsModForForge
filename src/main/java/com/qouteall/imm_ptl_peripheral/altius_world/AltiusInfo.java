package com.qouteall.imm_ptl_peripheral.altius_world;

import com.qouteall.imm_ptl_peripheral.ducks.IELevelProperties;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.dimension_sync.DimId;
import com.qouteall.immersive_portals.portal.global_portals.VerticalConnectingPortal;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.IServerConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AltiusInfo {
    
    private List<ResourceLocation> dimsFromTopToDown;
    
    public final boolean loop;
    public final boolean respectSpaceRatio;
    
    public AltiusInfo(
        List<RegistryKey<World>> dimsFromTopToDown
    ) {
        this(dimsFromTopToDown, false, false);
    }
    
    public AltiusInfo(
        List<RegistryKey<World>> dimsFromTopToDown, boolean loop, boolean respectSpaceRatio
    ) {
        this.dimsFromTopToDown = dimsFromTopToDown.stream().map(
            dimensionType -> dimensionType.func_240901_a_()
        ).collect(Collectors.toList());
        this.loop = loop;
        this.respectSpaceRatio = respectSpaceRatio;
    }
    
    // deprecated. used for upgrading old dimension stack
    public static AltiusInfo fromTag(CompoundNBT tag) {
        ListNBT listTag = tag.getList("dimensions", 8);
        List<RegistryKey<World>> dimensionTypeList = new ArrayList<>();
        listTag.forEach(t -> {
            StringNBT t1 = (StringNBT) t;
            String dimId = t1.getString();
            RegistryKey<World> dimensionType = DimId.idToKey(dimId);
            if (dimensionType != null) {
                dimensionTypeList.add(dimensionType);
            }
            else {
                Helper.log("Unknown Dimension Id " + dimId);
            }
        });
        return new AltiusInfo(dimensionTypeList);
    }
    
    public CompoundNBT toTag() {
        CompoundNBT tag = new CompoundNBT();
        ListNBT listTag = new ListNBT();
        dimsFromTopToDown.forEach(dimensionType -> {
            listTag.add(listTag.size(), StringNBT.valueOf(
                dimensionType.toString()
            ));
        });
        tag.put("dimensions", listTag);
        return tag;
    }
    
    public static AltiusInfo getInfoFromServer() {
        IServerConfiguration saveProperties = McHelper.getServer().func_240793_aU_();
        
        return ((IELevelProperties) saveProperties).getAltiusInfo();
    }
    
    public static void removeAltius() {
        IServerConfiguration saveProperties = McHelper.getServer().func_240793_aU_();
        
        ((IELevelProperties) saveProperties).setAltiusInfo(null);
    }
    
    public void createPortals() {
        List<ServerWorld> worldsFromTopToDown = dimsFromTopToDown.stream().flatMap(identifier -> {
            RegistryKey<World> dimension = DimId.idToKey(identifier);
            ServerWorld world = McHelper.getServer().getWorld(dimension);
            
            if (world != null) {
                return Stream.of(world);
            }
            else {
                McHelper.sendMessageToFirstLoggedPlayer(new StringTextComponent(
                    "Error: Dimension stack has invalid dimension " + dimension.func_240901_a_()
                ));
                
                return Stream.empty();
            }
        }).collect(Collectors.toList());
        
        if (worldsFromTopToDown.isEmpty()) {
            McHelper.sendMessageToFirstLoggedPlayer(new StringTextComponent(
                "Error: No dimension for dimension stack"
            ));
            return;
        }
        
        if (!McHelper.getGlobalPortals(worldsFromTopToDown.get(0)).isEmpty()) {
            Helper.err("There are already global portals when initializing dimension stack");
            return;
        }
        
        Helper.wrapAdjacentAndMap(
            worldsFromTopToDown.stream(),
            (top, down) -> {
                VerticalConnectingPortal.connectMutually(
                    top.func_234923_W_(), down.func_234923_W_(),
                    respectSpaceRatio
                );
                return null;
            }
        ).forEach(k -> {
        });
        
        if (loop) {
            VerticalConnectingPortal.connectMutually(
                Helper.lastOf(worldsFromTopToDown).getRegistryKey(),
                Helper.firstOf(worldsFromTopToDown).getRegistryKey(),
                respectSpaceRatio
            );
        }
        
        McHelper.sendMessageToFirstLoggedPlayer(
            new TranslationTextComponent("imm_ptl.dim_stack_initialized")
        );
    }
    
}
