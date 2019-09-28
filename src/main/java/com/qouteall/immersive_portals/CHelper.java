package com.qouteall.immersive_portals;

import com.qouteall.immersive_portals.render.MyRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.NetworkPlayerInfo;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CHelper {
    public static NetworkPlayerInfo getClientPlayerListEntry() {
        return Minecraft.getInstance().getConnection().getPlayerInfo(
            Minecraft.getInstance().player.getGameProfile().getId()
        );
    }
    
    //NOTE this may not be reliable
    public static DimensionType getOriginalDimension() {
        if (CGlobal.renderer.isRendering()) {
            return MyRenderHelper.originalPlayerDimension;
        }
        else {
            return Minecraft.getInstance().player.dimension;
        }
    }
    
    public static boolean shouldDisableFog() {
        return false;
//        if (CGlobal.isOptifinePresent) {
//            return Config.isFogOff() && Minecraft.getInstance().gameRenderer.fogStandard;
//        }
//        else {
//            return false;
//        }
    }
    
    //do not inline this
    //or it will crash in server
    public static World getClientWorld(DimensionType dimension) {
        return CGlobal.clientWorldLoader.getOrCreateFakedWorld(dimension);
    }
}
