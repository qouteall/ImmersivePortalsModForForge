package com.qouteall.hiding_in_the_bushes;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.particles.IParticleData;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.DimensionType;
import net.minecraft.world.ITickList;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeManager;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.chunk.AbstractChunkProvider;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.lighting.WorldLightManager;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Stream;

// used for IForgeBlockState#isPortalFrame
// the nether portal searching optimization needs to test whether a block state
// is obsidian without using getBlockState method because getBlockState is slow
public class DummyWorldReader implements IWorldReader {
    public static final DummyWorldReader instance = new DummyWorldReader();
    
    @Override
    public BlockState getBlockState(BlockPos pos) {
        return Blocks.AIR.getDefaultState();
    }
    
    @Override
    public FluidState getFluidState(BlockPos pos) {
        return null;
    }
    
    @Nullable
    @Override
    public IChunk getChunk(int x, int z, ChunkStatus requiredStatus, boolean nonnull) {
        return null;
    }
    
    @Override
    public boolean chunkExists(int chunkX, int chunkZ) {
        return false;
    }
    
    @Override
    public int getHeight(Heightmap.Type heightmapType, int x, int z) {
        return 0;
    }
    
    @Override
    public int getSkylightSubtracted() {
        return 0;
    }
    
    @Override
    public BiomeManager getBiomeManager() {
        return null;
    }
    
    @Override
    public Biome getNoiseBiomeRaw(int x, int y, int z) {
        return null;
    }
    
    @Override
    public boolean isRemote() {
        return false;
    }
    
    @Override
    public int getSeaLevel() {
        return 0;
    }
    
    @Override
    public DimensionType func_230315_m_() {
        return null;
    }
    
    @Override
    public WorldBorder getWorldBorder() {
        return null;
    }
    
    @Override
    public Stream<VoxelShape> func_230318_c_(@Nullable Entity p_230318_1_, AxisAlignedBB p_230318_2_, Predicate<Entity> p_230318_3_) {
        return null;
    }
    
    @Override
    public float func_230487_a_(Direction p_230487_1_, boolean p_230487_2_) {
        return 0;
    }
    
    @Override
    public WorldLightManager getLightManager() {
        return null;
    }
    
    @Nullable
    @Override
    public TileEntity getTileEntity(BlockPos pos) {
        return null;
    }
    
   
}
