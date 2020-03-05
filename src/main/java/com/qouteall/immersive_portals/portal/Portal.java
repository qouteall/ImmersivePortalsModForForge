package com.qouteall.immersive_portals.portal;

import com.qouteall.hiding_in_the_bushes.MyNetwork;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.my_util.SignalArged;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.util.Direction;
import net.minecraft.util.Tuple;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import java.util.UUID;

public class Portal extends Entity {
    public static EntityType<Portal> entityType;
    
    //basic properties
    public double width = 0;
    public double height = 0;
    public Vec3d axisW;
    public Vec3d axisH;
    public DimensionType dimensionTo;
    public Vec3d destination;
    
    //additional properteis
    public boolean loadFewerChunks = true;
    public boolean teleportable = true;
    public UUID specificPlayer;
    public SpecialPortalShape specialShape;
    
    private AxisAlignedBB boundingBoxCache;
    private Vec3d normal;
    
    public static final SignalArged<Portal> clientPortalTickSignal = new SignalArged<>();
    public static final SignalArged<Portal> serverPortalTickSignal = new SignalArged<>();
    
    public Portal(
        EntityType<?> entityType_1,
        World world_1
    ) {
        super(entityType_1, world_1);
    }
    
    @Override
    protected void registerData() {
        //do nothing
    }
    
    @Override
    protected void readAdditional(CompoundNBT compoundTag) {
        width = compoundTag.getDouble("width");
        height = compoundTag.getDouble("height");
        axisW = Helper.getVec3d(compoundTag, "axisW").normalize();
        axisH = Helper.getVec3d(compoundTag, "axisH").normalize();
        dimensionTo = DimensionType.getById(compoundTag.getInt("dimensionTo"));
        destination = Helper.getVec3d(compoundTag, "destination");
        if (compoundTag.contains("loadFewerChunks")) {
            loadFewerChunks = compoundTag.getBoolean("loadFewerChunks");
        }
        else {
            loadFewerChunks = true;
        }
        if (compoundTag.contains("specificPlayer")) {
            specificPlayer = compoundTag.getUniqueId("specificPlayer");
        }
        if (compoundTag.contains("specialShape")) {
            specialShape = new SpecialPortalShape(
                compoundTag.getList("specialShape", 6)
            );
            if (specialShape.triangles.isEmpty()) {
                specialShape = null;
            }
        }
        if (compoundTag.contains("teleportable")) {
            teleportable = compoundTag.getBoolean("teleportable");
        }
    }
    
    public boolean isTeleportable() {
        return teleportable;
    }
    
    @Override
    protected void writeAdditional(CompoundNBT compoundTag) {
        compoundTag.putDouble("width", width);
        compoundTag.putDouble("height", height);
        Helper.putVec3d(compoundTag, "axisW", axisW);
        Helper.putVec3d(compoundTag, "axisH", axisH);
        compoundTag.putInt("dimensionTo", dimensionTo.getId());
        Helper.putVec3d(compoundTag, "destination", destination);
        compoundTag.putBoolean("loadFewerChunks", loadFewerChunks);
    
        if (specificPlayer != null) {
            compoundTag.putUniqueId("specificPlayer", specificPlayer);
        }
    
        if (specialShape != null) {
            compoundTag.put("specialShape", specialShape.writeToTag());
        }
    
        compoundTag.putBoolean("teleportable", teleportable);
    }
    
    @Override
    public IPacket<?> createSpawnPacket() {
        return MyNetwork.createStcSpawnEntity(this);
    }
    
    @Override
    public void tick() {
//        if (boundingBoxCache == null) {
//            boundingBoxCache = getPortalCollisionBox();
//        }
//        setBoundingBox(boundingBoxCache);
    
        if (world.isRemote) {
            clientPortalTickSignal.emit(this);
            tickClient();
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
    
    @Override
    public AxisAlignedBB getBoundingBox() {
        if (axisW == null) {
            //avoid npe with pehkui
            //pehkui will invoke this when axisW is not initialized
            boundingBoxCache = null;
            return new AxisAlignedBB(0, 0, 0, 0, 0, 0);
        }
        if (boundingBoxCache == null) {
            boundingBoxCache = getPortalCollisionBox();
        }
        return boundingBoxCache;
    }
    
    @Override
    public void move(MoverType type, Vec3d movement) {
        //portal cannot be moved
    }
    
    @OnlyIn(Dist.CLIENT)
    private void tickClient() {
        ClientPlayerEntity player = Minecraft.getInstance().player;
        if (player != null) {
            if (!canBeSeenByPlayer(player)) {
                //removed in client but not in server
                remove();
            }
        }
    }
    
    public boolean isPortalValid() {
        return dimensionTo != null &&
            width != 0 &&
            height != 0 &&
            axisW != null &&
            axisH != null &&
            destination != null;
    }
    
    public boolean canRenderPortalInsideMe(Portal anotherPortal) {
        if (anotherPortal.dimension != dimensionTo) {
            return false;
        }
        return canRenderEntityInsideMe(anotherPortal.getPositionVec(), 0.5);
    }
    
    public boolean canRenderEntityInsideMe(Vec3d entityPos, double valve) {
        double v = entityPos.subtract(destination).dotProduct(getContentDirection());
        return v > valve;
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
    
        Vec3d wAxisVec = new Vec3d(Helper.getUnitFromAxis(wAxis));
        Vec3d hAxisVec = new Vec3d(Helper.getUnitFromAxis(hAxis));
    
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
    
        portals[0].width = width;
        portals[1].width = width;
        portals[2].width = width;
        portals[3].width = width;
    
        portals[0].height = height;
        portals[1].height = height;
        portals[2].height = height;
        portals[3].height = height;
    }
    
    public boolean shouldEntityTeleport(Entity entity) {
        return entity.dimension == this.dimension &&
            isTeleportable() &&
            isMovedThroughPortal(
                entity.getEyePosition(0),
                entity.getEyePosition(1)
            );
    }
    
    public void onEntityTeleportedOnServer(Entity entity) {
        //nothing
    }
    
    public boolean canBeSeenByPlayer(PlayerEntity player) {
        if (specificPlayer == null) {
            return true;
        }
        else {
            return specificPlayer.equals(player.getUniqueID());
        }
    }
    
    @Override
    public String toString() {
        return String.format(
            "%s{%s,%s,(%s %s %s %s)->(%s %s %s %s)}",
            getClass().getSimpleName(),
            getEntityId(),
            Direction.getFacingFromVector(
                getNormal().x, getNormal().y, getNormal().z
            ),
            dimension, (int) getPosX(), (int) getPosY(), (int) getPosZ(),
            dimensionTo, (int) destination.x, (int) destination.y, (int) destination.z
        );
    }
    
    //Geometry----------
    
    public Vec3d getNormal() {
        if (normal == null)
            normal = axisW.crossProduct(axisH).normalize();
        return normal;
    }
    
    public Vec3d getContentDirection() {
        return getNormal().scale(-1);
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
        vertices[0] = getPointInPlane(width / 2 - shrinkFactor, -height / 2 + shrinkFactor);
        vertices[1] = getPointInPlane(-width / 2 + shrinkFactor, -height / 2 + shrinkFactor);
        vertices[2] = getPointInPlane(width / 2 - shrinkFactor, height / 2 - shrinkFactor);
        vertices[3] = getPointInPlane(-width / 2 + shrinkFactor, height / 2 - shrinkFactor);
        
        return vertices;
    }
    
    //3  2
    //1  0
    public Vec3d[] getFourVerticesRelativeToCenter(double shrinkFactor) {
        Vec3d[] vertices = new Vec3d[4];
        vertices[0] = getPointInPlaneRelativeToCenter(
            width / 2 - shrinkFactor,
            -height / 2 + shrinkFactor
        );
        vertices[1] = getPointInPlaneRelativeToCenter(
            -width / 2 + shrinkFactor,
            -height / 2 + shrinkFactor
        );
        vertices[2] = getPointInPlaneRelativeToCenter(
            width / 2 - shrinkFactor,
            height / 2 - shrinkFactor
        );
        vertices[3] = getPointInPlaneRelativeToCenter(
            -width / 2 + shrinkFactor,
            height / 2 - shrinkFactor
        );
    
        return vertices;
    }
    
    public Vec3d applyTransformationToPoint(Vec3d pos) {
        Vec3d offset = destination.subtract(getPositionVec());
        return pos.add(offset);
    }
    
    public Vec3d reverseTransformPoint(Vec3d pos) {
        Vec3d offset = destination.subtract(getPositionVec());
        return pos.subtract(offset);
    }
    
    public Vec3d getCullingPoint() {
        return destination;
    }
    
    private AxisAlignedBB getPortalCollisionBox() {
        return new AxisAlignedBB(
            getPointInPlane(width / 2, height / 2)
                .add(getNormal().scale(0.2)),
            getPointInPlane(-width / 2, -height / 2)
                .add(getNormal().scale(-0.2))
        );
    }
    
    public boolean isPointInPortalProjection(Vec3d pos) {
        Vec3d offset = pos.subtract(getPositionVec());
    
        double yInPlane = offset.dotProduct(axisH);
        double xInPlane = offset.dotProduct(axisW);
    
        boolean roughResult = Math.abs(xInPlane) < (width / 2 + 0.1) &&
            Math.abs(yInPlane) < (height / 2 + 0.1);
    
        if (roughResult && specialShape != null) {
            return specialShape.triangles.stream()
                .anyMatch(triangle ->
                    triangle.isPointInTriangle(xInPlane, yInPlane)
                );
        }
    
        return roughResult;
    }
    
    public boolean isMovedThroughPortal(
        Vec3d lastTickPos,
        Vec3d pos
    ) {
        return pick(lastTickPos, pos) != null;
    }
    
    public Vec3d pick(
        Vec3d from,
        Vec3d to
    ) {
        double lastDistance = getDistanceToPlane(from);
        double nowDistance = getDistanceToPlane(to);
        
        if (!(lastDistance > 0 && nowDistance < 0)) {
            return null;
        }
        
        Vec3d lineOrigin = from;
        Vec3d lineDirection = to.subtract(from).normalize();
    
        double collidingT = Helper.getCollidingT(
            getPositionVec(),
            normal,
            lineOrigin,
            lineDirection
        );
        Vec3d collidingPoint = lineOrigin.add(lineDirection.scale(collidingT));
        
        if (isPointInPortalProjection(collidingPoint)) {
            return collidingPoint;
        }
        else {
            return null;
        }
    }
    
    public double getDistanceToNearestPointInPortal(
        Vec3d point
    ) {
        double distanceToPlane = getDistanceToPlane(point);
        Vec3d posInPlane = point.add(getNormal().scale(-distanceToPlane));
        Vec3d localPos = posInPlane.subtract(getPositionVec());
        double localX = localPos.dotProduct(axisW);
        double localY = localPos.dotProduct(axisH);
        double distanceToRect = getDistanceToRectangle(
            localX, localY,
            -(width / 2), -(height / 2),
            (width / 2), (height / 2)
        );
        return Math.sqrt(distanceToPlane * distanceToPlane + distanceToRect * distanceToRect);
    }
    
    public static double getDistanceToRectangle(
        double pointX, double pointY,
        double rectAX, double rectAY,
        double rectBX, double rectBY
    ) {
        assert rectAX <= rectBX;
        assert rectAY <= rectBY;
        
        double wx1 = rectAX - pointX;
        double wx2 = rectBX - pointX;
        double dx = (wx1 * wx2 < 0 ? 0 : Math.min(Math.abs(wx1), Math.abs(wx2)));
    
        double wy1 = rectAY - pointY;
        double wy2 = rectBY - pointY;
        double dy = (wy1 * wy2 < 0 ? 0 : Math.min(Math.abs(wy1), Math.abs(wy2)));
    
        return Math.sqrt(dx * dx + dy * dy);
    }
    
    public Vec3d getPointInPortalProjection(Vec3d pos) {
        Vec3d myPos = getPositionVec();
        Vec3d offset = pos.subtract(myPos);
        
        double yInPlane = offset.dotProduct(axisH);
        double xInPlane = offset.dotProduct(axisW);
        
        return myPos.add(
            axisW.scale(xInPlane)
        ).add(
            axisH.scale(yInPlane)
        );
    }
}
