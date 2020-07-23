package com.qouteall.immersive_portals.block_manipulation;

import com.qouteall.hiding_in_the_bushes.O_O;
import com.qouteall.immersive_portals.Global;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.RangedAttribute;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;
import net.minecraft.potion.Potion;

import java.util.function.BiFunction;

public class HandReachTweak {
    public static BiFunction<EffectType, Integer, Effect>
        statusEffectConstructor;
    
    public static final Attribute handReachMultiplierAttribute =
        (new RangedAttribute( "imm_ptl.hand_reach_multiplier",
            1.0D, 0.0D, 1024.0D
        )).func_233753_a_(true);
    
    public static Effect longerReachEffect;
    
    public static Potion longerReachPotion;
    
    public static double getActualHandReachMultiplier(PlayerEntity playerEntity) {
        if (O_O.isReachEntityAttributesPresent) {
            return 1;
        }
        if (O_O.isForge()) {
            return 1;
        }
        double multiplier = playerEntity.getAttribute(handReachMultiplierAttribute).getValue();
        if (Global.longerReachInCreative && playerEntity.isCreative()) {
            return multiplier * 10;
        }
        else {
            return multiplier;
        }
    }
    
    
}
