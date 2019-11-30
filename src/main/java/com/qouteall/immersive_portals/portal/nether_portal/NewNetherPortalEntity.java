package com.qouteall.immersive_portals.portal.nether_portal;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.PortalPlaceholderBlock;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import java.util.UUID;

public class NewNetherPortalEntity extends Portal {
    public static EntityType<NewNetherPortalEntity> entityType;
    
    public NetherPortalShape netherPortalShape;
    public UUID reversePortalId;
    
    private boolean isNotified = true;
    private boolean shouldBreakNetherPortal = false;
    
    public NewNetherPortalEntity(
        EntityType<?> entityType_1,
        World world_1
    ) {
        super(entityType_1, world_1);
    }
    
    public static void init() {
        PortalPlaceholderBlock.portalBlockUpdateSignal.connect((world, pos) -> {
            Helper.getEntitiesNearby(
                world,
                new Vec3d(pos),
                NewNetherPortalEntity.class,
                20
            ).forEach(
                NewNetherPortalEntity::notifyToCheckIntegrity
            );
        });
    }
    
    @Override
    public boolean isPortalValid() {
        if (world.isRemote) {
            return super.isPortalValid();
        }
        return super.isPortalValid() && netherPortalShape != null && reversePortalId != null;
    }
    
    @Override
    protected void readAdditional(CompoundNBT compoundTag) {
        super.readAdditional(compoundTag);
        if (compoundTag.contains("netherPortalShape")) {
            netherPortalShape = new NetherPortalShape(compoundTag.getCompound("netherPortalShape"));
        }
        reversePortalId = compoundTag.getUniqueId("reversePortalId");
    }
    
    @Override
    protected void writeAdditional(CompoundNBT compoundTag) {
        super.writeAdditional(compoundTag);
        if (netherPortalShape != null) {
            compoundTag.put("netherPortalShape", netherPortalShape.toTag());
        }
        compoundTag.putUniqueId("reversePortalId", reversePortalId);
    }
    
    
    private void breakPortalOnThisSide() {
        assert shouldBreakNetherPortal;
        assert !removed;
        
        netherPortalShape.area.forEach(
            blockPos -> world.setBlockState(
                blockPos, Blocks.AIR.getDefaultState()
            )
        );
        this.remove();
        
        Helper.log("Broke " + this);
    }
    
    private void notifyToCheckIntegrity() {
        isNotified = true;
    }
    
    private NewNetherPortalEntity getReversePortal() {
        assert !world.isRemote;
        
        ServerWorld world = getServer().getWorld(dimensionTo);
        return (NewNetherPortalEntity) world.getEntityByUuid(reversePortalId);
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (!world.isRemote) {
            if (isNotified) {
                isNotified = false;
                checkPortalIntegrity();
            }
            if (shouldBreakNetherPortal) {
                breakPortalOnThisSide();
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
            shouldBreakNetherPortal = true;
            NewNetherPortalEntity reversePortal = getReversePortal();
            if (reversePortal != null) {
                reversePortal.shouldBreakNetherPortal = true;
            }
        }
    }
    
    private boolean isPortalIntactOnThisSide() {
        assert Helper.getServer() != null;
        
        return netherPortalShape.area.stream()
            .allMatch(blockPos ->
                world.getBlockState(blockPos).getBlock() == PortalPlaceholderBlock.instance
            ) &&
            netherPortalShape.frameAreaWithoutCorner.stream()
                .allMatch(blockPos ->
                    world.getBlockState(blockPos).getBlock() == Blocks.OBSIDIAN
                );
    }
}
