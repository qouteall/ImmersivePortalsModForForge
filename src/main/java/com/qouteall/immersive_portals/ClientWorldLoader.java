package com.qouteall.immersive_portals;

import com.mojang.authlib.GameProfile;
import com.qouteall.immersive_portals.chunk_loading.DimensionalChunkPos;
import com.qouteall.immersive_portals.dimension_sync.DimensionTypeSync;
import com.qouteall.immersive_portals.ducks.IECamera;
import com.qouteall.immersive_portals.ducks.IEClientPlayNetworkHandler;
import com.qouteall.immersive_portals.ducks.IEClientWorld;
import com.qouteall.immersive_portals.ducks.IEParticleManager;
import com.qouteall.immersive_portals.ducks.IEWorld;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.context_management.DimensionRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketDirection;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.world.DimensionType;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.commons.lang3.Validate;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@OnlyIn(Dist.CLIENT)
public class ClientWorldLoader {
    public final Map<RegistryKey<World>, ClientWorld> clientWorldMap = new HashMap<>();
    public final Map<RegistryKey<World>, WorldRenderer> worldRendererMap = new HashMap<>();
    public final Map<RegistryKey<World>, DimensionRenderHelper> renderHelperMap = new HashMap<>();
    private Set<DimensionalChunkPos> unloadedChunks = new HashSet<>();
    
    private static final Minecraft client = Minecraft.getInstance();
    
    private boolean isInitialized = false;
    
    private boolean isLoadingFakedWorld = false;
    
    private boolean isHardCore = false;
    
    public boolean isClientRemoteTicking = false;
    
    public int ticksSinceEnteringWorld = 0;
    
    public ClientWorldLoader() {
        ModMain.postClientTickSignal.connectWithWeakRef(this, ClientWorldLoader::tick);
    }
    
    public boolean getIsLoadingFakedWorld() {
        return isLoadingFakedWorld;
    }
    
    private void tick() {
        if (CGlobal.isClientRemoteTickingEnabled) {
            isClientRemoteTicking = true;
            clientWorldMap.values().forEach(world -> {
                if (client.world != world) {
                    tickRemoteWorld(world);
                }
            });
            worldRendererMap.values().forEach(worldRenderer -> {
                if (worldRenderer != client.worldRenderer) {
                    worldRenderer.tick();
                }
            });
            isClientRemoteTicking = false;
        }
        
        boolean lightmapTextureConflict = false;
        for (DimensionRenderHelper helper : renderHelperMap.values()) {
            helper.tick();
            if (helper.world != client.world) {
                if (helper.lightmapTexture == client.gameRenderer.getLightTexture()) {
                    Helper.err(String.format(
                        "Lightmap Texture Conflict %s %s",
                        helper.world.func_234923_W_(),
                        client.world.func_234923_W_()
                    ));
                    lightmapTextureConflict = true;
                }
            }
        }
        if (lightmapTextureConflict) {
            renderHelperMap.values().forEach(DimensionRenderHelper::cleanUp);
            renderHelperMap.clear();
            Helper.log("Refreshed Lightmaps");
        }
        ticksSinceEnteringWorld++;
    }
    
    private static int reportedErrorNum = 0;
    
    private void tickRemoteWorld(ClientWorld newWorld) {
        List<Portal> nearbyPortals = CHelper.getClientNearbyPortals(10).collect(Collectors.toList());
        
        ClientWorld oldWorld = client.world;
        
        client.world = newWorld;
        ((IEParticleManager) client.particles).mySetWorld(newWorld);
        
        try {
            newWorld.tickEntities();
            newWorld.tick(() -> true);
            
            if (!client.isGamePaused()) {
                tickRemoteWorldRandomTicksClient(newWorld, nearbyPortals);
            }
        }
        catch (Throwable e) {
            if (reportedErrorNum < 200) {
                e.printStackTrace();
                reportedErrorNum++;
            }
        }
        finally {
            client.world = oldWorld;
            ((IEParticleManager) client.particles).mySetWorld(oldWorld);
        }
    }
    
    // show nether particles through portal
    private static void tickRemoteWorldRandomTicksClient(
        ClientWorld newWorld, List<Portal> nearbyPortals
    ) {
        nearbyPortals.stream().filter(
            portal -> portal.dimensionTo == newWorld.func_234923_W_()
        ).findFirst().ifPresent(portal -> {
            Vector3d playerPos = client.player.getPositionVec();
            Vector3d center = portal.transformPoint(playerPos);
            
            ActiveRenderInfo camera = client.gameRenderer.getActiveRenderInfo();
            Vector3d oldCameraPos = camera.getProjectedView();
            
            ((IECamera) camera).portal_setPos(center);
            
            newWorld.animateTick(
                (int) center.x, (int) center.y, (int) center.z
            );
            
            ((IECamera) camera).portal_setPos(oldCameraPos);
        });
    }
    
    public void cleanUp() {
        worldRendererMap.values().forEach(
            worldRenderer -> worldRenderer.setWorldAndLoadRenderers(null)
        );
        
        clientWorldMap.clear();
        worldRendererMap.clear();
        
        renderHelperMap.values().forEach(DimensionRenderHelper::cleanUp);
        renderHelperMap.clear();
        
        isInitialized = false;
        
        ModMain.clientTaskList.forceClearTasks();
        
        ticksSinceEnteringWorld = 0;
    }
    
    //@Nullable
    public WorldRenderer getWorldRenderer(RegistryKey<World> dimension) {
        initializeIfNeeded();
        
        return worldRendererMap.get(dimension);
    }
    
    //Create world if missing
    public ClientWorld getWorld(RegistryKey<World> dimension) {
        Validate.notNull(dimension);
        
        initializeIfNeeded();
        
        if (!clientWorldMap.containsKey(dimension)) {
            return createFakedClientWorld(dimension);
        }
        
        return clientWorldMap.get(dimension);
    }
    
    public DimensionRenderHelper getDimensionRenderHelper(RegistryKey<World> dimension) {
        initializeIfNeeded();
        
        DimensionRenderHelper result = renderHelperMap.computeIfAbsent(
            dimension,
            dimensionType -> {
                return new DimensionRenderHelper(
                    getWorld(dimension)
                );
            }
        );
        
        Validate.isTrue(result.world.func_234923_W_() == dimension);
        
        return result;
    }
    
    private void initializeIfNeeded() {
        if (!isInitialized) {
            Validate.isTrue(client.world != null);
            Validate.isTrue(client.worldRenderer != null);
            
            Validate.notNull(client.player);
            Validate.isTrue(client.player.world == client.world);
            
            RegistryKey<World> playerDimension = client.world.func_234923_W_();
            clientWorldMap.put(playerDimension, client.world);
            worldRendererMap.put(playerDimension, client.worldRenderer);
            renderHelperMap.put(
                client.world.func_234923_W_(),
                new DimensionRenderHelper(client.world)
            );
            
            isHardCore = client.world.getWorldInfo().isHardcore();
            
            isInitialized = true;
        }
    }
    
    //fool minecraft using the faked world
    private ClientWorld createFakedClientWorld(RegistryKey<World> dimension) {
        Validate.isTrue(client.player.world.func_234923_W_() != dimension);
        
        isLoadingFakedWorld = true;
        
        client.getProfiler().startSection("create_world");
        
        int chunkLoadDistance = 3;// my own chunk manager doesn't need it
        
        WorldRenderer worldRenderer = new WorldRenderer(client, client.getRenderTypeBuffers());
        
        ClientWorld newWorld;
        try {
            ClientPlayNetHandler newNetworkHandler = new ClientPlayNetHandler(
                client,
                new ChatScreen("You should not be seeing me. I'm just a faked screen."),
                new NetworkManager(PacketDirection.CLIENTBOUND),
                new GameProfile(null, "faked_profiler_id")
            );
            //multiple net handlers share the same playerListEntries object
            ClientPlayNetHandler mainNetHandler = client.player.connection;
            ((IEClientPlayNetworkHandler) newNetworkHandler).setPlayerListEntries(
                ((IEClientPlayNetworkHandler) mainNetHandler).getPlayerListEntries()
            );
            RegistryKey<DimensionType> dimensionTypeKey =
                DimensionTypeSync.getDimensionTypeKey(dimension);
            ClientWorld.ClientWorldInfo currentProperty =
                (ClientWorld.ClientWorldInfo) ((IEWorld) client.world).myGetProperties();
            DynamicRegistries dimensionTracker = mainNetHandler.func_239165_n_();
            ((IEClientPlayNetworkHandler) newNetworkHandler).portal_setRegistryManager(
                dimensionTracker);
            DimensionType dimensionType = dimensionTracker
                .func_230520_a_().func_230516_a_(dimensionTypeKey);
            
            ClientWorld.ClientWorldInfo properties = new ClientWorld.ClientWorldInfo(
                currentProperty.getDifficulty(),
                currentProperty.isHardcore(),
                currentProperty.func_239159_f_() < 1.0
            );
            newWorld = new ClientWorld(
                newNetworkHandler,
                properties,
                dimension,
                dimensionType,
                chunkLoadDistance,
                () -> client.getProfiler(),
                worldRenderer,
                client.world.func_234925_Z_(),
                client.world.getBiomeManager().seed
            );
        }
        catch (Exception e) {
            throw new IllegalStateException(
                "Creating Faked World " + dimension + " " + clientWorldMap.keySet(),
                e
            );
        }
        
        worldRenderer.setWorldAndLoadRenderers(newWorld);
        
        worldRenderer.onResourceManagerReload(client.getResourceManager());
        
        ((IEClientPlayNetworkHandler) ((IEClientWorld) newWorld).getNetHandler())
            .setWorld(newWorld);
        
        clientWorldMap.put(dimension, newWorld);
        worldRendererMap.put(dimension, worldRenderer);
        
        Helper.log("Faked World Created " + dimension.func_240901_a_());
        
        isLoadingFakedWorld = false;
        
        client.getProfiler().endSection();
        
        return newWorld;
    }
    
}
