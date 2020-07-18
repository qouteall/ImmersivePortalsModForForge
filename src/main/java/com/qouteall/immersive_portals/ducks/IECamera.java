package com.qouteall.immersive_portals.ducks;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.vector.Vector3d;

public interface IECamera {
    void resetState(Vector3d pos, ClientWorld currWorld);
    
    void portal_setPos(Vector3d pos);
    
    float getCameraY();
    
    float getLastCameraY();
    
    void setCameraY(float cameraY, float lastCameraY);
    
    void portal_setFocusedEntity(Entity arg);
}
