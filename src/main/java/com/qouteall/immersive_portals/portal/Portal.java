package com.qouteall.immersive_portals.portal;

import com.qouteall.hiding_in_the_bushes.MyNetwork;
import com.qouteall.immersive_portals.CHelper;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.dimension_sync.DimId;
import com.qouteall.immersive_portals.my_util.SignalArged;
import com.qouteall.immersive_portals.portal.extension.PortalExtension;
import com.qouteall.immersive_portals.teleportation.CollisionHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.util.Direction;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import javax.annotation.Nullable;
import java.util.UUID;

/**
 * Portal entity. Global portals are also entities but not added into world.
 */
public class Portal extends Entity {
    public static EntityType<Portal> entityType;
    
    
    /**
     * The portal area length along axisW
     */
    public double width = 0;
    public double height = 0;
    
    /**
     * axisW and axisH define the orientation of the portal
     * They should be normalized and should be perpendicular to each other
     */
    public Vector3d axisW;
    public Vector3d axisH;
    
    /**
     * The destination dimension
     */
    public RegistryKey<World> dimensionTo;
    /**
     * The destination position
     */
    public Vector3d destination;
    
    /**
     * If false, cannot teleport entities
     */
    public boolean teleportable = true;
    /**
     * If not null, this portal can only be accessed by one player
     */
    @Nullable
    public UUID specificPlayerId;
    /**
     * If not null, defines the special shape of the portal
     * The shape should not exceed the area defined by width and height
     */
    @Nullable
    public GeometryPortalShape specialShape;
    
    private AxisAlignedBB boundingBoxCache;
    private Vector3d normal;
    private Vector3d contentDirection;
    
    /**
     * For advanced frustum culling
     */
    public double cullableXStart = 0;
    public double cullableXEnd = 0;
    public double cullableYStart = 0;
    public double cullableYEnd = 0;
    
    /**
     * The rotating transformation of the portal
     */
    @Nullable
    public Quaternion rotation;
    
    /**
     * The scaling transformation of the portal
     */
    public double scaling = 1.0;
    /**
     * Whether the entity scale changes after crossing the portal
     */
    public boolean teleportChangesScale = true;
    
    private boolean interactable = true;
    
    /**
     * Additional things
     */
    public PortalExtension extension = new PortalExtension();
    
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
        dimensionTo = DimId.getWorldId(compoundTag, "dimensionTo", world.isRemote);
        destination = Helper.getVec3d(compoundTag, "destination");
        if (compoundTag.contains("specificPlayer")) {
            specificPlayerId = Helper.getUuid(compoundTag, "specificPlayer");
        }
        if (compoundTag.contains("specialShape")) {
            specialShape = new GeometryPortalShape(
                compoundTag.getList("specialShape", 6)
            );
            if (specialShape.triangles.isEmpty()) {
                specialShape = null;
            }
        }
        if (compoundTag.contains("teleportable")) {
            teleportable = compoundTag.getBoolean("teleportable");
        }
        if (compoundTag.contains("cullableXStart")) {
            cullableXStart = compoundTag.getDouble("cullableXStart");
            cullableXEnd = compoundTag.getDouble("cullableXEnd");
            cullableYStart = compoundTag.getDouble("cullableYStart");
            cullableYEnd = compoundTag.getDouble("cullableYEnd");
        }
        else {
            if (specialShape != null) {
                cullableXStart = 0;
                cullableXEnd = 0;
                cullableYStart = 0;
                cullableYEnd = 0;
            }
            else {
                initDefaultCullableRange();
            }
        }
        if (compoundTag.contains("rotationA")) {
            rotation = new Quaternion(
                compoundTag.getFloat("rotationB"),
                compoundTag.getFloat("rotationC"),
                compoundTag.getFloat("rotationD"),
                compoundTag.getFloat("rotationA")
            );
        }
        
        if (compoundTag.contains("interactable")) {
            interactable = compoundTag.getBoolean("interactable");
        }
        
        if (compoundTag.contains("scale")) {
            scaling = compoundTag.getDouble("scale");
        }
        if (compoundTag.contains("teleportChangesScale")) {
            teleportChangesScale = compoundTag.getBoolean("teleportChangesScale");
        }
        
        extension = new PortalExtension();
        extension.readFromNbt(compoundTag);
    }
    
    @Override
    protected void writeAdditional(CompoundNBT compoundTag) {
        compoundTag.putDouble("width", width);
        compoundTag.putDouble("height", height);
        Helper.putVec3d(compoundTag, "axisW", axisW);
        Helper.putVec3d(compoundTag, "axisH", axisH);
        DimId.putWorldId(compoundTag, "dimensionTo", dimensionTo);
        Helper.putVec3d(compoundTag, "destination", destination);
        
        if (specificPlayerId != null) {
            Helper.putUuid(compoundTag, "specificPlayer", specificPlayerId);
        }
        
        if (specialShape != null) {
            compoundTag.put("specialShape", specialShape.writeToTag());
        }
        
        compoundTag.putBoolean("teleportable", teleportable);
        
        if (specialShape == null) {
            initDefaultCullableRange();
        }
        compoundTag.putDouble("cullableXStart", cullableXStart);
        compoundTag.putDouble("cullableXEnd", cullableXEnd);
        compoundTag.putDouble("cullableYStart", cullableYStart);
        compoundTag.putDouble("cullableYEnd", cullableYEnd);
        if (rotation != null) {
            compoundTag.putDouble("rotationA", rotation.getW());
            compoundTag.putDouble("rotationB", rotation.getX());
            compoundTag.putDouble("rotationC", rotation.getY());
            compoundTag.putDouble("rotationD", rotation.getZ());
        }
        
        compoundTag.putBoolean("interactable", interactable);
        
        compoundTag.putDouble("scale", scaling);
        compoundTag.putBoolean("teleportChangesScale", teleportChangesScale);
        
        extension.writeToNbt(compoundTag);
    }
    
    public boolean isCullable() {
        if (specialShape == null) {
            initDefaultCullableRange();
        }
        return cullableXStart != cullableXEnd;
    }
    
    public boolean isTeleportable() {
        return teleportable;
    }
    
    /**
     * Determines whether the player should be able to reach through the portal or not.
     * Can be overridden by a sub class.
     *
     * @return the interactability of the portal
     */
    public boolean isInteractable() {
        return interactable;
    }
    
    /**
     * Changes the reach-through behavior of the portal.
     *
     * @param interactable the interactability of the portal
     */
    public void setInteractable(boolean interactable) {
        this.interactable = interactable;
    }
    
    public void updateCache() {
        boundingBoxCache = null;
        normal = null;
        contentDirection = null;
        getBoundingBox();
        getNormal();
        getContentDirection();
    }
    
    public void initDefaultCullableRange() {
        cullableXStart = -(width / 2);
        cullableXEnd = (width / 2);
        cullableYStart = -(height / 2);
        cullableYEnd = (height / 2);
    }
    
    public void initCullableRange(
        double cullableXStart,
        double cullableXEnd,
        double cullableYStart,
        double cullableYEnd
    ) {
        this.cullableXStart = Math.min(cullableXStart, cullableXEnd);
        this.cullableXEnd = Math.max(cullableXStart, cullableXEnd);
        this.cullableYStart = Math.min(cullableYStart, cullableYEnd);
        this.cullableYEnd = Math.max(cullableYStart, cullableYEnd);
    }
    
    @Override
    public IPacket<?> createSpawnPacket() {
        return MyNetwork.createStcSpawnEntity(this);
    }
    
    @Override
    public boolean isSpectatedByPlayer(ServerPlayerEntity spectator) {
        if (specificPlayerId == null) {
            return true;
        }
        return spectator.getUniqueID().equals(specificPlayerId);
    }
    
    @Override
    public void tick() {
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
        extension.tick(this);
        
        CollisionHelper.notifyCollidingPortals(this);
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
    public void setBoundingBox(AxisAlignedBB boundingBox) {
        boundingBoxCache = null;
    }
    
    @Override
    public void move(MoverType type, Vector3d movement) {
        //portal cannot be moved
    }
    
    public boolean isPortalValid() {
        boolean valid = dimensionTo != null &&
            width != 0 &&
            height != 0 &&
            axisW != null &&
            axisH != null &&
            destination != null;
        if (valid) {
            if (world instanceof ServerWorld) {
                ServerWorld destWorld = McHelper.getServer().getWorld(dimensionTo);
                if (destWorld == null) {
                    Helper.err("Missing Dimension " + dimensionTo.func_240901_a_());
                    return false;
                }
            }
        }
        return valid;
    }
    
    public boolean isInside(Vector3d entityPos, double valve) {
        double v = entityPos.subtract(destination).dotProduct(getContentDirection());
        return v > valve;
    }
    
    public void onEntityTeleportedOnServer(Entity entity) {
        //nothing
    }
    
    @Override
    public String toString() {
        return String.format(
            "%s{%s,%s,(%s %s %s %s)->(%s %s %s %s)%s%s}",
            getClass().getSimpleName(),
            getEntityId(),
            Direction.getFacingFromVector(
                getNormal().x, getNormal().y, getNormal().z
            ),
            world.func_234923_W_().func_240901_a_(), (int) getPosX(), (int) getPosY(), (int) getPosZ(),
            dimensionTo.func_240901_a_(), (int) destination.x, (int) destination.y, (int) destination.z,
            specificPlayerId != null ? (",specificAccessor:" + specificPlayerId.toString()) : "",
            hasScaling() ? (",scale:" + scaling) : ""
        );
    }
    
    public boolean hasScaling() {
        return scaling != 1.0;
    }
    
    public Vector3d getNormal() {
        if (normal == null) {
            normal = axisW.crossProduct(axisH).normalize();
        }
        return normal;
    }
    
    public Vector3d getContentDirection() {
        if (contentDirection == null) {
            contentDirection = transformLocalVecNonScale(getNormal().scale(-1));
        }
        return contentDirection;
    }
    
    public double getDistanceToPlane(
        Vector3d pos
    ) {
        return pos.subtract(getPositionVec()).dotProduct(getNormal());
    }
    
    public boolean isInFrontOfPortal(
        Vector3d playerPos
    ) {
        return getDistanceToPlane(playerPos) > 0;
    }
    
    public Vector3d getPointInPlane(double xInPlane, double yInPlane) {
        return getPositionVec().add(getPointInPlaneLocal(xInPlane, yInPlane));
    }
    
    public Vector3d getPointInPlaneLocal(double xInPlane, double yInPlane) {
        return axisW.scale(xInPlane).add(axisH.scale(yInPlane));
    }
    
    public Vector3d getPointInPlaneLocalClamped(double xInPlane, double yInPlane) {
        return getPointInPlaneLocal(
            MathHelper.clamp(xInPlane, -width / 2, width / 2),
            MathHelper.clamp(yInPlane, -height / 2, height / 2)
        );
    }
    
    //3  2
    //1  0
    public Vector3d[] getFourVerticesLocal(double shrinkFactor) {
        Vector3d[] vertices = new Vector3d[4];
        vertices[0] = getPointInPlaneLocal(
            width / 2 - shrinkFactor,
            -height / 2 + shrinkFactor
        );
        vertices[1] = getPointInPlaneLocal(
            -width / 2 + shrinkFactor,
            -height / 2 + shrinkFactor
        );
        vertices[2] = getPointInPlaneLocal(
            width / 2 - shrinkFactor,
            height / 2 - shrinkFactor
        );
        vertices[3] = getPointInPlaneLocal(
            -width / 2 + shrinkFactor,
            height / 2 - shrinkFactor
        );
        
        return vertices;
    }
    
    //3  2
    //1  0
    public Vector3d[] getFourVerticesLocalRotated(double shrinkFactor) {
        Vector3d[] fourVerticesLocal = getFourVerticesLocal(shrinkFactor);
        fourVerticesLocal[0] = transformLocalVec(fourVerticesLocal[0]);
        fourVerticesLocal[1] = transformLocalVec(fourVerticesLocal[1]);
        fourVerticesLocal[2] = transformLocalVec(fourVerticesLocal[2]);
        fourVerticesLocal[3] = transformLocalVec(fourVerticesLocal[3]);
        return fourVerticesLocal;
    }
    
    //3  2
    //1  0
    public Vector3d[] getFourVerticesLocalCullable(double shrinkFactor) {
        Vector3d[] vertices = new Vector3d[4];
        vertices[0] = getPointInPlaneLocal(
            cullableXEnd - shrinkFactor,
            cullableYStart + shrinkFactor
        );
        vertices[1] = getPointInPlaneLocal(
            cullableXStart + shrinkFactor,
            cullableYStart + shrinkFactor
        );
        vertices[2] = getPointInPlaneLocal(
            cullableXEnd - shrinkFactor,
            cullableYEnd - shrinkFactor
        );
        vertices[3] = getPointInPlaneLocal(
            cullableXStart + shrinkFactor,
            cullableYEnd - shrinkFactor
        );
        
        return vertices;
    }
    
    //Server side does not have Matrix3f
    public final Vector3d transformPointRough(Vector3d pos) {
        Vector3d offset = destination.subtract(getPositionVec());
        return pos.add(offset);
    }
    
    public Vector3d transformPoint(Vector3d pos) {
        Vector3d localPos = pos.subtract(getPositionVec());
        
        Vector3d result = transformLocalVec(localPos).add(destination);
        
        return result;
        
    }
    
    public Vector3d transformLocalVecNonScale(Vector3d localVec) {
        if (rotation == null) {
            return localVec;
        }
        
        Vector3f temp = new Vector3f(localVec);
        temp.transform(rotation);
        
        return new Vector3d(temp);
    }
    
    public Vector3d transformLocalVec(Vector3d localVec) {
        return transformLocalVecNonScale(localVec).scale(scaling);
    }
    
    @Deprecated
    public Vector3d untransformLocalVec(Vector3d localVec) {
        if (rotation == null) {
            return localVec;
        }
        
        Vector3f temp = new Vector3f(localVec);
        Quaternion r = rotation.copy();
        r.conjugate();
        temp.transform(r);
        return new Vector3d(temp);
    }
    
    @Deprecated
    public Vector3d untransformPoint(Vector3d point) {
        return getPositionVec().add(untransformLocalVec(point.subtract(destination)));
    }
    
    public Vector3d scaleLocalVec(Vector3d localVec) {
        if (scaling == 1.0) {
            return localVec;
        }
        
        return localVec.scale(scaling);
    }
    
    public Vector3d getCullingPoint() {
        return destination;
    }
    
    private AxisAlignedBB getPortalCollisionBox() {
        return new AxisAlignedBB(
            getPointInPlane(width / 2, height / 2)
                .add(getNormal().scale(0.2)),
            getPointInPlane(-width / 2, -height / 2)
                .add(getNormal().scale(-0.2))
        ).union(new AxisAlignedBB(
            getPointInPlane(-width / 2, height / 2)
                .add(getNormal().scale(0.2)),
            getPointInPlane(width / 2, -height / 2)
                .add(getNormal().scale(-0.2))
        ));
    }
    
    public AxisAlignedBB getThinAreaBox() {
        return new AxisAlignedBB(
            getPointInPlane(width / 2, height / 2),
            getPointInPlane(-width / 2, -height / 2)
        );
    }
    
    public boolean isPointInPortalProjection(Vector3d pos) {
        Vector3d offset = pos.subtract(getPositionVec());
        
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
        Vector3d lastTickPos,
        Vector3d pos
    ) {
        return rayTrace(lastTickPos, pos) != null;
    }
    
    public Vector3d rayTrace(
        Vector3d from,
        Vector3d to
    ) {
        double lastDistance = getDistanceToPlane(from);
        double nowDistance = getDistanceToPlane(to);
        
        if (!(lastDistance > 0 && nowDistance < 0)) {
            return null;
        }
        
        Vector3d lineOrigin = from;
        Vector3d lineDirection = to.subtract(from).normalize();
        
        double collidingT = Helper.getCollidingT(getPositionVec(), normal, lineOrigin, lineDirection);
        Vector3d collidingPoint = lineOrigin.add(lineDirection.scale(collidingT));
        
        if (isPointInPortalProjection(collidingPoint)) {
            return collidingPoint;
        }
        else {
            return null;
        }
    }
    
    public double getDistanceToNearestPointInPortal(
        Vector3d point
    ) {
        double distanceToPlane = getDistanceToPlane(point);
        Vector3d posInPlane = point.add(getNormal().scale(-distanceToPlane));
        Vector3d localPos = posInPlane.subtract(getPositionVec());
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
    
    public Vector3d getPointInPortalProjection(Vector3d pos) {
        Vector3d myPos = getPositionVec();
        Vector3d offset = pos.subtract(myPos);
        
        double yInPlane = offset.dotProduct(axisH);
        double xInPlane = offset.dotProduct(axisW);
        
        return myPos.add(
            axisW.scale(xInPlane)
        ).add(
            axisH.scale(yInPlane)
        );
    }
    
    public World getDestinationWorld() {
        return getDestinationWorld(world.isRemote());
    }
    
    /**
     * @return The {@link World} of this portal's {@link #dimensionTo}.
     */
    public World getDestinationWorld(boolean isClient) {
        if (isClient) {
            return CHelper.getClientWorld(dimensionTo);
        }
        else {
            return McHelper.getServer().getWorld(dimensionTo);
        }
    }
    
    public static boolean isParallelPortal(Portal currPortal, Portal outerPortal) {
        if (currPortal.world.func_234923_W_() != outerPortal.dimensionTo) {
            return false;
        }
        if (currPortal.dimensionTo != outerPortal.world.func_234923_W_()) {
            return false;
        }
        if (currPortal.getNormal().dotProduct(outerPortal.getContentDirection()) > -0.9) {
            return false;
        }
        return !outerPortal.isInside(currPortal.getPositionVec(), 0.1);
    }
    
    public static boolean isReversePortal(Portal a, Portal b) {
        return a.dimensionTo == b.world.func_234923_W_() &&
            a.world.func_234923_W_() == b.dimensionTo &&
            a.getPositionVec().distanceTo(b.destination) < 1 &&
            a.destination.distanceTo(b.getPositionVec()) < 1 &&
            a.getNormal().dotProduct(b.getContentDirection()) > 0.5;
    }
    
    public static boolean isFlippedPortal(Portal a, Portal b) {
        if (a == b) {
            return false;
        }
        return a.world == b.world &&
            a.dimensionTo == b.dimensionTo &&
            a.getPositionVec().distanceTo(b.getPositionVec()) < 1 &&
            a.destination.distanceTo(b.destination) < 1 &&
            a.getNormal().dotProduct(b.getNormal()) < -0.5;
    }
    
}
