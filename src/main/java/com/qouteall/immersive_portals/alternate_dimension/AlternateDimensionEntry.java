package com.qouteall.immersive_portals.alternate_dimension;

import net.minecraft.world.World;
import net.minecraft.world.dimension.Dimension;
import net.minecraft.world.dimension.DimensionType;
import net.minecraftforge.common.ModDimension;

import java.util.function.BiFunction;

public class AlternateDimensionEntry extends ModDimension {
    public static AlternateDimensionEntry instance;
    
    public AlternateDimensionEntry() {
    }
    
    @Override
    public BiFunction<World, DimensionType, ? extends Dimension> getFactory() {
        return AlternateDimension::new;
    }
}
