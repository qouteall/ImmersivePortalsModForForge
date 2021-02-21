package com.qouteall.immersive_portals.render;

import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.ducks.IEBuiltChunk;
import com.qouteall.immersive_portals.miscellaneous.GcMonitor;
import com.qouteall.immersive_portals.my_util.ObjectBuffer;
import com.qouteall.immersive_portals.optifine_compatibility.OFBuiltChunkStorageFix;
import com.qouteall.immersive_portals.render.context_management.PortalRendering;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher;
import net.minecraft.client.renderer.chunk.ChunkRenderDispatcher.ChunkRender;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.apache.commons.lang3.Validate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class MyBuiltChunkStorage extends ViewFrustum {
    
    
    public static class Preset {
        public ChunkRenderDispatcher.ChunkRender[] data;
        public long lastActiveTime;
        public boolean isNeighborUpdated;
        
        public Preset(ChunkRenderDispatcher.ChunkRender[] data, boolean isNeighborUpdated) {
            this.data = data;
            this.isNeighborUpdated = isNeighborUpdated;
        }
    }
    
    private final ChunkRenderDispatcher factory;
    private final Map<BlockPos, ChunkRenderDispatcher.ChunkRender> builtChunkMap = new HashMap<>();
    private final Map<ChunkPos, Preset> presets = new HashMap<>();
    private boolean shouldUpdateMainPresetNeighbor = true;
    private final ObjectBuffer<ChunkRenderDispatcher.ChunkRender> builtChunkBuffer;
    
    public MyBuiltChunkStorage(
        ChunkRenderDispatcher chunkBuilder,
        World world,
        int r,
        WorldRenderer worldRenderer
    ) {
        super(chunkBuilder, world, r, worldRenderer);
        factory = chunkBuilder;
        
        ModMain.postClientTickSignal.connectWithWeakRef(
            this, MyBuiltChunkStorage::tick
        );
        
        int cacheSize = countChunksX * countChunksY * countChunksZ;
        if (Global.cacheGlBuffer) {
            cacheSize = cacheSize / 10;
        }
        
        builtChunkBuffer = new ObjectBuffer<>(
            cacheSize,
            () -> factory.new ChunkRender(),
            ChunkRenderDispatcher.ChunkRender::deleteGlResources
        );
        
        ModMain.preGameRenderSignal.connectWithWeakRef(this, (this_) -> {
            Minecraft.getInstance().getProfiler().startSection("reserve");
            this_.builtChunkBuffer.reserveObjects(countChunksX * countChunksY * countChunksZ / 100);
            Minecraft.getInstance().getProfiler().endSection();
        });
    }
    
    @Override
    protected void createRenderChunks(ChunkRenderDispatcher chunkBuilder_1) {
        //nothing
    }
    
    @Override
    public void deleteGlResources() {
        getAllActiveBuiltChunks().forEach(
            ChunkRenderDispatcher.ChunkRender::deleteGlResources
        );
        builtChunkMap.clear();
        presets.clear();
        builtChunkBuffer.destroyAll();
    }
    
    @Override
    public void updateChunkPositions(double playerX, double playerZ) {
        Minecraft.getInstance().getProfiler().startSection("built_chunk_storage");
        
        ChunkPos cameraChunkPos = new ChunkPos(
            MathHelper.intFloorDiv((int) playerX, 16),
            MathHelper.intFloorDiv((int) playerZ, 16)
        );
        
        Preset preset = presets.computeIfAbsent(
            cameraChunkPos,
            whatever -> myCreatePreset(playerX, playerZ)
        );
        preset.lastActiveTime = System.nanoTime();
        
        this.renderChunks = preset.data;
        
        Minecraft.getInstance().getProfiler().startSection("neighbor");
        manageNeighbor(preset);
        Minecraft.getInstance().getProfiler().endSection();
        
        Minecraft.getInstance().getProfiler().endSection();
    }
    
    private void manageNeighbor(Preset preset) {
        boolean isRenderingPortal = PortalRendering.isRendering();
        if (!isRenderingPortal) {
            if (shouldUpdateMainPresetNeighbor) {
                shouldUpdateMainPresetNeighbor = false;
                OFBuiltChunkStorageFix.updateNeighbor(this, preset.data);
                preset.isNeighborUpdated = true;
            }
        }
        
        if (!preset.isNeighborUpdated) {
            OFBuiltChunkStorageFix.updateNeighbor(this, preset.data);
            preset.isNeighborUpdated = true;
            if (isRenderingPortal) {
                shouldUpdateMainPresetNeighbor = true;
            }
        }
    }
    
    @Override
    public void markForRerender(int cx, int cy, int cz, boolean isImportant) {
        //TODO change it
        ChunkRenderDispatcher.ChunkRender builtChunk = provideBuiltChunk(
            new BlockPos(cx * 16, cy * 16, cz * 16)
        );
        builtChunk.setNeedsUpdate(isImportant);
    }
    
    private Preset myCreatePreset(double playerXCoord, double playerZCoord) {
        ChunkRenderDispatcher.ChunkRender[] chunks =
            new ChunkRenderDispatcher.ChunkRender[this.countChunksX * this.countChunksY * this.countChunksZ];
        
        int int_1 = MathHelper.floor(playerXCoord);
        int int_2 = MathHelper.floor(playerZCoord);
        
        for (int cx = 0; cx < this.countChunksX; ++cx) {
            int int_4 = this.countChunksX * 16;
            int int_5 = int_1 - 8 - int_4 / 2;
            int px = int_5 + Math.floorMod(cx * 16 - int_5, int_4);
            
            for (int cz = 0; cz < this.countChunksZ; ++cz) {
                int int_8 = this.countChunksZ * 16;
                int int_9 = int_2 - 8 - int_8 / 2;
                int pz = int_9 + Math.floorMod(cz * 16 - int_9, int_8);
                
                for (int cy = 0; cy < this.countChunksY; ++cy) {
                    int py = cy * 16;
                    
                    int index = this.getChunkIndex(cx, cy, cz);
                    Validate.isTrue(px % 16 == 0);
                    Validate.isTrue(py % 16 == 0);
                    Validate.isTrue(pz % 16 == 0);
                    chunks[index] = provideBuiltChunk(
                        new BlockPos(px, py, pz)
                    );
                }
            }
        }
        
        return new Preset(chunks, false);
    }
    
    //copy because private
    private int getChunkIndex(int x, int y, int z) {
        return (z * this.countChunksY + y) * this.countChunksX + x;
    }
    
    private static BlockPos getBasePos(BlockPos blockPos) {
        return new BlockPos(
            MathHelper.intFloorDiv(blockPos.getX(), 16) * 16,
            MathHelper.intFloorDiv(MathHelper.clamp(blockPos.getY(), 0, 255), 16) * 16,
            MathHelper.intFloorDiv(blockPos.getZ(), 16) * 16
        );
    }
    
    public ChunkRenderDispatcher.ChunkRender provideBuiltChunk(BlockPos blockPos) {
        return provideBuiltChunkWithAlignedPos(getBasePos(blockPos));
    }
    
    private ChunkRenderDispatcher.ChunkRender provideBuiltChunkWithAlignedPos(BlockPos basePos) {
        assert basePos.getX() % 16 == 0;
        assert basePos.getY() % 16 == 0;
        assert basePos.getZ() % 16 == 0;
        if (basePos.getY() < 0 || basePos.getY() >= 256) {
            return null;
        }
        
        return builtChunkMap.computeIfAbsent(
            basePos.toImmutable(),
            whatever -> {
                ChunkRenderDispatcher.ChunkRender builtChunk = builtChunkBuffer.takeObject();
                
                builtChunk.setPosition(
                    basePos.getX(), basePos.getY(), basePos.getZ()
                );
                
                OFBuiltChunkStorageFix.onBuiltChunkCreated(
                    this, builtChunk
                );
                
                return builtChunk;
            }
        );
    }
    
    private void tick() {
        ClientWorld worldClient = Minecraft.getInstance().world;
        if (worldClient != null) {
            if (GcMonitor.isMemoryNotEnough()) {
                if (worldClient.getGameTime() % 3 == 0) {
                    purge();
                }
            }
            else {
                if (worldClient.getGameTime() % 213 == 66) {
                    purge();
                }
            }
        }
    }
    
    private void purge() {
        Minecraft.getInstance().getProfiler().startSection("my_built_chunk_storage_purge");
        
        long dropTime = Helper.secondToNano(GcMonitor.isMemoryNotEnough() ? 3 : 20);
        
        long currentTime = System.nanoTime();
        presets.entrySet().removeIf(entry -> {
            Preset preset = entry.getValue();
            if (preset.data == this.renderChunks) {
                return false;
            }
            return currentTime - preset.lastActiveTime > dropTime;
        });
        
        foreachActiveBuiltChunks(builtChunk -> {
            ((IEBuiltChunk) builtChunk).setMark(currentTime);
        });
        
        builtChunkMap.entrySet().removeIf(entry -> {
            ChunkRenderDispatcher.ChunkRender chunk = entry.getValue();
            if (((IEBuiltChunk) chunk).getMark() != currentTime) {
                builtChunkBuffer.returnObject(chunk);
                return true;
            }
            else {
                return false;
            }
        });
        
        OFBuiltChunkStorageFix.purgeRenderRegions(this);
        
        Minecraft.getInstance().getProfiler().endSection();
    }
    
    private Set<ChunkRenderDispatcher.ChunkRender> getAllActiveBuiltChunks() {
        HashSet<ChunkRenderDispatcher.ChunkRender> result = new HashSet<>();
        
        presets.forEach((key, preset) -> {
            result.addAll(Arrays.asList(preset.data));
        });
        
        if (renderChunks != null) {
            result.addAll(Arrays.asList(renderChunks));
        }
        
        return result;
    }
    
    private void foreachActiveBuiltChunks(Consumer<ChunkRenderDispatcher.ChunkRender> func) {
        if (renderChunks != null) {
            for (ChunkRenderDispatcher.ChunkRender chunk : renderChunks) {
                func.accept(chunk);
            }
        }
        
        for (Preset value : presets.values()) {
            for (ChunkRenderDispatcher.ChunkRender chunk : value.data) {
                func.accept(chunk);
            }
        }
    }
    
    public int getManagedChunkNum() {
        return builtChunkMap.size();
    }
    
    public ChunkRenderDispatcher.ChunkRender myGetRenderChunkRaw(
        BlockPos pos, ChunkRenderDispatcher.ChunkRender[] chunks
    ) {
        int i = MathHelper.intFloorDiv(pos.getX(), 16);
        int j = MathHelper.intFloorDiv(pos.getY(), 16);
        int k = MathHelper.intFloorDiv(pos.getZ(), 16);
        if (j >= 0 && j < this.countChunksY) {
            i = MathHelper.normalizeAngle(i, this.countChunksX);
            k = MathHelper.normalizeAngle(k, this.countChunksZ);
            return chunks[this.getChunkIndex(i, j, k)];
        }
        else {
            return null;
        }
    }
    
    public String getDebugString() {
        return String.format(
            "All:%s Needs Rebuild:%s",
            builtChunkMap.size(),
            builtChunkMap.values().stream()
                .filter(
                    builtChunk -> builtChunk.needsUpdate()
                ).count()
        );
    }
    
    public int getRadius() {
        return (countChunksX - 1) / 2;
    }
    
    public boolean isRegionActive(int cxStart, int czStart, int cxEnd, int czEnd) {
        for (int cx = cxStart; cx <= cxEnd; cx++) {
            for (int cz = czStart; cz <= czEnd; cz++) {
                if (builtChunkMap.containsKey(new BlockPos(cx * 16, 0, cz * 16))) {
                    return true;
                }
            }
        }
        
        return false;
    }
}
