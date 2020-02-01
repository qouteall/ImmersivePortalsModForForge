package com.qouteall.immersive_portals.alternate_dimension;

import net.minecraft.world.World;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraftforge.common.ModDimension;

import java.util.function.BiFunction;
import java.util.function.Function;

public class AlternateDimensionEntry extends ModDimension {
    public static AlternateDimensionEntry instance1;
    public static AlternateDimensionEntry instance2;
    public static AlternateDimensionEntry instance3;
    public static AlternateDimensionEntry instance4;
    public static AlternateDimensionEntry instance5;
    
    Function<AlternateDimension, ChunkGenerator> chunkGeneratorFunction;
    
    public AlternateDimensionEntry(
        Function<AlternateDimension, ChunkGenerator> chunkGeneratorFunction_
    ) {
        chunkGeneratorFunction = chunkGeneratorFunction_;
    }
    
    @Override
    public BiFunction<World, DimensionType, ? extends Dimension> getFactory() {
        return (world, type) -> new AlternateDimension(
            world,
            type,
            chunkGeneratorFunction
        );
    }
}
