package com.qouteall.hiding_in_the_bushes.alternate_dimension;

import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.alternate_dimension.ChaosBiomeSource;
import com.qouteall.immersive_portals.alternate_dimension.ErrorTerrainGenerator;
import com.qouteall.immersive_portals.alternate_dimension.MyFloatingIslandChunkGenerator;
import com.qouteall.immersive_portals.alternate_dimension.VoidChunkGenerator;
import net.minecraft.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.biome.provider.BiomeProviderType;
import net.minecraft.world.biome.provider.OverworldBiomeProvider;
import net.minecraft.world.biome.provider.OverworldBiomeProviderSettings;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.ChunkGeneratorType;
import net.minecraft.world.gen.EndGenerationSettings;
import net.minecraft.world.gen.OverworldGenSettings;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.Random;
import java.util.function.Function;

public class AlternateDimension extends Dimension {
    
    public static final BlockPos SPAWN = new BlockPos(100, 50, 0);
    
    private static Random random = new Random();
    Function<AlternateDimension, ChunkGenerator> chunkGeneratorFunction;
    
    public AlternateDimension(
        World worldIn,
        DimensionType typeIn,
        Function<AlternateDimension, ChunkGenerator> chunkGeneratorFunction_
    ) {
        super(worldIn, typeIn, 0.0F);
        chunkGeneratorFunction = chunkGeneratorFunction_;
    }
    
    public ChunkGenerator<?> createChunkGenerator() {
        return chunkGeneratorFunction.apply(this);
    }
    
    
    public ChunkGenerator<?> getChunkGenerator2() {
        OverworldGenSettings overworldChunkGeneratorConfig2 =
            (OverworldGenSettings) ChunkGeneratorType.SURFACE.createSettings();
        OverworldBiomeProviderSettings vanillaLayeredBiomeSourceConfig2 =
            ((OverworldBiomeProviderSettings) BiomeProviderType.VANILLA_LAYERED
                .func_226840_a_(this.world.getWorldInfo()))
                .setGeneratorSettings(
                    overworldChunkGeneratorConfig2
                );
        
        OverworldBiomeProvider newBiomeSource =
            BiomeProviderType.VANILLA_LAYERED.create(vanillaLayeredBiomeSourceConfig2);
        
        EndGenerationSettings generationSettings = ChunkGeneratorType.FLOATING_ISLANDS.createSettings();
        generationSettings.setDefaultBlock(Blocks.STONE.getDefaultState());
        generationSettings.setDefaultFluid(Blocks.AIR.getDefaultState());
        generationSettings.setSpawnPos(this.getSpawnCoordinate());
        return ChunkGeneratorType.FLOATING_ISLANDS.create(
            this.world,
            newBiomeSource,
            generationSettings
        );
    }
    
    public ChunkGenerator<?> getChunkGenerator1() {
        EndGenerationSettings generationSettings = ChunkGeneratorType.FLOATING_ISLANDS.createSettings();
        generationSettings.setDefaultBlock(Blocks.STONE.getDefaultState());
        generationSettings.setDefaultFluid(Blocks.WATER.getDefaultState());
        generationSettings.setSpawnPos(this.getSpawnCoordinate());
        return ChunkGeneratorType.FLOATING_ISLANDS.create(
            this.world,
            BiomeProviderType.FIXED.create(
                BiomeProviderType.FIXED.func_226840_a_(world.getWorldInfo()).setBiome(
                    Registry.BIOME.getRandom(random)
                )
            ),
            generationSettings
        );
    }
    
    public ChunkGenerator<?> getChunkGenerator3() {
        
        EndGenerationSettings generationSettings = ChunkGeneratorType.FLOATING_ISLANDS.createSettings();
        generationSettings.setDefaultBlock(Blocks.STONE.getDefaultState());
        generationSettings.setDefaultFluid(Blocks.WATER.getDefaultState());
        generationSettings.setSpawnPos(this.getSpawnCoordinate());
        
        
        return new MyFloatingIslandChunkGenerator(
            world,
            new ChaosBiomeSource(world.getSeed()),
            generationSettings
        
        );
    }
    
    public ChunkGenerator<?> getChunkGenerator4() {
        EndGenerationSettings generationSettings = ChunkGeneratorType.FLOATING_ISLANDS.createSettings();
        generationSettings.setDefaultBlock(Blocks.STONE.getDefaultState());
        generationSettings.setDefaultFluid(Blocks.WATER.getDefaultState());
        generationSettings.setSpawnPos(this.getSpawnCoordinate());
    
        return new ErrorTerrainGenerator(
            world,
            new ChaosBiomeSource(world.getSeed()),
            generationSettings
        );
    }
    
    public ChunkGenerator<?> getChunkGenerator5() {
        EndGenerationSettings generationSettings = ChunkGeneratorType.FLOATING_ISLANDS.createSettings();
        generationSettings.setDefaultBlock(Blocks.STONE.getDefaultState());
        generationSettings.setDefaultFluid(Blocks.WATER.getDefaultState());
        generationSettings.setSpawnPos(this.getSpawnCoordinate());
        
        return new VoidChunkGenerator(
            world,
            BiomeProviderType.FIXED.create(
                BiomeProviderType.FIXED.func_226840_a_(world.getWorldInfo()).setBiome(
                    Biomes.PLAINS
                )
            ),
            generationSettings
        );
    }
    
    
    /**
     * Calculates the angle of sun and moon in the sky relative to a specified time (usually worldTime)
     */
    public float calculateCelestialAngle(long worldTime, float partialTicks) {
        double d0 = MathHelper.frac((double) worldTime / 24000.0D - 0.25D);
        double d1 = 0.5D - Math.cos(d0 * Math.PI) / 2.0D;
        return (float) (d0 * 2.0D + d1) / 3.0F;
    }
    
    /**
     * Return Vec3D with biome specific fog color
     */
    @OnlyIn(Dist.CLIENT)
    public Vec3d getFogColor(float celestialAngle, float partialTicks) {
        float f = MathHelper.cos(celestialAngle * ((float) Math.PI * 2F)) * 2.0F + 0.5F;
        f = MathHelper.clamp(f, 0.0F, 1.0F);
        float f1 = 0.7529412F;
        float f2 = 0.84705883F;
        float f3 = 1.0F;
        f1 = f1 * (f * 0.94F + 0.06F);
        f2 = f2 * (f * 0.94F + 0.06F);
        f3 = f3 * (f * 0.91F + 0.09F);
        return new Vec3d((double) f1, (double) f2, (double) f3);
    }
    
    @OnlyIn(Dist.CLIENT)
    public boolean isSkyColored() {
        return true;
    }
    
    /**
     * True if the player can respawn in this dimension (true = overworld, false = nether).
     */
    public boolean canRespawnHere() {
        return false;
    }
    
    /**
     * Returns 'true' if in the "main surface world", but 'false' if in the Nether or End dimensions.
     */
    public boolean isSurfaceWorld() {
        return true;
    }
    
    /**
     * the y level at which clouds are rendered.
     */
    @OnlyIn(Dist.CLIENT)
    public float getCloudHeight() {
        return 128.0F;
    }
    
    @Nullable
    public BlockPos findSpawn(ChunkPos chunkPosIn, boolean checkValid) {
        Random random = new Random(this.world.getSeed());
        BlockPos blockpos = new BlockPos(
            chunkPosIn.getXStart() + random.nextInt(15),
            0,
            chunkPosIn.getZEnd() + random.nextInt(15)
        );
        return this.world.getGroundAboveSeaLevel(blockpos).getMaterial().blocksMovement() ? blockpos : null;
    }
    
    public BlockPos getSpawnCoordinate() {
        return SPAWN;
    }
    
    @Nullable
    public BlockPos findSpawn(int posX, int posZ, boolean checkValid) {
        return this.findSpawn(new ChunkPos(posX >> 4, posZ >> 4), checkValid);
    }
    
    /**
     * Returns true if the given X,Z coordinate should show environmental fog.
     */
    @OnlyIn(Dist.CLIENT)
    public boolean doesXZShowFog(int x, int z) {
        return false;
    }
    
    /**
     * Called when the world is performing a save. Only used to save the state of the Dragon Boss fight in
     * WorldProviderEnd in Vanilla.
     */
    public void onWorldSave() {
        CompoundNBT compoundnbt = new CompoundNBT();
        
        this.world.getWorldInfo().setDimensionData(
            this.world.getDimension().getType(),
            compoundnbt
        );
    }
    
    /**
     * Called when the world is updating entities. Only used in WorldProviderEnd to update the DragonFightManager in
     * Vanilla.
     */
    public void tick() {
    
    
    }
    
    //avoid dark sky when camera is low
//    @Override
//    public double getHorizon() {
//        return -999;
//    }
    
    //avoid dark fog when camera is low
    @OnlyIn(Dist.CLIENT)
    @Override
    public double getVoidFogYFactor() {
        if (Minecraft.getInstance().gameRenderer.getActiveRenderInfo().getProjectedView().y > 0) {
            return 999;
        }
        else {
            return -999;
        }
    }
    
    @Override
    public void updateWeather(Runnable defaultLogic) {
        ServerWorld overWorld = McHelper.getServer().getWorld(DimensionType.OVERWORLD);
    
        McHelper.getServer().getWorlds().forEach(thisWorld -> {
            if (thisWorld.dimension instanceof AlternateDimension) {
                thisWorld.rainingStrength = overWorld.getRainStrength(1);
                thisWorld.thunderingStrength = overWorld.getThunderStrength(1);
            }
        });
    }
}
