package com.qouteall.immersive_portals.portal;

import com.qouteall.hiding_in_the_bushes.MyNetwork;
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
    
        if (!world.isRemote()) {
            if (!isAlive) {
                remove();
            }
        }
    }
    
    @Override
    protected void registerData() {
        getDataManager().register(text, new StringTextComponent("Loading..."));
    }
    
    @Override
    protected void readAdditional(CompoundNBT var1) {
    
    }
    
    @Override
    protected void writeAdditional(CompoundNBT var1) {
    
    }
    
    @Override
    public IPacket<?> createSpawnPacket() {
        return MyNetwork.createStcSpawnEntity(this);
    }
    
    public void setText(ITextComponent str) {
        getDataManager().set(text, str);
    }
    
    public ITextComponent getText() {
        return getDataManager().get(text);
    }
}
