package com.qouteall.immersive_portals.render;

import com.google.common.collect.Streams;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.my_util.ObjectBuffer;
import com.qouteall.immersive_portals.optifine_compatibility.OFBuiltChunkNeighborFix;
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

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MyBuiltChunkStorage extends ViewFrustum {
    
    
    public static class Preset {
        public ChunkRenderDispatcher.ChunkRender[] data;
        public long lastActiveTime;
        public boolean isMainPreset;
        public boolean isNeighborUpdated;
    
        public Preset(ChunkRenderDispatcher.ChunkRender[] data, boolean isNeighborUpdated) {
            this.data = data;
            this.isNeighborUpdated = isNeighborUpdated;
        }
    }
    
    private ChunkRenderDispatcher factory;
    private Map<BlockPos, ChunkRenderDispatcher.ChunkRender> builtChunkMap = new HashMap<>();
    private Map<ChunkPos, Preset> presets = new HashMap<>();
    private boolean shouldUpdateMainPresetNeighbor = true;
    private ObjectBuffer<ChunkRenderDispatcher.ChunkRender> builtChunkBuffer;
    
    public MyBuiltChunkStorage(
        ChunkRenderDispatcher chunkBuilder_1,
        World world_1,
        int int_1,
        WorldRenderer worldRenderer_1
    ) {
        super(chunkBuilder_1, world_1, int_1, worldRenderer_1);
        factory = chunkBuilder_1;
        
        ModMain.postClientTickSignal.connectWithWeakRef(
            this, MyBuiltChunkStorage::tick
        );
        
        builtChunkBuffer = new ObjectBuffer<>(
            countChunksX * countChunksY * countChunksZ,
            () -> factory.new ChunkRender(),
            ChunkRenderDispatcher.ChunkRender::deleteGlResources
        );
        
        ModMain.preRenderSignal.connectWithWeakRef(this, (this_) -> {
            Minecraft.getInstance().getProfiler().startSection("reserve");
            this_.builtChunkBuffer.reserveObjects(countChunksX * countChunksY * countChunksZ / 70);
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
        boolean isRenderingPortal = CGlobal.renderer.isRendering();
        if (!isRenderingPortal) {
            if (shouldUpdateMainPresetNeighbor) {
                shouldUpdateMainPresetNeighbor = false;
                OFBuiltChunkNeighborFix.updateNeighbor(this, preset.data);
                preset.isNeighborUpdated = true;
            }
        }
        
        if (!preset.isNeighborUpdated) {
            preset.isNeighborUpdated = true;
            OFBuiltChunkNeighborFix.updateNeighbor(this, preset.data);
            if (isRenderingPortal) {
                shouldUpdateMainPresetNeighbor = true;
            }
        }
    }
    
    @Override
    public void markForRerender(int int_1, int int_2, int int_3, boolean boolean_1) {
        ChunkRenderDispatcher.ChunkRender builtChunk = provideBuiltChunk(
            new BlockPos(int_1 * 16, int_2 * 16, int_3 * 16)
        );
        builtChunk.setNeedsUpdate(boolean_1);
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
    private int getChunkIndex(int int_1, int int_2, int int_3) {
        return (int_3 * this.countChunksY + int_2) * this.countChunksX + int_1;
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
                //MinecraftClient.getInstance().getProfiler().push("new_built_chunk");
                //ChunkBuilder.BuiltChunk builtChunk = factory.new BuiltChunk();
                //MinecraftClient.getInstance().getProfiler().swap("set_origin");
    
                ChunkRenderDispatcher.ChunkRender builtChunk = builtChunkBuffer.takeObject();
    
                builtChunk.setPosition(
                    basePos.getX(), basePos.getY(), basePos.getZ()
                );
                //MinecraftClient.getInstance().getProfiler().pop();
                return builtChunk;
            }
        );
    }
    
    private void tick() {
        ClientWorld worldClient = Minecraft.getInstance().world;
        if (worldClient != null) {
            if (worldClient.getGameTime() % 213 == 66) {
                purge();
            }
        }
    }
    
    private void purge() {
        Minecraft.getInstance().getProfiler().startSection("my_built_chunk_storage_purge");
    
        long currentTime = System.nanoTime();
        presets.entrySet().removeIf(entry -> {
            Preset preset = entry.getValue();
            if (preset.data == this.renderChunks) {
                return false;
            }
            return currentTime - preset.lastActiveTime > Helper.secondToNano(10);
        });
    
        Set<ChunkRenderDispatcher.ChunkRender> activeBuiltChunks = getAllActiveBuiltChunks();
    
        List<ChunkRenderDispatcher.ChunkRender> chunksToDelete = builtChunkMap
            .values().stream().filter(
                builtChunk -> !activeBuiltChunks.contains(builtChunk)
            ).collect(Collectors.toList());
    
        chunksToDelete.forEach(
            builtChunk -> {
                builtChunkBuffer.returnObject(builtChunk);
                //builtChunk.delete();
                ChunkRenderDispatcher.ChunkRender removed =
                    builtChunkMap.remove(builtChunk.getPosition());
                if (removed == null) {
                    Helper.err("Chunk Renderer Abnormal " + builtChunk.getPosition());
                }
            }
        );
    
        Minecraft.getInstance().getProfiler().endSection();
    }
    
    private Set<ChunkRenderDispatcher.ChunkRender> getAllActiveBuiltChunks() {
        Stream<ChunkRenderDispatcher.ChunkRender> result;
        Stream<ChunkRenderDispatcher.ChunkRender> chunksFromPresets = presets.values().stream()
            .flatMap(
                preset -> Arrays.stream(preset.data)
            );
        if (renderChunks == null) {
            result = chunksFromPresets;
        }
        else {
            result = Streams.concat(
                Arrays.stream(renderChunks),
                chunksFromPresets
            );
        }
        return result.collect(Collectors.toSet());
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
}
