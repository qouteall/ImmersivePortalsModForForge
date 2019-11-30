package com.qouteall.immersive_portals.portal.nether_portal;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.PortalPlaceholderBlock;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;

import java.util.UUID;

public class NetherPortalEntity extends Portal {
    public static EntityType<NetherPortalEntity> entityType;
    
    //the reversed portal is in another dimension and face the opposite direction
    public UUID reversePortalId;
    public ObsidianFrame obsidianFrame;
    
    private boolean isNotified = true;
    private boolean shouldBreakNetherPortal = false;
    
    public static void init() {
        PortalPlaceholderBlock.portalBlockUpdateSignal.connect((world, pos) -> {
            McHelper.getEntitiesNearby(
                world,
                new Vec3d(pos),
                NetherPortalEntity.class,
                20
            ).forEach(
                NetherPortalEntity::notifyToCheckIntegrity
            );
        });
    }
    
    public NetherPortalEntity(
        EntityType<?> type,
        World world
    ) {
        super(type, world);
    }
    
    private void breakPortalOnThisSide() {
        assert shouldBreakNetherPortal;
        assert !removed;
        
        breakNetherPortalBlocks();
        this.remove();
    }
    
    private void breakNetherPortalBlocks() {
        ServerWorld world1 = McHelper.getServer().getWorld(dimension);
    
        obsidianFrame.boxWithoutObsidian.stream()
            .filter(
                blockPos -> world1.getBlockState(
                    blockPos
                ).getBlock() == PortalPlaceholderBlock.instance
            )
            .forEach(
                blockPos -> world1.setBlockState(
                    blockPos, Blocks.AIR.getDefaultState()
                )
            );
    }
    
    @Override
    public boolean isPortalValid() {
        return super.isPortalValid() &&
            reversePortalId != null &&
            obsidianFrame != null;
    }
    
    private void notifyToCheckIntegrity() {
        isNotified = true;
    }
    
    private NetherPortalEntity getReversePortal() {
        assert !world.isRemote;
        
        ServerWorld world = getServer().getWorld(dimensionTo);
        return world == null ?
            null : (NetherPortalEntity) world.getEntityByUuid(reversePortalId);
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
            NetherPortalEntity reversePortal = getReversePortal();
            if (reversePortal != null) {
                reversePortal.shouldBreakNetherPortal = true;
            }
            else {
                Helper.err(
                    "Cannot find the reverse portal. Nether portal may not be removed normally."
                );
            }
        }
    }
    
    private boolean isPortalIntactOnThisSide() {
        assert McHelper.getServer() != null;
        
        return NetherPortalMatcher.isObsidianFrameIntact(
            world,
            obsidianFrame.normalAxis,
            obsidianFrame.boxWithoutObsidian
        )
            && isInnerPortalBlocksIntact(world, obsidianFrame);
    }
    
    //if the region is not loaded, it will return true
    private static boolean isObsidianFrameIntact(
        DimensionType dimension,
        ObsidianFrame obsidianFrame
    ) {
        ServerWorld world = McHelper.getServer().getWorld(dimension);
        
        if (world == null) {
            return true;
        }
        
        if (!world.isBlockLoaded(obsidianFrame.boxWithoutObsidian.l)) {
            return true;
        }
    
        if (!NetherPortalMatcher.isObsidianFrameIntact(
            world,
            obsidianFrame.normalAxis,
            obsidianFrame.boxWithoutObsidian
        )) {
            return false;
        }
    
        return isInnerPortalBlocksIntact(world, obsidianFrame);
    }
    
    private static boolean isInnerPortalBlocksIntact(
        IWorld world,
        ObsidianFrame obsidianFrame
    ) {
        return obsidianFrame.boxWithoutObsidian.stream().allMatch(
            blockPos -> world.getBlockState(blockPos).getBlock()
                == PortalPlaceholderBlock.instance
        );
    }
    
    
    @Override
    protected void readAdditional(CompoundNBT compoundTag) {
        super.readAdditional(compoundTag);
    
        reversePortalId = compoundTag.getUniqueId("reversePortalId");
        obsidianFrame = ObsidianFrame.fromTag(compoundTag.getCompound("obsidianFrame"));
    }
    
    @Override
    protected void writeAdditional(CompoundNBT compoundTag) {
        super.writeAdditional(compoundTag);
    
        compoundTag.putUniqueId("reversePortalId", reversePortalId);
        compoundTag.put("obsidianFrame", obsidianFrame.toTag());
    }
    
    @Override
    public void writeSpawnData(PacketBuffer buffer) {
        super.writeSpawnData(buffer);
        buffer.writeUniqueId(reversePortalId);
        buffer.writeCompoundTag(obsidianFrame.toTag());
    }
    
    @Override
    public void readSpawnData(PacketBuffer additionalData) {
        super.readSpawnData(additionalData);
        reversePortalId = additionalData.readUniqueId();
        obsidianFrame = ObsidianFrame.fromTag(additionalData.readCompoundTag());
    }
}
