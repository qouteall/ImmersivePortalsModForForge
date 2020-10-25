package com.qouteall.immersive_portals.ducks;

import net.minecraft.world.storage.ISpawnWorldInfo;

public interface IEWorld {
    
    ISpawnWorldInfo myGetProperties();
    
    void portal_setWeather(float rainGradPrev, float rainGrad, float thunderGradPrev, float thunderGrad);
}
