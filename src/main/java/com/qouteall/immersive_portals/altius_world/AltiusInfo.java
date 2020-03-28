package com.qouteall.immersive_portals.altius_world;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ducks.IELevelProperties;
import com.qouteall.immersive_portals.portal.global_portals.GlobalPortalStorage;
import com.qouteall.immersive_portals.portal.global_portals.VerticalConnectingPortal;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.dimension.DimensionType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class AltiusInfo {
    //store identifier because forge
    private List<ResourceLocation> dimsFromTopToDown;
    
    public AltiusInfo(List<DimensionType> dimsFromTopToDown) {
        this.dimsFromTopToDown = dimsFromTopToDown.stream().map(
            dimensionType -> DimensionType.getKey(dimensionType)
        ).collect(Collectors.toList());
    }
    
    public static AltiusInfo getDummy() {
        return new AltiusInfo(new ArrayList<>());
    }
    
    public static AltiusInfo fromTag(CompoundNBT tag) {
        ListNBT listTag = tag.getList("dimensions", 8);
        List<DimensionType> dimensionTypeList = new ArrayList<>();
        listTag.forEach(t -> {
            StringNBT t1 = (StringNBT) t;
            String dimId = t1.getString();
            DimensionType dimensionType = Registry.DIMENSION_TYPE.getOrDefault(new ResourceLocation(dimId));
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
        return ((IELevelProperties) McHelper.getServer().getWorld(DimensionType.OVERWORLD)
            .getWorldInfo()).getAltiusInfo();
    }
    
    public static boolean isAltius() {
        return getInfoFromServer() != null;
    }
    
    public void createPortals() {
        GlobalPortalStorage gps =
            GlobalPortalStorage.get(McHelper.getServer().getWorld(DimensionType.OVERWORLD));
        if (gps.data == null || gps.data.isEmpty()) {
            Helper.wrapAdjacentAndMap(
                dimsFromTopToDown.stream(),
                (top, down) -> {
                    VerticalConnectingPortal.connectMutually(
                        DimensionType.byName(top), DimensionType.byName(down),
                        0, getHeight(DimensionType.byName(down))
                    );
                    return null;
                }
            ).forEach(k -> {
            });
            Helper.log("Initialized Portals For Altius");
        }
    }
    
    public static int getHeight(DimensionType dimensionType) {
        if (dimensionType == DimensionType.THE_NETHER) {
            return 128;
        }
        return 256;
    }
}
