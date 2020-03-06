package com.qouteall.immersive_portals.block_manipulation;

import com.qouteall.immersive_portals.Global;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.IAttribute;
import net.minecraft.entity.ai.attributes.RangedAttribute;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.EffectType;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.registry.Registry;

import java.util.function.BiFunction;

public class HandReachTweak {
    public static BiFunction<EffectType, Integer, Effect>
        statusEffectConstructor;
    
    public static final IAttribute handReachMultiplierAttribute =
        (new RangedAttribute((IAttribute) null, "imm_ptl.hand_reach_multiplier",
            1.0D, 0.0D, 1024.0D
        )).setDescription("Hand Reach Multiplier").setShouldWatch(true);
    
    public static Effect longerReachEffect;
    
    public static Potion longerReachPotion;
    
    public static double getActualHandReachMultiplier(PlayerEntity playerEntity) {
        double multiplier = playerEntity.getAttribute(handReachMultiplierAttribute).getValue();
        if (Global.longerReachInCreative && playerEntity.isCreative()) {
            return multiplier * 10;
        }
        else {
            return multiplier;
        }
    }
    
    
    public static void init() {
        Effect.class.hashCode();
        longerReachEffect = statusEffectConstructor.apply(EffectType.BENEFICIAL, 0)
            .addAttributesModifier(
                handReachMultiplierAttribute,
                "91AEAA56-2333-2333-2333-2F7F68070635",
                0.5,
                AttributeModifier.Operation.MULTIPLY_TOTAL
            );
        Registry.register(
            Registry.EFFECTS,
            new ResourceLocation("immersive_portals", "longer_reach"),
            longerReachEffect
        );
        
        longerReachPotion = new Potion(
            new EffectInstance(
                longerReachEffect, 3600, 1
            )
        );
        Registry.register(
            Registry.POTION,
            new ResourceLocation("immersive_portals", "longer_reach_potion"),
            longerReachPotion
        );
    }
    
    
}
