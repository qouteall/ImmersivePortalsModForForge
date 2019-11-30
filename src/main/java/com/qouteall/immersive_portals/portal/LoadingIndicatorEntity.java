package com.qouteall.immersive_portals.portal;

import com.qouteall.immersive_portals.Helper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

//it's client only
public class LoadingIndicatorEntity extends Entity {
    public static EntityType<LoadingIndicatorEntity> entityType;
    private static final DataParameter<String> text = EntityDataManager.createKey(
        LoadingIndicatorEntity.class, DataSerializers.STRING
    );
    
    public boolean isAlive = false;
    
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
    
        if (Helper.getEntitiesNearby(this, Portal.class, 1).findAny().isPresent()) {
            this.remove();
        }
    
        if (!world.isRemote) {
            if (!isAlive) {
                remove();
            }
        }
    }
    
    @Override
    protected void registerData() {
        dataManager.register(text, "Loading...");
    }
    
    @Override
    protected void readAdditional(CompoundNBT var1) {
    
    }
    
    @Override
    protected void writeAdditional(CompoundNBT var1) {
    
    }
    
    @Override
    public IPacket<?> createSpawnPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
    
    public void setText(String str) {
        dataManager.set(text, str);
    }
    
    public String getText() {
        return dataManager.get(text);
    }
}
