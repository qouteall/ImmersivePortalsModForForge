package com.qouteall.imm_ptl_peripheral.altius_world;

import com.qouteall.imm_ptl_peripheral.ducks.IELevelProperties;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.dimension_sync.DimId;
import com.qouteall.immersive_portals.portal.global_portals.GlobalPortalStorage;
import com.qouteall.immersive_portals.portal.global_portals.VerticalConnectingPortal;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.IServerConfiguration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AltiusInfo {
    //store identifier because forge
    private List<ResourceLocation> dimsFromTopToDown;
    
    public AltiusInfo(List<RegistryKey<World>> dimsFromTopToDown) {
        this.dimsFromTopToDown = dimsFromTopToDown.stream().map(
            dimensionType -> dimensionType.func_240901_a_()
        ).collect(Collectors.toList());
    }
    
    public static AltiusInfo getDummy() {
        return new AltiusInfo(new ArrayList<>());
    }
    
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
    
    public static boolean isAltius() {
        return getInfoFromServer() != null;
    }
    
    public void createPortals() {
        if (dimsFromTopToDown.isEmpty()) {
            Helper.err("Dimension List is empty?");
            return;
        }
        RegistryKey<World> topDimension = DimId.idToKey(dimsFromTopToDown.get(0));
        if (topDimension == null) {
            Helper.err("Invalid Dimension " + dimsFromTopToDown.get(0));
            return;
        }
        ServerWorld world = McHelper.getServer().getWorld(topDimension);
        if (world == null) {
            Helper.err("Missing Dimension " + topDimension.func_240901_a_());
            return;
        }
        GlobalPortalStorage gps = GlobalPortalStorage.get(world);
        if (gps.data == null || gps.data.isEmpty()) {
            Helper.wrapAdjacentAndMap(
                dimsFromTopToDown.stream(),
                (top, down) -> {
                    VerticalConnectingPortal.connectMutually(
                        DimId.idToKey(top), DimId.idToKey(down),
                        0, VerticalConnectingPortal.getHeight(DimId.idToKey(down))
                    );
                    return null;
                }
            ).forEach(k -> {
            });
            Helper.log("Initialized Portals For Altius");
        }
    }
    
}
