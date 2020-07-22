package com.qouteall.immersive_portals.chunk_loading;

import com.qouteall.hiding_in_the_bushes.O_O;
import com.qouteall.immersive_portals.CGlobal;
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
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeContainer;
import net.minecraft.world.biome.Biomes;
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
import java.util.stream.Stream;

//this class is modified based on ClientChunkManager
//re-write this class upon updating mod
@OnlyIn(Dist.CLIENT)
public class MyClientChunkManager extends ClientChunkProvider {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Chunk emptyChunk;
    private final WorldLightManager lightingProvider;
    private final ClientWorld world;
    
    private final Long2ObjectLinkedOpenHashMap<Chunk> chunkMap =
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
    public void unloadChunk(int int_1, int int_2) {
        synchronized (chunkMap) {
            
            ChunkPos chunkPos = new ChunkPos(int_1, int_2);
            Chunk worldChunk_1 = chunkMap.get(chunkPos.asLong());
            if (positionEquals(worldChunk_1, int_1, int_2)) {
                chunkMap.remove(chunkPos.asLong());
                O_O.postChunkUnloadEventForge(worldChunk_1);
            }
        }
    }
    
    @Override
    public Chunk getChunk(int int_1, int int_2, ChunkStatus chunkStatus_1, boolean boolean_1) {
        synchronized (chunkMap) {
            Chunk worldChunk_1 = chunkMap.get(ChunkPos.asLong(int_1, int_2));
            if (positionEquals(worldChunk_1, int_1, int_2)) {
                return worldChunk_1;
            }
            
            return boolean_1 ? this.emptyChunk : null;
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
        int k,
        boolean bl
    ) {
        long chunkPosLong = ChunkPos.asLong(x, z);
        
        Chunk worldChunk;
        synchronized (chunkMap) {
            worldChunk = (Chunk) this.chunkMap.get(chunkPosLong);
            if (!bl && positionEquals(worldChunk, x, z)) {
                worldChunk.read(biomeArray, packetByteBuf, compoundTag, k);
            }
            else {
                if (biomeArray == null) {
                    LOGGER.error(
                        "Missing Biome Array: {} {} {} Client Biome May be Incorrect",
                        world.func_234923_W_().func_240901_a_(), x, z
                    );
                    biomeArray = new BiomeContainer(
                        Stream.generate(() -> Biomes.PLAINS)
                            .limit(BiomeContainer.BIOMES_SIZE)
                            .toArray(Biome[]::new)
                    );
                }
                
                worldChunk = new Chunk(this.world, new ChunkPos(x, z), biomeArray);
                worldChunk.read(biomeArray, packetByteBuf, compoundTag, k);
                chunkMap.put(chunkPosLong, worldChunk);
            }
        }
        
        ChunkSection[] chunkSections = worldChunk.getSections();
        WorldLightManager lightingProvider = this.getLightManager();
        lightingProvider.enableLightSources(new ChunkPos(x, z), true);
        
        for (int m = 0; m < chunkSections.length; ++m) {
            ChunkSection chunkSection = chunkSections[m];
            lightingProvider.updateSectionStatus(
                SectionPos.of(x, m, z),
                ChunkSection.isEmpty(chunkSection)
            );
        }
        
        this.world.onChunkLoaded(x, z);
        
        O_O.postChunkLoadEventForge(worldChunk);
        
        return worldChunk;
    }
    
    public static void updateLightStatus(Chunk chunk) {
        ChunkSection[] chunkSections_1 = chunk.getSections();
        WorldLightManager lightingProvider = chunk.getWorld().getLightManager();
        for (int int_5 = 0; int_5 < chunkSections_1.length; ++int_5) {
            ChunkSection chunkSection_1 = chunkSections_1[int_5];
            lightingProvider.updateSectionStatus(
                SectionPos.of(chunk.getPos().x, int_5, chunk.getPos().z),
                ChunkSection.isEmpty(chunkSection_1)
            );
        }
    }
    
    public List<Chunk> getCopiedChunkList() {
        synchronized (chunkMap) {
            return Arrays.asList(chunkMap.values().toArray(new Chunk[0]));
        }
    }
    
    @Override
    public void setCenter(int int_1, int int_2) {
        //do nothing
    }
    
    @Override
    public void setViewDistance(int int_1) {
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
    public void markLightChanged(LightType lightType_1, SectionPos chunkSectionPos_1) {
        CGlobal.clientWorldLoader.getWorldRenderer(
            world.func_234923_W_()
        ).markForRerender(
            chunkSectionPos_1.getSectionX(),
            chunkSectionPos_1.getSectionY(),
            chunkSectionPos_1.getSectionZ()
        );
    }
    
    @Override
    public boolean canTick(BlockPos blockPos_1) {
        return this.chunkExists(blockPos_1.getX() >> 4, blockPos_1.getZ() >> 4);
    }
    
    @Override
    public boolean isChunkLoaded(ChunkPos chunkPos_1) {
        return this.chunkExists(chunkPos_1.x, chunkPos_1.z);
    }
    
    @Override
    public boolean isChunkLoaded(Entity entity_1) {
        return this.chunkExists(
            MathHelper.floor(entity_1.getPosX()) >> 4,
            MathHelper.floor(entity_1.getPosZ()) >> 4
        );
    }
    
    private static boolean positionEquals(Chunk worldChunk_1, int int_1, int int_2) {
        if (worldChunk_1 == null) {
            return false;
        }
        else {
            ChunkPos chunkPos_1 = worldChunk_1.getPos();
            return chunkPos_1.x == int_1 && chunkPos_1.z == int_2;
        }
    }
    
}
