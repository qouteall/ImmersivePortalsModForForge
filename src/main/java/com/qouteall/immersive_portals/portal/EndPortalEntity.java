package com.qouteall.immersive_portals.portal;

import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ducks.IEEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.Items;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.GameType;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Objects;

public class EndPortalEntity extends Portal {
    public static EntityType<EndPortalEntity> entityType;
    
    // only used by scaled view type end portal
    private EndPortalEntity clientFakedReversePortal;
    
    public EndPortalEntity(
        EntityType<?> entityType_1,
        World world_1
    ) {
        super(entityType_1, world_1);
    
        renderingMergable = true;
        hasCrossPortalCollision = false;
    }
    
    public static void onEndPortalComplete(ServerWorld world, Vector3d portalCenter) {
        if (Global.endPortalMode == Global.EndPortalMode.normal) {
            generateClassicalEndPortal(world, new Vector3d(0, 120, 0), portalCenter);
        }
        else if (Global.endPortalMode == Global.EndPortalMode.toObsidianPlatform) {
            BlockPos endSpawnPos = ServerWorld.field_241108_a_;
            generateClassicalEndPortal(world,
                Vector3d.func_237489_a_(endSpawnPos).add(0, 1, 0), portalCenter
            );
        }
        else if (Global.endPortalMode == Global.EndPortalMode.scaledView) {
            generateScaledViewEndPortal(world, portalCenter);
        }
        else {
            Helper.err("End portal mode abnormal");
        }
        
        // for toObsidianPlatform mode, if the platform does not get generated before
        // going through portal, the player may fall into void
        generateObsidianPlatform();
    }
    
    private static void generateClassicalEndPortal(ServerWorld world, Vector3d destination, Vector3d portalCenter) {
        Portal portal = new EndPortalEntity(entityType, world);
        
        portal.setPosition(portalCenter.x, portalCenter.y, portalCenter.z);
        
        portal.setDestination(destination);
        
        portal.dimensionTo = World.field_234920_i_;
        
        portal.axisW = new Vector3d(0, 0, 1);
        portal.axisH = new Vector3d(1, 0, 0);
        
        portal.width = 3;
        portal.height = 3;
        
        world.addEntity(portal);
    }
    
    private static void generateScaledViewEndPortal(ServerWorld world, Vector3d portalCenter) {
        ServerWorld endWorld = McHelper.getServerWorld(World.field_234920_i_);
        
        double d = 3;
        final Vector3d viewBoxSize = new Vector3d(d, 1.2, d);
        final double scale = 280 / d;
        
        AxisAlignedBB thisSideBox = Helper.getBoxByBottomPosAndSize(
            portalCenter.add(0, 0, 0), viewBoxSize
        );
        AxisAlignedBB otherSideBox = Helper.getBoxByBottomPosAndSize(
            new Vector3d(0, 0, 0),
            viewBoxSize.scale(scale)
        );
        
        for (Direction direction : Direction.values()) {
            Portal portal = PortalManipulation.createOrthodoxPortal(
                EndPortalEntity.entityType,
                world, endWorld,
                direction, Helper.getBoxSurface(thisSideBox, direction),
                Helper.getBoxSurface(otherSideBox, direction).getCenter()
            );
            portal.scaling = scale;
            portal.teleportChangesScale = false;
            PortalExtension.get(portal).adjustPositionAfterTeleport = true;
            portal.portalTag = "view_box";
            //creating a new entity type needs registering
            //it's easier to discriminate it by portalTag
            
            McHelper.spawnServerEntity(portal);
        }
    }
    
    @Override
    public void tick() {
        super.tick();
        
        if (world.isRemote()) {
            tickClient();
        }
    }
    
    @OnlyIn(Dist.CLIENT)
    private void tickClient() {
        if (Objects.equals(portalTag, "view_box")) {
            ClientPlayerEntity player = Minecraft.getInstance().player;
            if (player == null) {
                return;
            }
            if (getNormal().y > 0.5) {
                if (((IEEntity) player).getCollidingPortal() == this) {
                    Vector3d cameraPosVec = player.getEyePosition(1);
                    double dist = this.getDistanceToNearestPointInPortal(cameraPosVec);
                    if (dist < 1) {
                        double mul = dist / 2 + 0.1;
                        player.setMotion(
                            player.getMotion().x * mul,
                            player.getMotion().y * mul,
                            player.getMotion().z * mul
                        );
                    }
                }
            }
            fuseView = true;
//            if (player.world == this.world && player.getPos().squaredDistanceTo(getOriginPos()) < 10 * 10) {
//                if (clientFakedReversePortal == null) {
//                    // client only faked portal
//                    clientFakedReversePortal =
//                        PortalManipulation.createReversePortal(this, EndPortalEntity.entityType);
//
//                    int newEntityId = -getEntityId();
//                    clientFakedReversePortal.setEntityId(newEntityId);
//
//                    clientFakedReversePortal.teleportable = false;
//
//                    clientFakedReversePortal.portalTag = "view_box_faked_reverse";
//
//                    clientFakedReversePortal.clientFakedReversePortal = this;
//
//                    ((ClientWorld) getDestinationWorld()).addEntity(
//                        clientFakedReversePortal.getEntityId(),
//                        clientFakedReversePortal
//                    );
//                }
//            }
//            else {
//                if (clientFakedReversePortal != null) {
//                    clientFakedReversePortal.remove();
//                    clientFakedReversePortal = null;
//                }
//            }
        }
        else if (Objects.equals(portalTag, "view_box_faked_reverse")) {
            if (clientFakedReversePortal.removed) {
                remove();
            }
        }
    }
    
    @Override
    public void onEntityTeleportedOnServer(Entity entity) {
        if (shouldAddSlowFalling(entity)) {
            int duration = 120;
            
            if (Objects.equals(this.portalTag, "view_box")) {
                duration = 200;
            }
            
            LivingEntity livingEntity = (LivingEntity) entity;
            livingEntity.addPotionEffect(
                new EffectInstance(
                    Effects.SLOW_FALLING,
                    duration,//duration
                    1//amplifier
                )
            );
        }
        if (entity instanceof ServerPlayerEntity) {
            generateObsidianPlatform();
        }
    }
    
    @Override
    public void transformVelocity(Entity entity) {
    
    }
    
    // arrows cannot go through end portal
    // avoid easily snipping end crystals
    @Override
    public boolean canTeleportEntity(Entity entity) {
        if (entity instanceof ArrowEntity) {
            return false;
        }
        return super.canTeleportEntity(entity);
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
    
    // if the bounding box is too small
    // grouping will fail
    @Override
    public boolean shouldLimitBoundingBox() {
        return false;
    }
    
    private static void generateObsidianPlatform() {
        ServerWorld endWorld = McHelper.getServer().getWorld(World.field_234920_i_);
        
        ServerWorld.func_241121_a_(endWorld);
    }
}
