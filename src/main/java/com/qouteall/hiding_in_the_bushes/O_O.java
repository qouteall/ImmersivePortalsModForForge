package com.qouteall.hiding_in_the_bushes;

import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import net.minecraft.block.Blocks;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorld;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.DimensionManager;

public class O_O {
    public static boolean isForge() {
        return true;
    }
    
    @OnlyIn(Dist.CLIENT)
    public static void onPlayerChangeDimensionClient(
        DimensionType from, DimensionType to
    ) {
        //nothing
    }
    
    @OnlyIn(Dist.CLIENT)
    public static void segregateClientEntity(
        ClientWorld fromWorld,
        Entity entity
    ) {
        ((IEClientWorld_MA) fromWorld).removeEntityWhilstMaintainingCapability(entity);
        entity.removed = false;
    }
    
    public static void segregateServerEntity(
        ServerWorld fromWorld,
        Entity entity
    ) {
        fromWorld.removeEntity(entity, true);
        entity.revive();
    }
    
    public static void segregateServerPlayer(
        ServerWorld fromWorld,
        ServerPlayerEntity player
    ) {
        fromWorld.removePlayer(player, true);
        player.revive();
    }
    
    public static void onPlayerTravelOnServer(
        ServerPlayerEntity player,
        DimensionType from,
        DimensionType to
    ) {
        Global.serverTeleportationManager.isFiringMyChangeDimensionEvent = true;
        net.minecraftforge.fml.hooks.BasicEventHooks.firePlayerChangedDimensionEvent(
            player, from, to
        );
        Global.serverTeleportationManager.isFiringMyChangeDimensionEvent = false;
    }
    
    public static void loadConfigFabric() {
        //nothing
    }
    
    public static boolean isObsidian(IWorld world, BlockPos obsidianPos) {
        return world.getBlockState(obsidianPos).isPortalFrame(world, obsidianPos);
    }
    
    public static final boolean isReachEntityAttributesPresent = false;
    
    public static void registerDimensionsForge() {
        try {
            DimensionManager.fireRegister();
        }
        catch (Throwable e) {
            Helper.err("Exception When Registering Dimensions " + e);
        }
        
    }
    
    public static boolean detectOptiFine(){
        return MyMixinConnector.getIsOptifinePresent();
    }
}
