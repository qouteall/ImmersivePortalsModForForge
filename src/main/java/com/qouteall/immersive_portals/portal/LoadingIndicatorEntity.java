package com.qouteall.immersive_portals.portal;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.fml.network.NetworkHooks;

public class LoadingIndicatorEntity extends Entity {
    public static EntityType<LoadingIndicatorEntity> entityType;
    private static final DataParameter<ITextComponent> text = EntityDataManager.createKey(
        LoadingIndicatorEntity.class, DataSerializers.TEXT_COMPONENT
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
    
        if (!world.isRemote) {
            if (!isAlive) {
                remove();
            }
        }
    }
    
    @Override
    protected void registerData() {
        dataManager.register(text, new StringTextComponent("Loading..."));
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
    
    public void setText(ITextComponent str) {
        dataManager.set(text, str);
    }
    
    public ITextComponent getText() {
        return dataManager.get(text);
    }
}
