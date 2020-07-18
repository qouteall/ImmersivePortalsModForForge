package com.qouteall.immersive_portals.render.context_management;

import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.ducks.IECamera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class FogRendererContext {
    public float red;
    public float green;
    public float blue;
    public int waterFogColor = -1;
    public int nextWaterFogColor = -1;
    public long lastWaterFogColorUpdateTime = -1L;
    
    public static Consumer<FogRendererContext> copyContextFromObject;
    public static Consumer<FogRendererContext> copyContextToObject;
    public static Supplier<Vector3d> getCurrentFogColor;
    
    public static StaticFieldsSwappingManager<FogRendererContext> swappingManager;
    
    public static void init() {
        //load the class and apply mixin
        FogRenderer.class.hashCode();
        
        swappingManager = new StaticFieldsSwappingManager<>(
            copyContextFromObject, copyContextToObject
        );
        
    }
    
    public static void update() {
        swappingManager.setOuterDimension(RenderStates.originalPlayerDimension);
        swappingManager.resetChecks();
        CGlobal.clientWorldLoader.clientWorldMap.keySet().forEach(dimension ->
            swappingManager.contextMap.computeIfAbsent(
                dimension,
                k -> new StaticFieldsSwappingManager.ContextRecord<>(
                    dimension,
                    new FogRendererContext(),
                    dimension != RenderStates.originalPlayerDimension
                )
            )
        );
    }
    
    public static Vector3d getFogColorOf(
        ClientWorld destWorld, Vector3d pos
    ) {
        Minecraft client = Minecraft.getInstance();
        ClientWorld oldWorld = client.world;
        
        RegistryKey<World> newWorldKey = destWorld.func_234923_W_();
        
        swappingManager.contextMap.computeIfAbsent(
            newWorldKey,
            k -> new StaticFieldsSwappingManager.ContextRecord<>(
                k, new FogRendererContext(), true
            )
        );
        
        swappingManager.pushSwapping(newWorldKey);
        client.world = destWorld;
        
        ActiveRenderInfo newCamera = new ActiveRenderInfo();
        ((IECamera) newCamera).portal_setPos(pos);
        ((IECamera) newCamera).portal_setFocusedEntity(client.renderViewEntity);
        
        try {
            FogRenderer.updateFogColor(
                newCamera,
                RenderStates.tickDelta,
                destWorld,
                client.gameSettings.renderDistanceChunks,
                client.gameRenderer.getBossColorModifier(RenderStates.tickDelta)
            );
            
            Vector3d result = getCurrentFogColor.get();
            
            return result;
        }
        finally {
            swappingManager.popSwapping();
            client.world = oldWorld;
        }
    }
    
    public static void onPlayerTeleport(RegistryKey<World> from, RegistryKey<World> to) {
        swappingManager.updateOuterDimensionAndChangeContext(to);
    }
    
}
