package com.qouteall.immersive_portals.mixin.altius_world;

import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.Lifecycle;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.alternate_dimension.AlternateDimensions;
import com.qouteall.immersive_portals.altius_world.AltiusInfo;
import com.qouteall.immersive_portals.ducks.IEGeneratorOptions;
import com.qouteall.immersive_portals.ducks.IELevelProperties;
import net.minecraft.command.TimerCallbackManager;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.registry.DynamicRegistries;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.border.WorldBorder;
import net.minecraft.world.gen.settings.DimensionGeneratorSettings;
import net.minecraft.world.storage.ServerWorldInfo;
import net.minecraft.world.storage.VersionData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedHashSet;
import java.util.UUID;

@Mixin(ServerWorldInfo.class)
public class MixinLevelProperties implements IELevelProperties {
    
    @Shadow
    @Final
    private Lifecycle field_237344_d_;
    @Shadow
    @Final
    private DimensionGeneratorSettings field_237343_c_;
    AltiusInfo altiusInfo;
    
    @Inject(
        method = "<init>(Lcom/mojang/datafixers/DataFixer;ILnet/minecraft/nbt/CompoundNBT;ZIIIFJJIIIZIZZZLnet/minecraft/world/border/WorldBorder$Serializer;IILjava/util/UUID;Ljava/util/LinkedHashSet;Lnet/minecraft/command/TimerCallbackManager;Lnet/minecraft/nbt/CompoundNBT;Lnet/minecraft/nbt/CompoundNBT;Lnet/minecraft/world/WorldSettings;Lnet/minecraft/world/gen/settings/DimensionGeneratorSettings;Lcom/mojang/serialization/Lifecycle;)V",
        at = @At("RETURN")
    )
    private void onConstructedFromLevelInfo(
        DataFixer dataFixer, int dataVersion, CompoundNBT playerData,
        boolean modded, int spawnX, int spawnY, int spawnZ, float spawnAngle,
        long time, long timeOfDay, int version, int clearWeatherTime, int rainTime,
        boolean raining, int thunderTime, boolean thundering, boolean initialized,
        boolean difficultyLocked, WorldBorder.Serializer worldBorder, int wanderingTraderSpawnDelay,
        int wanderingTraderSpawnChance, UUID wanderingTraderId, LinkedHashSet<String> serverBrands,
        TimerCallbackManager<MinecraftServer> scheduledEvents, CompoundNBT customBossEvents, CompoundNBT dragonFight,
        WorldSettings levelInfo, DimensionGeneratorSettings generatorOptions, Lifecycle lifecycle, CallbackInfo ci
    ) {
        altiusInfo = ((IELevelProperties) (Object) levelInfo).getAltiusInfo();
        
        // TODO use more appropriate way to get rid of the warning screen
        if (Global.enableAlternateDimensions && generatorOptions.func_236224_e_().keySet().size() == 8) {
            lifecycle = Lifecycle.stable();
        }
    }
    
    @Inject(
        method = "Lnet/minecraft/world/storage/ServerWorldInfo;func_237369_a_(Lcom/mojang/serialization/Dynamic;Lcom/mojang/datafixers/DataFixer;ILnet/minecraft/nbt/CompoundNBT;Lnet/minecraft/world/WorldSettings;Lnet/minecraft/world/storage/VersionData;Lnet/minecraft/world/gen/settings/DimensionGeneratorSettings;Lcom/mojang/serialization/Lifecycle;)Lnet/minecraft/world/storage/ServerWorldInfo;",
        at = @At("RETURN"),
        cancellable = true
    )
    private static void onReadDataFromTag(
        Dynamic<INBT> dynamic,
        DataFixer dataFixer,
        int i,
        CompoundNBT playerTag,
        WorldSettings levelInfo,
        VersionData saveVersionInfo,
        DimensionGeneratorSettings generatorOptions,
        Lifecycle lifecycle,
        CallbackInfoReturnable<ServerWorldInfo> cir
    ) {
        ServerWorldInfo levelProperties = cir.getReturnValue();
        
        MixinLevelProperties this_ = (MixinLevelProperties) (Object) levelProperties;
        
        AltiusInfo levelInfoAltiusInfo = ((IELevelProperties) (Object) levelInfo).getAltiusInfo();
        if (levelInfoAltiusInfo != null) {
            this_.altiusInfo = levelInfoAltiusInfo;
            return;
        }
        
        INBT altiusTag = dynamic.getElement("altius", null);
        if (altiusTag != null) {
            this_.altiusInfo = AltiusInfo.fromTag(((CompoundNBT) altiusTag));
        }
    }
    
    @Inject(
        method = "Lnet/minecraft/world/storage/ServerWorldInfo;func_237370_a_(Lnet/minecraft/util/registry/DynamicRegistries;Lnet/minecraft/nbt/CompoundNBT;Lnet/minecraft/nbt/CompoundNBT;)V",
        at = @At("HEAD")
    )
    private void onUpdateProperties(
        DynamicRegistries dynamicRegistryManager, CompoundNBT compoundTag,
        CompoundNBT compoundTag2, CallbackInfo ci
    ) {
        ((IEGeneratorOptions) field_237343_c_).setDimOptionRegistry(
            AlternateDimensions.getAlternateDimensionsRemoved(
                field_237343_c_.func_236224_e_()
            )
        );
        if (altiusInfo != null) {
            compoundTag.put("altius", altiusInfo.toTag());
        }
    }
    
    @Override
    public AltiusInfo getAltiusInfo() {
        return altiusInfo;
    }
    
    @Override
    public void setAltiusInfo(AltiusInfo cond) {
        altiusInfo = cond;
    }
}
