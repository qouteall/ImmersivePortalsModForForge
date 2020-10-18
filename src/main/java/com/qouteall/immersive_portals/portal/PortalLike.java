package com.qouteall.immersive_portals.portal;

import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public interface PortalLike {
    boolean isConventionalPortal();
    
    // bounding box
    AxisAlignedBB getAreaBox();
    
    Vector3d getOriginPos();
    
    Vector3d getDestPos();
    
    World getOriginWorld();
    
    World getDestWorld();
    
    boolean isRoughlyVisibleTo(Vector3d cameraPos);
    
    Vector3d getContentDirection();
    
    @Nullable
    Quaternion getRotation();
    
    double getScale();
    
    boolean getIsMirror();
    
    boolean getIsGlobal();
    
    // used for advanced frustum culling
    @Nullable
    Vector3d[] getAggressiveAreaVertices();
    
    // used for super advanced frustum culling
    @Nullable
    Vector3d[] getConservativeAreaVertices();
    
    void renderViewAreaMesh(Vector3d posInPlayerCoordinate, Consumer<Vector3d> vertexOutput);
}
