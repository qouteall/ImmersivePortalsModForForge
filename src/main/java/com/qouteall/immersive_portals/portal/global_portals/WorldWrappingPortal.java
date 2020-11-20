package com.qouteall.immersive_portals.portal.global_portals;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.my_util.IntBox;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class WorldWrappingPortal extends GlobalTrackedPortal {
    public static EntityType<WorldWrappingPortal> entityType;
    
    public boolean isInward = true;
    public int zoneId = -1;
    
    public WorldWrappingPortal(
        EntityType<?> entityType_1,
        World world_1
    ) {
        super(entityType_1, world_1);
    }
    
    @Override
    protected void readAdditional(CompoundNBT compoundTag) {
        super.readAdditional(compoundTag);
        
        if (compoundTag.contains("isInward")) {
            isInward = compoundTag.getBoolean("isInward");
        }
        if (compoundTag.contains("zoneId")) {
            zoneId = compoundTag.getInt("zoneId");
        }
    }
    
    @Override
    protected void writeAdditional(CompoundNBT compoundTag) {
        super.writeAdditional(compoundTag);
        
        compoundTag.putBoolean("isInward", isInward);
        compoundTag.putInt("zoneId", zoneId);
    }
    
    private static WorldWrappingPortal createWrappingPortal(
        ServerWorld serverWorld,
        AxisAlignedBB area,
        Direction direction,
        int zoneId,
        boolean isInward
    ) {
        WorldWrappingPortal portal = entityType.create(serverWorld);
        portal.isInward = isInward;
        portal.zoneId = zoneId;
        
        initWrappingPortal(serverWorld, area, direction, isInward, portal);
        
        return portal;
    }
    
    public static void initWrappingPortal(
        ServerWorld serverWorld,
        AxisAlignedBB area,
        Direction direction,
        boolean isInward,
        Portal portal
    ) {
        Vector3d areaSize = Helper.getBoxSize(area);
        
        Tuple<Direction, Direction> axises = Helper.getPerpendicularDirections(
            isInward ? direction : direction.getOpposite()
        );
        AxisAlignedBB boxSurface = Helper.getBoxSurfaceInversed(area, direction);
        Vector3d center = boxSurface.getCenter();
        AxisAlignedBB oppositeSurface = Helper.getBoxSurfaceInversed(area, direction.getOpposite());
        Vector3d destination = oppositeSurface.getCenter();
        portal.setPosition(center.x, center.y, center.z);
        portal.setDestination(destination);
        
        portal.axisW = Vector3d.func_237491_b_(axises.getA().getDirectionVec());
        portal.axisH = Vector3d.func_237491_b_(axises.getB().getDirectionVec());
        portal.width = Helper.getCoordinate(areaSize, axises.getA().getAxis());
        portal.height = Helper.getCoordinate(areaSize, axises.getB().getAxis());
        
        portal.dimensionTo = serverWorld.func_234923_W_();
    }
    
    public static class WrappingZone {
        public ServerWorld world;
        public boolean isInwardZone;
        public int id;
        public List<WorldWrappingPortal> portals;
        
        public WrappingZone(
            ServerWorld world,
            boolean isInwardZone,
            int id,
            List<WorldWrappingPortal> portals
        ) {
            this.world = world;
            this.isInwardZone = isInwardZone;
            this.id = id;
            this.portals = portals;
        }
        
        public boolean isValid() {
            return (portals.size() == 4) &&
                (portals.get(0).isInward == isInwardZone) &&
                (portals.get(1).isInward == isInwardZone) &&
                (portals.get(2).isInward == isInwardZone) &&
                (portals.get(3).isInward == isInwardZone);
        }
        
        public void removeFromWorld() {
            GlobalPortalStorage gps = GlobalPortalStorage.get(world);
            portals.forEach(worldWrappingPortal -> gps.removePortal(worldWrappingPortal));
        }
        
        public AxisAlignedBB getArea() {
            return portals.stream().map(
                Portal::getThinAreaBox
            ).reduce(AxisAlignedBB::union).orElse(null);
        }
        
        public IntBox getIntArea() {
            AxisAlignedBB floatBox = getArea();
            
            return new IntBox(
                new BlockPos(
                    Math.round(floatBox.minX), 0, Math.round(floatBox.minZ)
                ),
                new BlockPos(
                    Math.round(floatBox.maxX) - 1, 256, Math.round(floatBox.maxZ) - 1
                )
            );
        }
        
        public IntBox getBorderBox() {
            
            if (!isInwardZone) {
                return getIntArea();
            }
            
            AxisAlignedBB floatBox = getArea();
            
            return new IntBox(
                new BlockPos(
                    Math.round(floatBox.minX) - 1, 0, Math.round(floatBox.minZ) - 1
                ),
                new BlockPos(
                    Math.round(floatBox.maxX), 256, Math.round(floatBox.maxZ)
                )
            );
        }
        
        @Override
        public String toString() {
            AxisAlignedBB area = getArea();
            return String.format(
                "[%d] %s %s %s ~ %s %s\n",
                id,
                isInwardZone ? "inward" : "outward",
                area.minX, area.minZ,
                area.maxX, area.maxZ
            );
        }
    }
    
    public static List<WrappingZone> getWrappingZones(ServerWorld world) {
        GlobalPortalStorage gps = GlobalPortalStorage.get(world);
        
        List<WrappingZone> result = new ArrayList<>();
        
        gps.data.stream()
            .filter(portal -> portal instanceof WorldWrappingPortal)
            .map(portal -> ((WorldWrappingPortal) portal))
            .collect(Collectors.groupingBy(
                portal -> portal.zoneId
            ))
            .forEach((zoneId, portals) -> {
                result.add(new WrappingZone(
                    world, portals.get(0).isInward,
                    zoneId, portals
                ));
            });
        
        return result;
    }
    
    public static int getAvailableId(List<WrappingZone> zones) {
        return zones.stream()
            .max(Comparator.comparingInt(z -> z.id))
            .map(z -> z.id + 1)
            .orElse(1);
    }
    
    public static void invokeAddWrappingZone(
        ServerWorld world,
        int x1, int z1, int x2, int z2,
        boolean isInward,
        Consumer<ITextComponent> feedbackSender
    ) {
        List<WrappingZone> wrappingZones = getWrappingZones(world);
        
        for (WrappingZone zone : wrappingZones) {
            if (!zone.isValid()) {
                feedbackSender.accept(new TranslationTextComponent(
                    "imm_ptl.removed_invalid_wrapping_portals",
                    Helper.myToString(zone.portals.stream())
                ));
                zone.removeFromWorld();
            }
        }
        
        int availableId = getAvailableId(wrappingZones);
    
        AxisAlignedBB box = new IntBox(new BlockPos(x1, 0, z1), new BlockPos(x2, 255, z2)).toRealNumberBox();
        
        WorldWrappingPortal p1 = createWrappingPortal(
            world, box, Direction.NORTH, availableId, isInward
        );
        WorldWrappingPortal p2 = createWrappingPortal(
            world, box, Direction.SOUTH, availableId, isInward
        );
        WorldWrappingPortal p3 = createWrappingPortal(
            world, box, Direction.WEST, availableId, isInward
        );
        WorldWrappingPortal p4 = createWrappingPortal(
            world, box, Direction.EAST, availableId, isInward
        );
        
        GlobalPortalStorage gps = GlobalPortalStorage.get(world);
        gps.addPortal(p1);
        gps.addPortal(p2);
        gps.addPortal(p3);
        gps.addPortal(p4);
    }
    
    public static void invokeViewWrappingZones(
        ServerWorld world,
        Consumer<ITextComponent> feedbackSender
    ) {
        List<WrappingZone> wrappingZones = getWrappingZones(world);
        
        wrappingZones.forEach(wrappingZone -> {
            feedbackSender.accept(new StringTextComponent(wrappingZone.toString()));
        });
    }
    
    public static void invokeRemoveWrappingZone(
        ServerWorld world,
        Vector3d playerPos,
        Consumer<ITextComponent> feedbackSender
    ) {
        List<WrappingZone> wrappingZones = getWrappingZones(world);
        
        WrappingZone zone = wrappingZones.stream()
            .filter(z -> z.getArea().contains(playerPos))
            .findFirst().orElse(null);
        
        if (zone != null) {
            zone.removeFromWorld();
            feedbackSender.accept(new TranslationTextComponent("imm_ptl.removed_portal", zone.toString()));
        }
        else {
            feedbackSender.accept(new TranslationTextComponent("imm_ptl.not_in_wrapping_zone"));
        }
    }
    
    public static void invokeRemoveWrappingZone(
        ServerWorld world,
        int zoneId,
        Consumer<ITextComponent> feedbackSender
    ) {
        List<WrappingZone> wrappingZones = getWrappingZones(world);
        
        WrappingZone zone = wrappingZones.stream()
            .filter(wrappingZone -> wrappingZone.id == zoneId)
            .findFirst().orElse(null);
        
        if (zone != null) {
            zone.removeFromWorld();
            feedbackSender.accept(new TranslationTextComponent("imm_ptl.removed_portal", zone.toString()));
        }
        else {
            feedbackSender.accept(new TranslationTextComponent("imm_ptl.cannot_find_zone"));
        }
    }
    
}
