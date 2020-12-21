package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ducks.IEWorldRendererChunkInfo;
import com.qouteall.immersive_portals.my_util.BoxPredicate;
import com.qouteall.immersive_portals.my_util.LimitedLogger;
import com.qouteall.immersive_portals.my_util.Plane;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.PortalLike;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.Validate;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@OnlyIn(Dist.CLIENT)
public class PortalRenderingGroup implements PortalLike {
    
    private static final LimitedLogger limitedLogger = new LimitedLogger(20);
    
    public final Portal.TransformationDesc transformationDesc;
    public final List<Portal> portals = new ArrayList<>();
    
    private AxisAlignedBB exactBoundingBox;
    private Vector3d origin;
    private Vector3d dest;
    
    @Nullable
    private AxisAlignedBB destAreaBoxCache = null;
    
    @Nullable
    private Boolean isEnclosedCache = null;
    
    private final UUID uuid = MathHelper.getRandomUUID();
    
    public PortalRenderingGroup(Portal.TransformationDesc transformationDesc) {
        this.transformationDesc = transformationDesc;
    }
    
    public void addPortal(Portal portal) {
        Validate.isTrue(portal.world.isRemote());
        Validate.isTrue(!portal.getIsGlobal());
        
        if (portals.contains(portal)) {
            limitedLogger.err("Adding duplicate portal into group " + portal);
            return;
        }
        
        portals.add(portal);
        updateCache();
    }
    
    public void removePortal(Portal portal) {
        portals.remove(portal);
        
        updateCache();
    }
    
    public void updateCache() {
        exactBoundingBox = null;
        origin = null;
        dest = null;
        destAreaBoxCache = null;
        isEnclosedCache = null;
    }
    
    public AxisAlignedBB getDestAreaBox() {
        if (destAreaBoxCache == null) {
            destAreaBoxCache = (
                Helper.transformBox(getExactAreaBox(), pos -> {
                    return portals.get(0).transformPoint(pos);
                })
            );
        }
        
        return destAreaBoxCache;
    }
    
    @Override
    public boolean isConventionalPortal() {
        return false;
    }
    
    @Override
    public AxisAlignedBB getExactAreaBox() {
        if (exactBoundingBox == null) {
            exactBoundingBox = portals.stream().map(
                Portal::getExactBoundingBox
            ).reduce(AxisAlignedBB::union).get();
        }
        return exactBoundingBox;
    }
    
    @Override
    public Vector3d transformPoint(Vector3d pos) {
        return portals.get(0).transformPoint(pos);
    }
    
    @Override
    public Vector3d transformLocalVec(Vector3d localVec) {
        return portals.get(0).transformLocalVec(localVec);
    }
    
    @Override
    public double getDistanceToNearestPointInPortal(Vector3d point) {
        return Helper.getDistanceToBox(getExactAreaBox(), point);
    }
    
    @Override
    public double getDestAreaRadiusEstimation() {
        double maxDimension = getSizeEstimation();
        return maxDimension * transformationDesc.scaling;
    }
    
    
    @Override
    public Vector3d getOriginPos() {
        if (origin == null) {
            origin = getExactAreaBox().getCenter();
        }
        
        return origin;
    }
    
    @Override
    public Vector3d getDestPos() {
        if (dest == null) {
            dest = transformPoint(getOriginPos());
        }
        
        return dest;
    }
    
    @Override
    public World getOriginWorld() {
        return portals.get(0).world;
    }
    
    @Override
    public World getDestWorld() {
        return portals.get(0).getDestWorld();
    }
    
    @Override
    public RegistryKey<World> getDestDim() {
        return portals.get(0).getDestDim();
    }
    
    @Override
    public boolean isRoughlyVisibleTo(Vector3d cameraPos) {
        return true;
    }
    
    @Nullable
    @Override
    public Plane getInnerClipping() {
        return null;
    }
    
    @Override
    public boolean isInside(Vector3d entityPos, double valve) {
        return getDestAreaBox().contains(entityPos);
    }
    
    @Nullable
    @Override
    public Quaternion getRotation() {
        return transformationDesc.rotation;
    }
    
    @Override
    public double getScale() {
        return transformationDesc.scaling;
    }
    
    @Override
    public boolean getIsGlobal() {
        return false;
    }
    
    @Nullable
    @Override
    public Vector3d[] getOuterFrustumCullingVertices() {
        return null;
    }
    
    @Override
    public void renderViewAreaMesh(Vector3d posInPlayerCoordinate, Consumer<Vector3d> vertexOutput) {
        for (Portal portal : portals) {
            Vector3d relativeToGroup = portal.getOriginPos().subtract(getOriginPos());
            portal.renderViewAreaMesh(
                posInPlayerCoordinate.add(relativeToGroup),
                vertexOutput
            );
        }
    }
    
    @Nullable
    @Override
    public Matrix4f getAdditionalCameraTransformation() {
        return portals.get(0).getAdditionalCameraTransformation();
    }
    
    @Nullable
    @Override
    public UUID getDiscriminator() {
        return uuid;
    }
    
    public void purge() {
        portals.removeIf(portal -> {
            return portal.removed;
        });
    }
    
    @Override
    public boolean isParallelWith(Portal portal) {
        return portals.stream().anyMatch(p -> p.isParallelWith(portal));
    }
    
    @OnlyIn(Dist.CLIENT)
    @Override
    public BoxPredicate getInnerFrustumCullingFunc(
        double cameraX, double cameraY, double cameraZ
    ) {
        Vector3d cameraPos = new Vector3d(cameraX, cameraY, cameraZ);
        
        List<BoxPredicate> funcs = portals.stream().filter(
            portal -> portal.isInFrontOfPortal(cameraPos)
        ).map(
            portal -> portal.getInnerFrustumCullingFunc(cameraX, cameraY, cameraZ)
        ).collect(Collectors.toList());
        
        return (minX, minY, minZ, maxX, maxY, maxZ) -> {
            // return true if all funcs return true
            for (BoxPredicate func : funcs) {
                if (!func.test(minX, minY, minZ, maxX, maxY, maxZ)) {
                    return false;
                }
            }
            return true;
        };
    }
    
    @OnlyIn(Dist.CLIENT)
    @Override
    public void doAdditionalRenderingCull(ObjectList<?> visibleChunks) {
        if (!isEnclosed()) {
            return;
        }
        
        //contract because the exact bounding box is a little bigger
        AxisAlignedBB enclosedDestAreaBox = getDestAreaBox().shrink(0.5);
        if (enclosedDestAreaBox != null) {
            Helper.removeIf(visibleChunks, (obj) -> {
                ChunkRenderDispatcher.ChunkRender builtChunk =
                    ((IEWorldRendererChunkInfo) obj).getBuiltChunk();

                return !builtChunk.boundingBox.intersects(enclosedDestAreaBox);
            });
        }
    }
    
    @Override
    public boolean isFuseView() {
        return portals.get(0).isFuseView();
    }
    
    @Override
    public String toString() {
        return String.format("PortalRenderingGroup(%s)%s", portals.size(), portals.get(0).portalTag);
    }
    
    public boolean isEnclosed(){
        if (isEnclosedCache == null) {
            isEnclosedCache = portals.stream().allMatch(
                p -> p.getOriginPos().subtract(getOriginPos()).dotProduct(p.getNormal()) > 0.3
            );
        }
    
        return isEnclosedCache;
    }
}
