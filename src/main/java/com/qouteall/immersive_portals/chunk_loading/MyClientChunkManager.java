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
import java.util.function.BooleanSupplier;

//this class is modified based on ClientChunkManager
//re-write this class upon updating mod
@OnlyIn(Dist.CLIENT)
public class MyClientChunkManager extends ClientChunkProvider {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Chunk emptyChunk;
    private final WorldLightManager lightingProvider;
    private final ClientWorld world;
    
    private final Long2ObjectLinkedOpenHashMap<Chunk> chunkMapNew = new Long2ObjectLinkedOpenHashMap<>();
    
    public MyClientChunkManager(ClientWorld clientWorld, int int_1) {
        super(clientWorld, int_1);
        this.world = clientWorld;
        this.emptyChunk = new EmptyChunk(clientWorld, new ChunkPos(0, 0));
        this.lightingProvider = new WorldLightManager(
            this,
            true,
            RenderDimensionRedirect.hasSkylight(clientWorld.dimension)
        );
        
    }
    
    @Override
    public WorldLightManager getLightManager() {
        return this.lightingProvider;
    }
    
    @Override
    public void unloadChunk(int int_1, int int_2) {
        synchronized (chunkMapNew) {
            
            ChunkPos chunkPos = new ChunkPos(int_1, int_2);
            Chunk worldChunk_1 = chunkMapNew.get(chunkPos.asLong());
            if (positionEquals(worldChunk_1, int_1, int_2)) {
                chunkMapNew.remove(chunkPos.asLong());
                O_O.postChunkUnloadEventForge(worldChunk_1);
            }
        }
    }
    
    @Override
    public Chunk getChunk(int int_1, int int_2, ChunkStatus chunkStatus_1, boolean boolean_1) {
        synchronized (chunkMapNew) {
            Chunk worldChunk_1 = chunkMapNew.get(ChunkPos.asLong(int_1, int_2));
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
        BiomeContainer biomeArray_1,
        PacketBuffer packetByteBuf_1,
        CompoundNBT compoundTag_1,
        int sections
    ) {
        ChunkPos chunkPos = new ChunkPos(x, z);
        Chunk worldChunk_1;
        
        synchronized (chunkMapNew) {
            worldChunk_1 = (Chunk) chunkMapNew.get(chunkPos.asLong());
            if (!positionEquals(worldChunk_1, x, z)) {
                if (biomeArray_1 == null) {
                    LOGGER.warn(
                        "Ignoring chunk since we don't have complete data: {}, {}",
                        x,
                        z
                    );
                    return null;
                }
                
                worldChunk_1 = new Chunk(this.world, chunkPos, biomeArray_1);
                worldChunk_1.read(biomeArray_1, packetByteBuf_1, compoundTag_1, sections);
                chunkMapNew.put(chunkPos.asLong(), worldChunk_1);
            }
            else {
                worldChunk_1.read(biomeArray_1, packetByteBuf_1, compoundTag_1, sections);
            }
        }
        
        ChunkSection[] chunkSections = worldChunk_1.getSections();
        WorldLightManager lightingProvider = this.getLightManager();
        lightingProvider.enableLightSources(chunkPos, true);
        
        for (int i = 0; i < chunkSections.length; ++i) {
            ChunkSection chunkSection_1 = chunkSections[i];
            lightingProvider.updateSectionStatus(
                SectionPos.of(x, i, z),
                ChunkSection.isEmpty(chunkSection_1)
            );
        }
        
        this.world.onChunkLoaded(x, z);
        
        O_O.postChunkLoadEventForge(worldChunk_1);
        
        return worldChunk_1;
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
        synchronized (chunkMapNew) {
            return Arrays.asList(chunkMapNew.values().toArray(new Chunk[0]));
        }
    }
    
    @Override
    public void tick(BooleanSupplier booleanSupplier_1) {
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
        synchronized (chunkMapNew) {
            return chunkMapNew.size();
        }
    }
    
    @Override
    public void markLightChanged(LightType lightType_1, SectionPos chunkSectionPos_1) {
        CGlobal.clientWorldLoader.getWorldRenderer(
            world.dimension.getType()
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
