package com.qouteall.immersive_portals.portal;

import com.qouteall.immersive_portals.McHelper;
import net.minecraft.block.pattern.BlockPattern;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Items;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
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
        Portal portal = new EndPortalEntity(entityType, world);
        
        Vector3d center = Vector3d.func_237491_b_(pattern.getFrontTopLeft()).add(-1.5, 0.5, -1.5);
        portal.setPosition(center.x, center.y, center.z);
        
        portal.destination = new Vector3d(0, 120, 0);
        
        portal.dimensionTo = World.field_234920_i_;
        
        portal.axisW = new Vector3d(0, 0, 1);
        portal.axisH = new Vector3d(1, 0, 0);
        
        portal.width = 3;
        portal.height = 3;
        
        world.addEntity(portal);
    }
    
    @Override
    public void onEntityTeleportedOnServer(Entity entity) {
        if (shouldAddSlowFalling(entity)) {
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
    
    private boolean shouldAddSlowFalling(Entity entity) {
        if (entity instanceof LivingEntity) {
            if (entity instanceof ServerPlayerEntity) {
                ServerPlayerEntity player = (ServerPlayerEntity) entity;
                if (player.interactionManager.getGameType() == GameType.CREATIVE) {
                    return false;
                }
                if (player.getItemStackFromSlot(EquipmentSlotType.CHEST).getItem() == Items.ELYTRA) {
                    return false;
                }
            }
            return true;
        }
        else {
            return false;
        }
    }
    
    private void generateObsidianPlatform() {
        ServerWorld endWorld = McHelper.getServer().getWorld(World.field_234920_i_);
        
        ServerWorld.func_241121_a_(endWorld);
    }
}
