package com.qouteall.immersive_portals.portal.nether_portal;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.PortalPlaceholderBlock;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import java.util.UUID;

public abstract class BreakablePortalEntity extends Portal {
    public BlockPortalShape blockPortalShape;
    public UUID reversePortalId;
    public boolean unbreakable = false;
    private boolean isNotified = true;
    private boolean shouldBreakPortal = false;
    
    public BreakablePortalEntity(
        EntityType<?> entityType_1,
        World world_1
    ) {
        super(entityType_1, world_1);
    }
    
    @Override
    public boolean isPortalValid() {
        if (world.isRemote) {
            return super.isPortalValid();
        }
        return super.isPortalValid() && blockPortalShape != null && reversePortalId != null;
    }
    
    @Override
    protected void readAdditional(CompoundNBT compoundTag) {
        super.readAdditional(compoundTag);
        if (compoundTag.contains("netherPortalShape")) {
            blockPortalShape = new BlockPortalShape(compoundTag.getCompound("netherPortalShape"));
        }
        reversePortalId = Helper.getUuid(compoundTag,"reversePortalId");
        unbreakable = compoundTag.getBoolean("unbreakable");
    }
    
    @Override
    protected void writeAdditional(CompoundNBT compoundTag) {
        super.writeAdditional(compoundTag);
        if (blockPortalShape != null) {
            compoundTag.put("netherPortalShape", blockPortalShape.toTag());
        }
        Helper.putUuid(compoundTag,"reversePortalId", reversePortalId);
        compoundTag.putBoolean("unbreakable", unbreakable);
    }
    
    private void breakPortalOnThisSide() {
        assert shouldBreakPortal;
        assert !removed;
        
        blockPortalShape.area.forEach(
            blockPos -> {
                if (world.getBlockState(blockPos).getBlock() == PortalPlaceholderBlock.instance) {
                    world.setBlockState(
                        blockPos, Blocks.AIR.getDefaultState()
                    );
                }
            }
        );
        this.remove();
        
        Helper.log("Broke " + this);
    }
    
    public void notifyPlaceholderUpdate() {
        isNotified = true;
    }
    
    private BreakablePortalEntity getReversePortal() {
        assert !world.isRemote;
        
        ServerWorld world = getServer().getWorld(dimensionTo);
        Entity entity = world.getEntityByUuid(reversePortalId);
        if (entity instanceof BreakablePortalEntity) {
            return (BreakablePortalEntity) entity;
        }
        else {
            return null;
        }
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (world.isRemote) {
            addSoundAndParticle();
        }
        else {
            if (!unbreakable) {
                if (isNotified) {
                    isNotified = false;
                    checkPortalIntegrity();
                }
                if (shouldBreakPortal) {
                    breakPortalOnThisSide();
                }
    
                if (world.getGameTime() % 233 == getEntityId() % 233) {
                    checkPortalIntegrity();
                }
            }
        }
        
    }
    
    private void checkPortalIntegrity() {
        assert !world.isRemote;
        
        if (!isPortalValid()) {
            remove();
            return;
        }
        
        if (!isPortalIntactOnThisSide()) {
            shouldBreakPortal = true;
            BreakablePortalEntity reversePortal = getReversePortal();
            if (reversePortal != null) {
                reversePortal.shouldBreakPortal = true;
            }
        }
    }
    
    protected abstract boolean isPortalIntactOnThisSide();
    
    @OnlyIn(Dist.CLIENT)
    protected abstract void addSoundAndParticle();
}
