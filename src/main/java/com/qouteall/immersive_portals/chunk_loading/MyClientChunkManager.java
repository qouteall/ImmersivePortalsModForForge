package com.qouteall.immersive_portals.chunk_loading;

import com.qouteall.immersive_portals.my_util.Helper;
import net.minecraft.client.Minecraft;
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
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.lighting.WorldLightManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

//this class is modified based on ClientChunkManager
//re-write this class upon updating mod
@OnlyIn(Dist.CLIENT)
public class MyClientChunkManager extends ClientChunkProvider {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Chunk emptyChunk;
    private final WorldLightManager lightingProvider;
    private final ClientWorld world;
    
    //its performance is a little lower than vanilla
    //but this is indispensable
    private ConcurrentHashMap<ChunkPos, Chunk> chunkMap = new ConcurrentHashMap<>();
    
    public MyClientChunkManager(ClientWorld clientWorld_1, int int_1) {
        super(clientWorld_1, int_1);
        this.world = clientWorld_1;
        this.emptyChunk = new EmptyChunk(clientWorld_1, new ChunkPos(0, 0));
        this.lightingProvider = new WorldLightManager(
            this,
            true,
            clientWorld_1.getDimension().hasSkyLight()
        );
    }
    
    @Override
    public WorldLightManager getLightManager() {
        return this.lightingProvider;
    }
    
    private static boolean isChunkValid(Chunk worldChunk_1, int int_1, int int_2) {
        if (worldChunk_1 == null) {
            return false;
        }
        else {
            ChunkPos chunkPos_1 = worldChunk_1.getPos();
            return chunkPos_1.x == int_1 && chunkPos_1.z == int_2;
        }
    }
    
    @Override
    public void unloadChunk(int int_1, int int_2) {
        ChunkPos chunkPos = new ChunkPos(int_1, int_2);
        Chunk chunk = chunkMap.get(chunkPos);
        if (isChunkValid(chunk, int_1, int_2)) {
            chunkMap.remove(chunkPos);
            world.onChunkUnloaded(chunk);
        }
    }
    
    @Override
    public IBlockReader getWorld() {
        return this.world;
    }
    
    //@Nullable
    public Chunk loadChunkFromPacket(
        World world_1,
        int x,
        int z,
        PacketBuffer packetByteBuf_1,
        CompoundNBT compoundTag_1,
        int mask,
        boolean isFullChunk
    ) {
        ChunkPos chunkPos = new ChunkPos(x, z);
        Chunk chunk = chunkMap.get(chunkPos);
        if (!isChunkValid(chunk, x, z)) {
            if (!isFullChunk) {
                LOGGER.warn(
                    "Ignoring chunk since we don't have complete data: {}, {}",
                    x,
                    z
                );
                return null;
            }
            
            chunk = new Chunk(
                world_1,
                new ChunkPos(x, z),
                new Biome[256]
            );
            chunk.read(packetByteBuf_1, compoundTag_1, mask, isFullChunk);
            chunkMap.put(chunkPos, chunk);
    
            world.onChunkUnloaded(chunk);//TODO wrong?
        }
        else {
            if (isFullChunk) {
                Helper.log(String.format(
                    "received full chunk while chunk is present. entity may duplicate %s %s",
                    chunk.getWorld().dimension.getType(),
                    chunk.getPos()
                ));
            }
            chunk.read(packetByteBuf_1, compoundTag_1, mask, isFullChunk);
        }
    
        ChunkSection[] chunkSections_1 = chunk.getSections();
        WorldLightManager lightingProvider_1 = this.getLightManager();
        lightingProvider_1.func_215571_a(new ChunkPos(x, z), true);
        
        for (int int_5 = 0; int_5 < chunkSections_1.length; ++int_5) {
            ChunkSection chunkSection_1 = chunkSections_1[int_5];
            lightingProvider_1.updateSectionStatus(
                SectionPos.of(x, int_5, z),
                ChunkSection.isEmpty(chunkSection_1)
            );
        }
        
        return chunk;
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
    
    private static int getLoadDistance(int int_1) {
        return Math.max(2, int_1) + 3;
    }
    
    @Override
    public String makeString() {
        return "Hacked Client Chunk Manager " + chunkMap.size();
    }
    
    @Override
    public ChunkGenerator<?> getChunkGenerator() {
        return null;
    }
    
    @Override
    public void markLightChanged(LightType lightType_1, SectionPos chunkSectionPos_1) {
        Minecraft.getInstance().worldRenderer.markForRerender(
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
            MathHelper.floor(entity_1.posX) >> 4,
            MathHelper.floor(entity_1.posZ) >> 4
        );
    }
    
    // $FF: synthetic method
    //@Nullable
    @Override
    public Chunk getChunk(int var1, int var2, ChunkStatus var3, boolean var4) {
        Chunk worldChunk_1 = chunkMap.get(new ChunkPos(var1, var2));
        if (isChunkValid(worldChunk_1, var1, var2)) {
            return worldChunk_1;
        }
        
        return var4 ? this.emptyChunk : null;
    }
    
    public int getChunkNum() {
        return chunkMap.size();
    }
    
}
