package com.qouteall.immersive_portals.portal;

import com.qouteall.immersive_portals.MyNetwork;
import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.my_util.SignalArged;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityClassification;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;

import java.util.stream.Stream;

public class Portal extends Entity implements IEntityAdditionalSpawnData {
    public static EntityType<Portal> entityType;
    
    public Vec3d axisW;
    public Vec3d axisH;
    private Vec3d normal;
    public DimensionType dimensionTo;
    public Vec3d destination;
    public boolean loadFewerChunks = true;
    
    public AxisAlignedBB boundingBoxCache;
    
    public static final SignalArged<Portal> clientPortalTickSignal = new SignalArged<>();
    public static final SignalArged<Portal> serverPortalTickSignal = new SignalArged<>();
    
    
    public Portal(
        EntityType<?> entityType_1,
        World world_1
    ) {
        super(entityType_1, world_1);
    }
    
    
    public Stream<Entity> getEntitiesToTeleport() {
        return world.getEntitiesWithinAABB(
            Entity.class,
            getPortalCollisionBox()
        ).stream().filter(
            e -> !(e instanceof Portal)
        ).filter(
            this::shouldEntityTeleport
        );
    }
    
    @Override
    protected void registerData() {
        //do nothing
    }
    
    @Override
    protected void readAdditional(CompoundNBT compoundTag) {
        axisW = Helper.getVec3d(compoundTag, "axisW");
        axisH = Helper.getVec3d(compoundTag, "axisH");
        dimensionTo = DimensionType.getById(compoundTag.getInt("dimensionTo"));
        destination = Helper.getVec3d(compoundTag, "destination");
        if (compoundTag.contains("loadFewerChunks")) {
            loadFewerChunks = compoundTag.getBoolean("loadFewerChunks");
        }
        else {
            loadFewerChunks = true;
        }
    }
    
    public Vec3d getNormal() {
        if (normal == null)
            normal = axisW.crossProduct(axisH).normalize();
        return normal;
    }
    
    public boolean isTeleportable() {
        return true;
    }
    
    public Vec3d getContentDirection() {
        return getNormal().scale(-1);
    }
    
    @Override
    protected void writeAdditional(CompoundNBT compoundTag) {
        Helper.putVec3d(compoundTag, "axisW", axisW);
        Helper.putVec3d(compoundTag, "axisH", axisH);
        compoundTag.putInt("dimensionTo", dimensionTo.getId());
        Helper.putVec3d(compoundTag, "destination", destination);
        compoundTag.putBoolean("loadFewerChunks", loadFewerChunks);
    }
    
    @Override
    public IPacket<?> createSpawnPacket() {
        return MyNetwork.createStcSpawnEntity(this);
    }
    
    @Override
    public void tick() {
        if (boundingBoxCache == null) {
            boundingBoxCache = getPortalCollisionBox();
        }
        setBoundingBox(boundingBoxCache);
        
        if (world.isRemote) {
            clientPortalTickSignal.emit(this);
        }
        else {
            if (!isPortalValid()) {
                Helper.log("removed invalid portal" + this);
                removed = true;
                return;
            }
            serverPortalTickSignal.emit(this);
        }
    }
    
    
    public boolean isPortalValid() {
        return dimensionTo != null &&
            axisW != null &&
            axisH != null &&
            destination != null;
    }
    
    public double getDistanceToPlane(
        Vec3d pos
    ) {
        return pos.subtract(getPositionVec()).dotProduct(getNormal());
    }
    
    public boolean isInFrontOfPortal(
        Vec3d playerPos
    ) {
        return getDistanceToPlane(playerPos) > 0;
    }
    
    public boolean canRenderPortalInsideMe(Portal anotherPortal) {
        if (anotherPortal.dimension != dimensionTo) {
            return false;
        }
        double v = anotherPortal.getPositionVec().subtract(destination).dotProduct(
            getContentDirection());
        return v > 0.5;
    }
    
    public boolean canRenderEntityInsideMe(Vec3d entityPos) {
        double v = entityPos.subtract(destination).dotProduct(getContentDirection());
        return v > 0;
    }
    
    public Vec3d getPointInPlane(double xInPlane, double yInPlane) {
        return getPositionVec().add(getPointInPlaneRelativeToCenter(xInPlane, yInPlane));
    }
    
    public Vec3d getPointInPlaneRelativeToCenter(double xInPlane, double yInPlane) {
        return axisW.scale(xInPlane).add(axisH.scale(yInPlane));
    }
    
    //3  2
    //1  0
    public Vec3d[] getFourVertices(double shrinkFactor) {
        Vec3d[] vertices = new Vec3d[4];
        vertices[0] = getPointInPlane(1 - shrinkFactor, -1 + shrinkFactor);
        vertices[1] = getPointInPlane(-1 + shrinkFactor, -1 + shrinkFactor);
        vertices[2] = getPointInPlane(1 - shrinkFactor, 1 - shrinkFactor);
        vertices[3] = getPointInPlane(-1 + shrinkFactor, 1 - shrinkFactor);
        
        return vertices;
    }
    
    //3  2
    //1  0
    public Vec3d[] getFourVerticesRelativeToCenter(double shrinkFactor) {
        Vec3d[] vertices = new Vec3d[4];
        vertices[0] = getPointInPlaneRelativeToCenter(1 - shrinkFactor, -1 + shrinkFactor);
        vertices[1] = getPointInPlaneRelativeToCenter(-1 + shrinkFactor, -1 + shrinkFactor);
        vertices[2] = getPointInPlaneRelativeToCenter(1 - shrinkFactor, 1 - shrinkFactor);
        vertices[3] = getPointInPlaneRelativeToCenter(-1 + shrinkFactor, 1 - shrinkFactor);
        
        return vertices;
    }
    
    public Vec3d applyTransformationToPoint(Vec3d pos) {
        Vec3d offset = destination.subtract(getPositionVec());
        return pos.add(offset);
    }
    
    public Vec3d getCullingPoint() {
        return destination;
    }
    
    private AxisAlignedBB getPortalCollisionBox() {
        return new AxisAlignedBB(
            getPointInPlane(1, 1)
                .add(getNormal().scale(0.1)),
            getPointInPlane(-1, -1)
                .add(getNormal().scale(-0.1))
        );
    }
    
    @Override
    public String toString() {
        return "Portal{" +
            "id=" + getEntityId() +
            ", in=" + dimension + getPosition() +
            ", to=" + dimensionTo + new BlockPos(destination) +
            ", normal=" + new BlockPos(getNormal()) +
            '}';
    }
    
    //0 and 3 are connected
    //1 and 2 are connected
    //0 and 1 are in same dimension but facing opposite
    //2 and 3 are in same dimension but facing opposite
    public static void initBiWayBiFacedPortal(
        Portal[] portals,
        DimensionType dimension1,
        Vec3d center1,
        DimensionType dimension2,
        Vec3d center2,
        Direction.Axis normalAxis,
        Vec3d portalSize
    ) {
        Tuple<Direction.Axis, Direction.Axis> anotherTwoAxis = Helper.getAnotherTwoAxis(normalAxis);
        Direction.Axis wAxis = anotherTwoAxis.getA();
        Direction.Axis hAxis = anotherTwoAxis.getB();
        
        float width = (float) Helper.getCoordinate(portalSize, wAxis);
        float height = (float) Helper.getCoordinate(portalSize, hAxis);
        
        Vec3d wAxisVec = new Vec3d(Helper.getUnitFromAxis(wAxis)).scale(width);
        Vec3d hAxisVec = new Vec3d(Helper.getUnitFromAxis(hAxis)).scale(height);
        
        portals[0].setPosition(center1.x, center1.y, center1.z);
        portals[1].setPosition(center1.x, center1.y, center1.z);
        portals[2].setPosition(center2.x, center2.y, center2.z);
        portals[3].setPosition(center2.x, center2.y, center2.z);
        
        portals[0].destination = center2;
        portals[1].destination = center2;
        portals[2].destination = center1;
        portals[3].destination = center1;
        
        assert portals[0].dimension == dimension1;
        assert portals[1].dimension == dimension1;
        assert portals[2].dimension == dimension2;
        assert portals[3].dimension == dimension2;
        
        portals[0].dimensionTo = dimension2;
        portals[1].dimensionTo = dimension2;
        portals[2].dimensionTo = dimension1;
        portals[3].dimensionTo = dimension1;
        
        portals[0].axisW = wAxisVec;
        portals[1].axisW = wAxisVec.scale(-1);
        portals[2].axisW = wAxisVec;
        portals[3].axisW = wAxisVec.scale(-1);
        
        portals[0].axisH = hAxisVec;
        portals[1].axisH = hAxisVec;
        portals[2].axisH = hAxisVec;
        portals[3].axisH = hAxisVec;
    }
    
    public boolean shouldEntityTeleport(Entity entity) {
        float eyeHeight = entity.getEyeHeight();
        return entity.dimension == this.dimension &&
            isTeleportable() &&
            isMovedThroughPortal(
                Helper.lastTickPosOf(entity).add(0, eyeHeight, 0),
                entity.getPositionVec().add(0, eyeHeight, 0)
            );
    }
    
    public boolean isPointInPortalProjection(Vec3d pos) {
        Vec3d offset = pos.subtract(getPositionVec());
        
        double yInPlane = offset.dotProduct(axisH);
        double xInPlane = offset.dotProduct(axisW);
        
        return Math.abs(xInPlane) < (1 + 0.1) &&
            Math.abs(yInPlane) < (1 + 0.1);
        
    }
    
    public boolean isMovedThroughPortal(
        Vec3d lastTickPos,
        Vec3d pos
    ) {
        if (pos.squareDistanceTo(lastTickPos) > 5 * 5) {
            //entity moves to fast
            return false;
        }
        
        double lastDistance = getDistanceToPlane(lastTickPos);
        double nowDistance = getDistanceToPlane(pos);
        
        if (!(lastDistance > 0 && nowDistance < 0)) {
            return false;
        }
        
        Vec3d lineOrigin = lastTickPos;
        Vec3d lineDirection = pos.subtract(lastTickPos).normalize();
        
        double collidingT = Helper.getCollidingT(
            getPositionVec(),
            normal,
            lineOrigin,
            lineDirection
        );
        Vec3d collidingPoint = lineOrigin.add(lineDirection.scale(collidingT));
        
        return isPointInPortalProjection(collidingPoint);
    }
    
    public void onEntityTeleportedOnServer(Entity entity) {
        //nothing
    }
    
    @Override
    public void writeSpawnData(PacketBuffer buffer) {
        buffer.writeDouble(axisW.x);
        buffer.writeDouble(axisW.y);
        buffer.writeDouble(axisW.z);
        buffer.writeDouble(axisH.x);
        buffer.writeDouble(axisH.y);
        buffer.writeDouble(axisH.z);
        buffer.writeInt(dimensionTo.getId());
        buffer.writeDouble(destination.x);
        buffer.writeDouble(destination.y);
        buffer.writeDouble(destination.z);
        buffer.writeBoolean(loadFewerChunks);
    }
    
    @Override
    public void readSpawnData(PacketBuffer additionalData) {
        axisW = new Vec3d(
            additionalData.readDouble(),
            additionalData.readDouble(),
            additionalData.readDouble()
        );
        axisH = new Vec3d(
            additionalData.readDouble(),
            additionalData.readDouble(),
            additionalData.readDouble()
        );
        dimensionTo = DimensionType.getById(additionalData.readInt());
        destination = new Vec3d(
            additionalData.readDouble(),
            additionalData.readDouble(),
            additionalData.readDouble()
        );
        loadFewerChunks = additionalData.readBoolean();
        
    }
}
