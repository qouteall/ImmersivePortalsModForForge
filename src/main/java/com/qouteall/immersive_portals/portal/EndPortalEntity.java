package com.qouteall.immersive_portals.portal;

import com.qouteall.immersive_portals.McHelper;
import net.minecraft.block.Blocks;
import net.minecraft.block.pattern.BlockPattern;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;

public class EndPortalEntity extends Portal {
    public static EntityType<EndPortalEntity> entityType;
    
    public EndPortalEntity(
        EntityType<?> entityType_1,
        World world_1
    ) {
        super(entityType_1, world_1);
    }
    
    public static void onEndPortalComplete(ServerWorld world, BlockPattern.PatternHelper pattern) {
        Portal portal = entityType.create(world);
        
        Vec3d center = new Vec3d(pattern.getFrontTopLeft()).add(-1.5, 0.5, -1.5);
        portal.setPosition(center.x, center.y, center.z);
        
        portal.destination = new Vec3d(0, 120, 0);
        
        portal.dimensionTo = DimensionType.THE_END;
    
        portal.axisW = new Vec3d(0, 0, 1);
        portal.axisH = new Vec3d(1, 0, 0);
        portal.width = 3;
        portal.height = 3;
        
        portal.loadFewerChunks = false;
    
        world.addEntity(portal);
    }
    
    @Override
    public void onEntityTeleportedOnServer(Entity entity) {
        if (entity instanceof LivingEntity) {
            LivingEntity livingEntity = (LivingEntity) entity;
            livingEntity.addPotionEffect(
                new EffectInstance(
                    Effects.SLOW_FALLING,
                    120,//duration
                    1//amplifier
                )
            );
        }
        if (entity instanceof ServerPlayerEntity) {
            generateObsidianPlatform();
        }
    }
    
    private void generateObsidianPlatform() {
        ServerWorld endWorld = McHelper.getServer().getWorld(DimensionType.THE_END);
        BlockPos spawnPoint = endWorld.getSpawnCoordinate();
        
        int int_1 = spawnPoint.getX();
        int int_2 = spawnPoint.getY() - 1;
        int int_3 = spawnPoint.getZ();
        
        for (int int_6 = -2; int_6 <= 2; ++int_6) {
            for (int int_7 = -2; int_7 <= 2; ++int_7) {
                for (int int_8 = -1; int_8 < 3; ++int_8) {
                    int int_9 = int_1 + int_7 * 1 + int_6 * 0;
                    int int_10 = int_2 + int_8;
                    int int_11 = int_3 + int_7 * 0 - int_6 * 1;
                    boolean boolean_1 = int_8 < 0;
                    endWorld.setBlockState(
                        new BlockPos(int_9, int_10, int_11),
                        boolean_1 ? Blocks.OBSIDIAN.getDefaultState() :
                            Blocks.AIR.getDefaultState()
                    );
                }
            }
        }
    }
}
