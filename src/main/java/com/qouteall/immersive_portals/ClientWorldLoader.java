package com.qouteall.immersive_portals;

import com.mojang.authlib.GameProfile;
import com.qouteall.immersive_portals.ducks.IEClientPlayNetworkHandler;
import com.qouteall.immersive_portals.ducks.IEClientWorld;
import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.render.DimensionRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketDirection;
import net.minecraft.world.GameType;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.Validate;

import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class ClientWorldLoader {
    public Map<DimensionType, ClientWorld> clientWorldMap = new HashMap<>();
    public Map<DimensionType, WorldRenderer> worldRendererMap = new HashMap<>();
    public Map<DimensionType, DimensionRenderHelper> renderHelperMap = new HashMap<>();
    
    private Minecraft mc = Minecraft.getInstance();
    
    private boolean isInitialized = false;
    
    private boolean isLoadingFakedWorld = false;
    
    private boolean isHardCore = false;
    
    public ClientWorldLoader() {
        ModMain.postClientTickSignal.connectWithWeakRef(this, ClientWorldLoader::tick);
    }
    
    public boolean getIsLoadingFakedWorld() {
        return isLoadingFakedWorld;
    }
    
    private void tick() {
        if (CGlobal.isClientRemoteTickingEnabled) {
            clientWorldMap.values().forEach(world -> {
                if (mc.world != world) {
                    //NOTE tick() does not include ticking entities
                    world.tickEntities();
                    world.tick(() -> true);
                }
            });
        }
        renderHelperMap.values().forEach(DimensionRenderHelper::tick);
        
    }
    
    public void cleanUp() {
        worldRendererMap.values().forEach(
            worldRenderer -> worldRenderer.setWorldAndLoadRenderers(null)
        );
        
        clientWorldMap.clear();
        worldRendererMap.clear();
        renderHelperMap.clear();
        
        isInitialized = false;
    }
    
    //@Nullable
    public ClientWorld getWorld(DimensionType dimension) {
        initializeIfNeeded();
        
        return clientWorldMap.get(dimension);
    }
    
    //@Nullable
    public WorldRenderer getWorldRenderer(DimensionType dimension) {
        initializeIfNeeded();
        
        return worldRendererMap.get(dimension);
    }
    
    public ClientWorld getOrCreateFakedWorld(DimensionType dimension) {
        Validate.notNull(dimension);
        
        initializeIfNeeded();
        
        if (!clientWorldMap.containsKey(dimension)) {
            return createFakedClientWorld(dimension);
        }
    
        return getWorld(dimension);
    }
    
    public DimensionRenderHelper getDimensionRenderHelper(DimensionType dimension) {
        initializeIfNeeded();
        
        DimensionRenderHelper result = renderHelperMap.computeIfAbsent(
            dimension,
            dimensionType -> {
                return new DimensionRenderHelper(
                    getOrCreateFakedWorld(dimension)
                );
            }
        );
        assert result.world.dimension.getType() == dimension;
        return result;
    }
    
    private void initializeIfNeeded() {
        if (!isInitialized) {
            assert (mc.world != null);
            assert (mc.worldRenderer != null);
            
            DimensionType playerDimension = mc.world.getDimension().getType();
            clientWorldMap.put(playerDimension, mc.world);
            worldRendererMap.put(playerDimension, mc.worldRenderer);
            renderHelperMap.put(
                mc.world.dimension.getType(),
                new DimensionRenderHelper(mc.world)
            );
            
            isHardCore = mc.world.getWorldInfo().isHardcore();
            
            isInitialized = true;
        }
    }
    
    private ClientWorld createFakedClientWorld(DimensionType dimension) {
        assert mc.world.dimension.getType() == mc.player.dimension;
        assert (mc.player.dimension != dimension);
        
        isLoadingFakedWorld = true;
    
        //OFHelper.onBeginCreatingFakedWorld();
        
        //TODO get load distance
        int chunkLoadDistance = 3;
        
        WorldRenderer worldRenderer = new WorldRenderer(mc);
    
        ClientWorld newWorld;
        try {
            ClientPlayNetHandler newNetworkHandler = new ClientPlayNetHandler(
                mc,
                new ChatScreen("You should not be seeing me. I'm just a faked screen."),
                new NetworkManager(PacketDirection.CLIENTBOUND),
                new GameProfile(null, "faked_profiler_id")
            );
            //multiple net handlers share the same playerListEntries object
            ((IEClientPlayNetworkHandler) newNetworkHandler).setPlayerListEntries(
                ((IEClientPlayNetworkHandler) mc.player.connection).getPlayerListEntries()
            );
            newWorld = new ClientWorld(
                newNetworkHandler,
                new WorldSettings(
                    0L,
                    GameType.CREATIVE,
                    true,
                    isHardCore,
                    WorldType.FLAT
                ),
                dimension,
                chunkLoadDistance,
                mc.getProfiler(),
                worldRenderer
            );
        }
        catch (Exception e) {
            throw new IllegalStateException(
                "Creating Faked World " + dimension + " " + clientWorldMap.keySet(),
                e
            );
        }
    
        worldRenderer.setWorldAndLoadRenderers(newWorld);
        
        worldRenderer.onResourceManagerReload(mc.getResourceManager());
        
        ((IEClientPlayNetworkHandler) ((IEClientWorld) newWorld).getNetHandler())
            .setWorld(newWorld);
        
        clientWorldMap.put(dimension, newWorld);
        worldRendererMap.put(dimension, worldRenderer);
        
        Helper.log("Faked World Created " + dimension);
        
        isLoadingFakedWorld = false;
    
        //OFHelper.onFinishCreatingFakedWorld();
        
        return newWorld;
    }
    
}
