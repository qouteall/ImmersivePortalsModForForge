package com.qouteall.immersive_portals.mixin_client;

import com.google.common.collect.Streams;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.ducks.IEChunkRenderDispatcher;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.render.MyRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.chunk.ChunkRender;
import net.minecraft.client.renderer.chunk.IChunkRendererFactory;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.stream.Collectors;

//NOTE WeakReference does not override equals()
//don't put them into set
@Mixin(value = ViewFrustum.class)
public abstract class MixinViewFrustum implements IEChunkRenderDispatcher {
    @Shadow
    @Final
    protected WorldRenderer renderGlobal;
    @Shadow
    @Final
    protected World world;
    @Shadow
    protected int countChunksY;
    @Shadow
    protected int countChunksX;
    @Shadow
    protected int countChunksZ;
    @Shadow
    public ChunkRender[] renderChunks;
    
    private Map<ChunkPos, ChunkRender[]> presetCache;
    private IChunkRendererFactory factory;
    private Map<BlockPos, ChunkRender> chunkRendererMap;
    private Set<ChunkRender[]> isNeighborUpdated;
    
    @Inject(
        method = "<init>",
        at = @At("RETURN")
    )
    private void onConstruct(
        World world_1,
        int renderDistanceChunks,
        WorldRenderer worldRenderer_1,
        IChunkRendererFactory chunkRendererFactory,
        CallbackInfo ci
    ) {
        this.factory = chunkRendererFactory;
        
        chunkRendererMap = new HashMap<>();
        
        presetCache = new HashMap<>();
        isNeighborUpdated = new HashSet<>();
        
        ModMain.postClientTickSignal.connectWithWeakRef(
            ((IEChunkRenderDispatcher) this),
            IEChunkRenderDispatcher::tick
        );
        
        if (CGlobal.useHackedChunkRenderDispatcher) {
            //it will run createChunks() before this
            for (ChunkRender renderChunk : renderChunks) {
                chunkRendererMap.put(getOriginNonMutable(renderChunk), renderChunk);
            }
        }
    }
    
    private BlockPos getOriginNonMutable(ChunkRender renderChunk) {
        return renderChunk.getPosition().toImmutable();
    }
    
    @Inject(method = "deleteGlResources", at = @At("HEAD"), cancellable = true)
    private void delete(CallbackInfo ci) {
        if (CGlobal.useHackedChunkRenderDispatcher) {
            chunkRendererMap.values().forEach(ChunkRender::deleteGlResources);
            
            chunkRendererMap.clear();
            
            presetCache.clear();
            isNeighborUpdated.clear();
            
            ci.cancel();
        }
    }
    
    @Override
    public void tick() {
        if (CGlobal.useHackedChunkRenderDispatcher) {
            ClientWorld worldClient = Minecraft.getInstance().world;
            if (worldClient != null) {
                if (worldClient.getGameTime() % 533 == 66) {
                    dismissInactiveChunkRenderers();
                    presetCache.clear();
                    isNeighborUpdated.clear();
                }
            }
        }
    }
    
    private ChunkRender findAndEmployChunkRenderer(BlockPos basePos) {
        assert !chunkRendererMap.containsKey(basePos);
        assert CGlobal.useHackedChunkRenderDispatcher;
    
        Minecraft.getInstance().getProfiler().startSection("create_chunk_renderer");
        ChunkRender chunkRenderer = factory.create(world, renderGlobal);
        Minecraft.getInstance().getProfiler().endSection();
        
        
        employChunkRenderer(chunkRenderer, basePos);
        
        if (chunkRenderer.getWorld() == null) {
            Helper.err("Employed invalid chunk renderer");
        }
        
        return chunkRenderer;
    }
    
    private void employChunkRenderer(ChunkRender chunkRenderer, BlockPos basePos) {
        assert CGlobal.useHackedChunkRenderDispatcher;
    
        Minecraft.getInstance().getProfiler().startSection("employ");
        
        assert basePos.getX() % 16 == 0;
        assert basePos.getY() % 16 == 0;
        assert basePos.getZ() % 16 == 0;
    
        chunkRenderer.setPosition(basePos.getX(), basePos.getY(), basePos.getZ());
        BlockPos origin = getOriginNonMutable(chunkRenderer);
        assert !chunkRendererMap.containsKey(origin);
        chunkRendererMap.put(origin, chunkRenderer);
    
        Minecraft.getInstance().getProfiler().endSection();
    }
    
    private void dismissChunkRenderer(BlockPos basePos) {
        assert CGlobal.useHackedChunkRenderDispatcher;
        
        ChunkRender chunkRenderer = chunkRendererMap.remove(basePos);
        
        if (chunkRenderer == null) {
            Helper.log("Chunk Renderer Abnormal");
            return;
        }
    
        chunkRenderer.deleteGlResources();
    }
    
    private void dismissInactiveChunkRenderers() {
        assert CGlobal.useHackedChunkRenderDispatcher;
        
        Set<ChunkRender> activeRenderers = Streams.concat(
            presetCache.values().stream().flatMap(
                Arrays::stream
            ),
            Arrays.stream(renderChunks)
        ).collect(Collectors.toSet());
        
        chunkRendererMap.values().stream()
            .filter(r -> !activeRenderers.contains(r))
            .collect(Collectors.toList())
            .forEach(
                chunkRenderer -> dismissChunkRenderer(getOriginNonMutable(chunkRenderer))
            );
    }
    
    @Inject(method = "updateChunkPositions", at = @At("HEAD"), cancellable = true)
    private void updateCameraPosition(double viewEntityX, double viewEntityZ, CallbackInfo ci) {
        if (CGlobal.useHackedChunkRenderDispatcher) {
            Minecraft.getInstance().getProfiler().startSection(
                "update_hacked_chunk_render_dispatcher"
            );
            
            ChunkPos currPlayerChunkPos = new ChunkPos(
                (((int) viewEntityX) / 16),
                (((int) viewEntityZ) / 16)
            );
            renderChunks = presetCache.computeIfAbsent(
                currPlayerChunkPos,
                k -> createPreset(viewEntityX, viewEntityZ)
            );
    
            if (!isNeighborUpdated.contains(renderChunks)) {
        
                isNeighborUpdated.add(renderChunks);
            }
    
            Minecraft.getInstance().getProfiler().endSection();
            
            ci.cancel();
        }
        else {
            if (CGlobal.renderer.isRendering()) {
                if (
                    Minecraft.getInstance().renderViewEntity.dimension ==
                        MyRenderHelper.originalPlayerDimension
                ) {
                    ci.cancel();
                }
            }
        }
    }
    
    private ChunkRender[] createPreset(double viewEntityX, double viewEntityZ) {
        ChunkRender[] preset = new ChunkRender[this.countChunksX * this.countChunksY * this.countChunksZ];
        
        int px = MathHelper.floor(viewEntityX) - 8;
        int pz = MathHelper.floor(viewEntityZ) - 8;
    
        int maxLen = this.countChunksX * 16;
    
        for (int cx = 0; cx < this.countChunksX; ++cx) {
            int posX = this.getBaseCoordinate(px, maxLen, cx);
        
            for (int cz = 0; cz < this.countChunksZ; ++cz) {
                int posZ = this.getBaseCoordinate(pz, maxLen, cz);
            
                for (int cy = 0; cy < this.countChunksY; ++cy) {
                    int posY = cy * 16;
                
                    preset[this.getIndex(cx, cy, cz)] =
                        validateChunkRenderer(
                            myGetChunkRenderer(
                                new BlockPos(posX, posY, posZ)
                            )
                        );
                }
            }
        }
        
        return preset;
    }
    
    //NOTE input block pos instead of chunk pos
    private ChunkRender myGetChunkRenderer(BlockPos blockPos) {
        assert CGlobal.useHackedChunkRenderDispatcher;
        
        BlockPos basePos = getBasePos(blockPos);
        
        if (chunkRendererMap.containsKey(basePos)) {
            return chunkRendererMap.get(basePos);
        }
        else {
            return findAndEmployChunkRenderer(basePos);
        }
    }
    
    private static BlockPos getBasePos(BlockPos blockPos) {
        return new BlockPos(
            MathHelper.intFloorDiv(blockPos.getX(), 16) * 16,
            MathHelper.intFloorDiv(blockPos.getY(), 16) * 16,
            MathHelper.intFloorDiv(blockPos.getZ(), 16) * 16
        );
    }
    
    @Shadow
    public abstract int getBaseCoordinate(int int_1, int int_2, int int_3);
    
    @Shadow
    public abstract int getIndex(int int_1, int int_2, int int_3);
    
    @Shadow
    public abstract ChunkRender getRenderChunk(BlockPos pos);
    
    @Override
    public int getEmployedRendererNum() {
        return CGlobal.useHackedChunkRenderDispatcher ? chunkRendererMap.size() : renderChunks.length;
    }
    
    @Override
    public void rebuildAll() {
        for (ChunkRender chunkRenderer : renderChunks) {
            chunkRenderer.setNeedsUpdate(true);
        }
    }
    
    private ChunkRender validateChunkRenderer(ChunkRender chunkRenderer) {
        if (chunkRenderer.getWorld() == null) {
            Helper.err("Invalid Chunk Renderer " +
                world.dimension.getType() +
                getOriginNonMutable(chunkRenderer));
            return findAndEmployChunkRenderer(getOriginNonMutable(chunkRenderer));
        }
        else {
            return chunkRenderer;
        }
    }
}
