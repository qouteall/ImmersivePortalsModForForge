package com.qouteall.immersive_portals.portal.custom_portal_gen;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonElement;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.Lifecycle;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUseContext;
import net.minecraft.server.IDynamicRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.util.registry.WorldSettingsImport;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.server.ServerWorld;

public class CustomPortalGenManagement {
    private static final Multimap<Item, CustomPortalGeneration> useItemGen = HashMultimap.create();
    private static final Multimap<Item, CustomPortalGeneration> throwItemGen = HashMultimap.create();
    
    public static void onDatapackReload() {
        useItemGen.clear();
        throwItemGen.clear();
        
        Helper.log("Loading custom portal gen");
        
        MinecraftServer server = McHelper.getServer();
        
        IDynamicRegistries.Impl registryTracker = new IDynamicRegistries.Impl();
        
        WorldSettingsImport<JsonElement> registryOps =
            WorldSettingsImport.func_240876_a_(JsonOps.INSTANCE,
                server.resourceManager.func_240970_h_(),
                registryTracker
            );
        
        SimpleRegistry<CustomPortalGeneration> emptyRegistry = new SimpleRegistry<>(
            CustomPortalGeneration.registryRegistryKey,
            Lifecycle.stable()
        );
        
        DataResult<SimpleRegistry<CustomPortalGeneration>> dataResult =
            registryOps.func_241797_a_(
                emptyRegistry,
                CustomPortalGeneration.registryRegistryKey,
                CustomPortalGeneration.codec
            );
        
        SimpleRegistry<CustomPortalGeneration> result = dataResult.get().left().orElse(null);
        
        if (result == null) {
            DataResult.PartialResult<SimpleRegistry<CustomPortalGeneration>> r =
                dataResult.get().right().get();
            McHelper.sendMessageToFirstLoggedPlayer(new StringTextComponent(
                "Error when parsing custom portal generation\n" +
                    r.message()
            ));
            return;
        }
        
        result.stream().forEach(gen -> {
            if (!gen.initAndCheck()) {
                Helper.log("Custom Portal Gen Is Not Activated " + gen.toString());
                return;
            }
            
            Helper.log("Loaded Custom Portal Gen " + gen.toString());
            
            load(gen);
            
            if (gen.reversible) {
                CustomPortalGeneration reverse = gen.getReverse();
                if (gen.initAndCheck()) {
                    load(reverse);
                }
            }
        });
    }
    
    private static void load(CustomPortalGeneration gen) {
        PortalGenTrigger trigger = gen.trigger;
        if (trigger instanceof PortalGenTrigger.UseItemTrigger) {
            useItemGen.put(((PortalGenTrigger.UseItemTrigger) trigger).item, gen);
        }
        else if (trigger instanceof PortalGenTrigger.ThrowItemTrigger) {
            throwItemGen.put(
                ((PortalGenTrigger.ThrowItemTrigger) trigger).item,
                gen
            );
        }
    }
    
    public static void onItemUse(ItemUseContext context, ActionResultType actionResult) {
        if (context.getWorld().isRemote()) {
            return;
        }
        
        Item item = context.getItem().getItem();
        if (useItemGen.containsKey(item)) {
            ModMain.serverTaskList.addTask(() -> {
                for (CustomPortalGeneration gen : useItemGen.get(item)) {
                    boolean result = gen.perform(
                        ((ServerWorld) context.getWorld()),
                        context.getPos().offset(context.getFace())
                    );
                    if (result) {
                        if (gen.trigger instanceof PortalGenTrigger.UseItemTrigger) {
                            PortalGenTrigger.UseItemTrigger trigger =
                                (PortalGenTrigger.UseItemTrigger) gen.trigger;
                            if (trigger.consume) {
                                context.getItem().shrink(1);
                            }
                        }
                        break;
                    }
                }
                return true;
            });
        }
    }
    
    public static void onItemTick(ItemEntity entity) {
        if (entity.world.isRemote()) {
            return;
        }
        if (entity.getThrowerId() == null) {
            return;
        }
        
        if (entity.cannotPickup()) {
            Item item = entity.getItem().getItem();
            if (throwItemGen.containsKey(item)) {
                ModMain.serverTaskList.addTask(() -> {
                    for (CustomPortalGeneration gen : throwItemGen.get(item)) {
                        boolean result = gen.perform(
                            ((ServerWorld) entity.world),
                            entity.func_233580_cy_()
                        );
                        if (result) {
                            entity.getItem().shrink(1);
                            break;
                        }
                    }
                    return true;
                });
            }
        }
    }
}
