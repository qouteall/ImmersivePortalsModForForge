package com.qouteall.immersive_portals.portal.extension;

import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.nbt.CompoundNBT;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

// the additional features of a portal
public class PortalExtension {
    public double motionAffinity = 0;
    public boolean isSpecialFlippingPortal = false;
    
    public PortalExtension() {
    
    }
    
    public void readFromNbt(CompoundNBT compoundTag) {
        if (compoundTag.contains("motionAffinity")) {
            motionAffinity = compoundTag.getDouble("motionAffinity");
        }
        if (compoundTag.contains("isSpecialFlippingPortal")) {
            isSpecialFlippingPortal = compoundTag.getBoolean("isSpecialFlippingPortal");
        }
        
    }
    
    public void writeToNbt(CompoundNBT compoundTag) {
        if (motionAffinity != 0) {
            compoundTag.putDouble("motionAffinity", motionAffinity);
        }
        if (isSpecialFlippingPortal) {
            compoundTag.putBoolean("isSpecialFlippingPortal", isSpecialFlippingPortal);
        }
    }
    
    public void tick(Portal portal) {
        if (portal.world.isRemote()) {
            tickClient(portal);
        }
    }
    
    @OnlyIn(Dist.CLIENT)
    private void tickClient(Portal portal) {
    
    }
}
