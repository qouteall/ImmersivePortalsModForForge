package com.qouteall.immersive_portals.alternate_dimension;

import java.util.Random;
import java.util.stream.IntStream;
import net.minecraft.world.gen.SimplexNoiseGenerator;

public class CompositeSimplexNoiseSampler {
    private SimplexNoiseGenerator[] samplers;
    
    public CompositeSimplexNoiseSampler(int samplerNum, long seed) {
        samplers = IntStream.range(0, samplerNum)
            .mapToObj(i -> new SimplexNoiseGenerator(new Random(seed + i)))
            .toArray(SimplexNoiseGenerator[]::new);
    }
    
    public int getSamplerNum() {
        return samplers.length;
    }
    
    public int sample(double x, double z) {
        int result = 0;
        for (int i = 0; i < getSamplerNum(); i++) {
            result = result * 2;
            double r = samplers[i].getValue(x, z);
            if (r > 0) {
                result += 1;
            }
        }
        return result;
    }
}
