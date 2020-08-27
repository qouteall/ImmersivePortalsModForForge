package com.qouteall.immersive_portals.portal.custom_portal_gen;

import com.google.common.collect.Lists;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.ListCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.custom_portal_gen.form.PortalGenForm;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class CustomPortalGeneration {
    public static final RegistryKey<World> theSameDimension = RegistryKey.func_240903_a_(
        Registry.field_239699_ae_,
        new ResourceLocation("imm_ptl:the_same_dimension")
    );
    
    public static final Codec<List<RegistryKey<World>>> dimensionListCodec =
        new ListCodec<>(World.field_234917_f_);
    public static final Codec<List<String>> stringListCodec =
        new ListCodec<>(Codec.STRING);
    
    public static RegistryKey<Registry<Codec<CustomPortalGeneration>>> schemaRegistryKey = RegistryKey.func_240904_a_(
        new ResourceLocation("imm_ptl:custom_portal_gen_schema")
    );
    
    public static RegistryKey<Registry<CustomPortalGeneration>> registryRegistryKey =
        RegistryKey.func_240904_a_(new ResourceLocation("imm_ptl:custom_portal_generation"));
    
    public static final Codec<CustomPortalGeneration> codecV1 = RecordCodecBuilder.create(instance -> {
        return instance.group(
            dimensionListCodec.fieldOf("from").forGetter(o -> o.fromDimensions),
            World.field_234917_f_.fieldOf("to").forGetter(o -> o.toDimension),
            Codec.INT.optionalFieldOf("space_ratio_from", 1).forGetter(o -> o.spaceRatioFrom),
            Codec.INT.optionalFieldOf("space_ratio_to", 1).forGetter(o -> o.spaceRatioTo),
            Codec.BOOL.optionalFieldOf("reversible", true).forGetter(o -> o.reversible),
            PortalGenForm.codec.fieldOf("form").forGetter(o -> o.form),
            PortalGenTrigger.triggerCodec.fieldOf("trigger").forGetter(o -> o.trigger),
            stringListCodec.optionalFieldOf("post_invoke_commands", Collections.emptyList())
                .forGetter(o -> o.postInvokeCommands)
        ).apply(instance, instance.stable(CustomPortalGeneration::new));
    });
    
    public static SimpleRegistry<Codec<CustomPortalGeneration>> schemaRegistry = Util.make(() -> {
        SimpleRegistry<Codec<CustomPortalGeneration>> registry = new SimpleRegistry<>(
            schemaRegistryKey, Lifecycle.stable()
        );
        Registry.register(
            registry, new ResourceLocation("imm_ptl:v1"), codecV1
        );
        return registry;
    });
    
    public static final MapCodec<CustomPortalGeneration> codec = schemaRegistry.dispatchMap(
        "schema_version", e -> codecV1, Function.identity()
    );
    
    
    public final List<RegistryKey<World>> fromDimensions;
    public final RegistryKey<World> toDimension;
    public final int spaceRatioFrom;
    public final int spaceRatioTo;
    public final boolean reversible;
    public final PortalGenForm form;
    public final PortalGenTrigger trigger;
    public final List<String> postInvokeCommands;
    
    public CustomPortalGeneration(
        List<RegistryKey<World>> fromDimensions, RegistryKey<World> toDimension,
        int spaceRatioFrom, int spaceRatioTo, boolean reversible,
        PortalGenForm form, PortalGenTrigger trigger,
        List<String> postInvokeCommands
    ) {
        this.fromDimensions = fromDimensions;
        this.toDimension = toDimension;
        this.spaceRatioFrom = spaceRatioFrom;
        this.spaceRatioTo = spaceRatioTo;
        this.reversible = reversible;
        this.form = form;
        this.trigger = trigger;
        this.postInvokeCommands = postInvokeCommands;
    }
    
    public CustomPortalGeneration getReverse() {
        if (toDimension == theSameDimension) {
            return new CustomPortalGeneration(
                fromDimensions,
                theSameDimension,
                spaceRatioTo,
                spaceRatioFrom,
                false,
                form.getReverse(),
                trigger,
                postInvokeCommands
            );
        }
        
        if (fromDimensions.size() == 1) {
            return new CustomPortalGeneration(
                Lists.newArrayList(toDimension),
                fromDimensions.get(0),
                spaceRatioTo,
                spaceRatioFrom,
                false,
                form.getReverse(),
                trigger,
                postInvokeCommands
            );
        }
        
        Helper.err("Cannot get reverse custom portal gen");
        return null;
    }
    
    public BlockPos mapPosition(BlockPos from) {
        return Helper.divide(Helper.scale(from, spaceRatioTo), spaceRatioFrom);
    }
    
    public boolean initAndCheck() {
        // if from dimension is not present, nothing happens
        
        RegistryKey<World> toDimension = this.toDimension;
        if (toDimension != theSameDimension) {
            if (McHelper.getServer().getWorld(toDimension) == null) {
                return false;
            }
        }
        
        if (!form.initAndCheck()) {
            return false;
        }
        
        return true;
    }
    
    @Override
    public String toString() {
        return McHelper.serializeToJson(
            this,
            codec.codec()
        );
    }
    
    public boolean perform(ServerWorld world, BlockPos startPos) {
        if (!fromDimensions.contains(world.func_234923_W_())) {
            return false;
        }
        
        if (!world.isBlockLoaded(startPos)) {
            Helper.log("Skip custom portal generation because chunk not loaded");
            return false;
        }
        
        RegistryKey<World> destDimension = this.toDimension;
        
        if (destDimension == theSameDimension) {
            destDimension = world.func_234923_W_();
        }
        
        ServerWorld toWorld = McHelper.getServer().getWorld(destDimension);
        
        if (toWorld == null) {
            Helper.err("Missing dimension " + destDimension.func_240901_a_());
            return false;
        }
        
        world.getProfiler().startSection("custom_portal_gen_perform");
        boolean result = form.perform(this, world, startPos, toWorld);
        world.getProfiler().endSection();
        return result;
    }
    
    public void onPortalGenerated(Portal portal) {
        if (postInvokeCommands.isEmpty()) {
            return;
        }
        
        CommandSource commandSource = portal.getCommandSource().withPermissionLevel(4).withFeedbackDisabled();
        Commands commandManager = McHelper.getServer().getCommandManager();
        
        for (String command : postInvokeCommands) {
            commandManager.handleCommand(commandSource, command);
        }
    }
}
