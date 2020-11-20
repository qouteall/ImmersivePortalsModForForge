package com.qouteall.immersive_portals.chunk_loading;

import com.qouteall.hiding_in_the_bushes.O_O;
import com.qouteall.immersive_portals.ClientWorldLoader;
import com.qouteall.immersive_portals.render.context_management.RenderDimensionRedirect;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import net.minecraft.client.multiplayer.ClientChunkProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.LightType;
import net.minecraft.world.biome.BiomeContainer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.lighting.WorldLightManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.List;

// allow storing chunks that are far away from the player
@OnlyIn(Dist.CLIENT)
public class MyClientChunkManager extends ClientChunkProvider {
    private static final Logger LOGGER = LogManager.getLogger();
    protected final Chunk emptyChunk;
    protected final WorldLightManager lightingProvider;
    protected final ClientWorld world;
    
    protected final Long2ObjectLinkedOpenHashMap<Chunk> chunkMap =
        new Long2ObjectLinkedOpenHashMap<>();
    
    public MyClientChunkManager(ClientWorld clientWorld, int loadDistance) {
        super(clientWorld, loadDistance);
        this.world = clientWorld;
        this.emptyChunk = new EmptyChunk(clientWorld, new ChunkPos(0, 0));
        this.lightingProvider = new WorldLightManager(
            this,
            true,
            RenderDimensionRedirect.hasSkylight(clientWorld)
        );
        
    }
    
    @Override
    public WorldLightManager getLightManager() {
        return this.lightingProvider;
    }
    
    @Override
    public void unloadChunk(int x, int z) {
        synchronized (chunkMap) {
            
            ChunkPos chunkPos = new ChunkPos(x, z);
            Chunk chunk = chunkMap.get(chunkPos.asLong());
            if (positionEquals(chunk, x, z)) {
                chunkMap.remove(chunkPos.asLong());
                O_O.postClientChunkUnloadEvent(chunk);
                world.onChunkUnloaded(chunk);
            }
        }
    }
    
    @Override
    public Chunk getChunk(int x, int z, ChunkStatus chunkStatus, boolean create) {
        // the profiler shows that this is not a hot spot
        synchronized (chunkMap) {
            Chunk chunk = chunkMap.get(ChunkPos.asLong(x, z));
            if (positionEquals(chunk, x, z)) {
                return chunk;
            }
            
            return create ? this.emptyChunk : null;
        }
    }
    
    @Override
    public IBlockReader getWorld() {
        return this.world;
    }
    
    @Override
    public Chunk loadChunk(
        int x,
        int z,
        BiomeContainer biomeArray,
        PacketBuffer packetByteBuf,
        CompoundNBT compoundTag,
        int mask,
        boolean forceCreate
    ) {
        long chunkPosLong = ChunkPos.asLong(x, z);
        
        Chunk worldChunk;
        synchronized (chunkMap) {
            worldChunk = (Chunk) this.chunkMap.get(chunkPosLong);
            if (!forceCreate && positionEquals(worldChunk, x, z)) {
                worldChunk.read(biomeArray, packetByteBuf, compoundTag, mask);
            }
            else {
                if (biomeArray == null) {
                    LOGGER.error(
                        "Missing Biome Array: {} {} {} Client Biome May be Incorrect",
                        world.func_234923_W_().func_240901_a_(), x, z
                    );
                    throw new RuntimeException("Null biome array");
                }
                
                worldChunk = new Chunk(this.world, new ChunkPos(x, z), biomeArray);
                worldChunk.read(biomeArray, packetByteBuf, compoundTag, mask);
                chunkMap.put(chunkPosLong, worldChunk);
            }
        }
        
        ChunkSection[] chunkSections = worldChunk.getSections();
        WorldLightManager lightingProvider = this.getLightManager();
        lightingProvider.enableLightSources(new ChunkPos(x, z), true);
        
        for (int cy = 0; cy < chunkSections.length; ++cy) {
            ChunkSection chunkSection = chunkSections[cy];
            lightingProvider.updateSectionStatus(
                SectionPos.of(x, cy, z),
                ChunkSection.isEmpty(chunkSection)
            );
        }
        
        this.world.onChunkLoaded(x, z);
        
        O_O.postClientChunkLoadEvent(worldChunk);
        
        return worldChunk;
    }
    
    public static void updateLightStatus(Chunk chunk) {
        WorldLightManager lightingProvider = chunk.getWorld().getLightManager();
        ChunkSection[] chunkSections = chunk.getSections();
        for (int cy = 0; cy < chunkSections.length; ++cy) {
            ChunkSection chunkSection = chunkSections[cy];
            lightingProvider.updateSectionStatus(
                SectionPos.of(chunk.getPos().x, cy, chunk.getPos().z),
                ChunkSection.isEmpty(chunkSection)
            );
        }
    }
    
    public List<Chunk> getCopiedChunkList() {
        synchronized (chunkMap) {
            return Arrays.asList(chunkMap.values().toArray(new Chunk[0]));
        }
    }
    
    @Override
    public void setCenter(int x, int z) {
        //do nothing
    }
    
    @Override
    public void setViewDistance(int r) {
        //do nothing
    }
    
    @Override
    public String makeString() {
        return "Client Chunks (ImmPtl) " + getLoadedChunksCount();
    }
    
    @Override
    public int getLoadedChunksCount() {
        synchronized (chunkMap) {
            return chunkMap.size();
        }
    }
    
    @Override
    public void markLightChanged(LightType lightType, SectionPos chunkSectionPos) {
        ClientWorldLoader.getWorldRenderer(
            world.func_234923_W_()
        ).markForRerender(
            chunkSectionPos.getSectionX(),
            chunkSectionPos.getSectionY(),
            chunkSectionPos.getSectionZ()
        );
    }
    
    @Override
    public boolean canTick(BlockPos blockPos) {
        return this.chunkExists(blockPos.getX() >> 4, blockPos.getZ() >> 4);
    }
    
    @Override
    public boolean isChunkLoaded(ChunkPos chunkPos) {
        return this.chunkExists(chunkPos.x, chunkPos.z);
    }
    
    @Override
    public boolean isChunkLoaded(Entity entity) {
        return this.chunkExists(
            MathHelper.floor(entity.getPosX()) >> 4,
            MathHelper.floor(entity.getPosZ()) >> 4
        );
    }
    
    protected static boolean positionEquals(Chunk worldChunk, int x, int z) {
        if (worldChunk == null) {
            return false;
        }
        else {
            ChunkPos chunkPos = worldChunk.getPos();
            return chunkPos.x == x && chunkPos.z == z;
        }
    }
    
}
