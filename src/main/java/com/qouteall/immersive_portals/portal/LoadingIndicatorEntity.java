package com.qouteall.immersive_portals.portal;

import com.qouteall.immersive_portals.my_util.Helper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.world.World;

//it's client only
public class LoadingIndicatorEntity extends Entity {
    public static EntityType<LoadingIndicatorEntity> entityType;
    
    public LoadingIndicatorEntity(EntityType type, World world) {
        super(type, world);
    }
    
    @Override
    public Iterable<ItemStack> getArmorInventoryList() {
        return null;
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (Helper.getEntitiesNearby(this, Portal.class, 3).findAny().isPresent()) {
            this.remove();
        }
    }
    
    @Override
    protected void registerData() {
    
    }
    
    @Override
    protected void readAdditional(CompoundNBT var1) {
    
    }
    
    @Override
    protected void writeAdditional(CompoundNBT var1) {
    
    }
    
    @Override
    public IPacket<?> createSpawnPacket() {
        return null;
    }
}
