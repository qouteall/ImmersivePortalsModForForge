package com.qouteall.immersive_portals.portal.extension;

import com.qouteall.immersive_portals.chunk_loading.NewChunkTrackingGraph;
import com.qouteall.immersive_portals.portal.Portal;
import java.util.WeakHashMap;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

// the additional features of a portal
public class PortalExtension {
    public double motionAffinity = 0;
    
    private static class PlayerPortalVisibility {
        public long lastVisibleTime = 0;
        public int currentCap = 0;
        public int targetCap = 0;
        
        public void updateEverySecond() {
            if (targetCap > currentCap) {
                currentCap++;
            }
            else if (targetCap < currentCap) {
                currentCap--;
            }
        }
    }
    
    private WeakHashMap<ServerPlayerEntity, PlayerPortalVisibility> playerLoadStatus;
    
    public PortalExtension() {
    
    }
    
    public void readFromNbt(CompoundNBT compoundTag) {
        if (compoundTag.contains("motionAffinity")) {
            motionAffinity = compoundTag.getDouble("motionAffinity");
        }
    }
    
    public void writeToNbt(CompoundNBT compoundTag) {
        if (motionAffinity != 0) {
            compoundTag.putDouble("motionAffinity", motionAffinity);
        }
    }
    
    public void tick(Portal portal) {
        if (portal.world.isRemote()) {
            tickClient(portal);
        }
        else {
            if (playerLoadStatus == null) {
                playerLoadStatus = new WeakHashMap<>();
            }
            playerLoadStatus.entrySet().removeIf(e -> e.getKey().removed);
            
            if (portal.world.getGameTime() % 20 == 1) {
                for (PlayerPortalVisibility value : playerLoadStatus.values()) {
                    value.updateEverySecond();
                }
            }
        }
    }
    
    @OnlyIn(Dist.CLIENT)
    private void tickClient(Portal portal) {
    
    }
    
    public int refreshAndGetLoadDistanceCap(Portal portal, ServerPlayerEntity player, int currentCap) {
        if (playerLoadStatus == null) {
            playerLoadStatus = new WeakHashMap<>();
        }
        
        PlayerPortalVisibility rec = playerLoadStatus.computeIfAbsent(
            player, k -> new PlayerPortalVisibility()
        );
        
        final int dropTimeout = NewChunkTrackingGraph.updateInterval * 2;
        
        long worldTime = portal.world.getGameTime();
        
        long timePassed = Math.abs(worldTime - rec.lastVisibleTime);
        if (timePassed > dropTimeout) {
            // not loaded for sometime and reload, reset all
            rec.targetCap = currentCap;
            rec.currentCap = 0;
        }
        else if (timePassed == dropTimeout) {
            // being checked the second time in this turn
            rec.targetCap = Math.max(rec.targetCap, currentCap);
        }
        else {
            // being checked the first time in this turn
            rec.targetCap = currentCap;
        }
        
        return rec.currentCap;
    }
}
