package com.qouteall.imm_ptl_peripheral;

import com.mojang.serialization.Lifecycle;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.commands.PortalCommand;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.NonNullList;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.SimpleRegistry;
import net.minecraft.util.text.ChatType;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CommandStickItem extends Item {
    
    private static RegistryKey<Registry<Data>> registryRegistryKey = RegistryKey.func_240904_a_(new ResourceLocation("immersive_portals:command_stick_type"));
    
    public static class Data {
        public final String command;
        public final String nameTranslationKey;
        public final List<String> descriptionTranslationKeys;
        
        public Data(
            String command, String nameTranslationKey, List<String> descriptionTranslationKeys
        ) {
            this.command = command;
            this.nameTranslationKey = nameTranslationKey;
            this.descriptionTranslationKeys = descriptionTranslationKeys;
        }
        
        public void serialize(CompoundNBT tag) {
            tag.putString("command", command);
            tag.putString("nameTranslationKey", nameTranslationKey);
            ListNBT listTag = new ListNBT();
            for (String descriptionTK : descriptionTranslationKeys) {
                listTag.add(StringNBT.valueOf(descriptionTK));
            }
            tag.put("descriptionTranslationKeys", listTag);
        }
        
        public static Data deserialize(CompoundNBT tag) {
            return new Data(
                tag.getString("command"),
                tag.getString("nameTranslationKey"),
                tag.getList(
                    "descriptionTranslationKeys",
                    StringNBT.valueOf("").getId()
                )
                    .stream()
                    .map(tag1 -> ((StringNBT) tag1).getString())
                    .collect(Collectors.toList())
            );
        }
    }
    
    public static final SimpleRegistry<Data> commandStickTypeRegistry = new SimpleRegistry<>(
        registryRegistryKey,
        Lifecycle.stable()
    );
    
    public static void registerType(String id, Data data) {
        commandStickTypeRegistry.register(
            RegistryKey.func_240903_a_(
                registryRegistryKey, new ResourceLocation(id)
            ),
            data,
            Lifecycle.stable()
        );
    }
    
    public static final CommandStickItem instance = new CommandStickItem(new Item.Properties());
    
    public CommandStickItem(Properties settings) {
        super(settings);
    }
    
    @Override
    public boolean hasEffect(ItemStack stack) {
        return true;
    }
    
    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, PlayerEntity player, Hand hand) {
        doUse(player, player.getHeldItem(hand));
        return super.onItemRightClick(world, player, hand);
    }
    
    private void doUse(PlayerEntity player, ItemStack stack) {
        if (player.world.isRemote()) {
            return;
        }
        
        if (canUseCommand(player)) {
            Data data = Data.deserialize(stack.getOrCreateTag());
            
            CommandSource commandSource = player.getCommandSource().withPermissionLevel(2);
            
            Commands commandManager = McHelper.getServer().getCommandManager();
            
            commandManager.handleCommand(commandSource, data.command);
        }
        else {
            sendMessage(player, new StringTextComponent("No Permission"));
        }
    }
    
    private static boolean canUseCommand(PlayerEntity player) {
        if (Global.easeCommandStickPermission) {
            return true;// any player regardless of gamemode can use
        }
        else {
            return player.hasPermissionLevel(2) || player.isCreative();
        }
    }
    
    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<ITextComponent> tooltip, ITooltipFlag context) {
        super.addInformation(stack, world, tooltip, context);
        
        Data data = Data.deserialize(stack.getOrCreateTag());
        
        tooltip.add(new StringTextComponent(data.command));
        
        for (String descriptionTranslationKey : data.descriptionTranslationKeys) {
            tooltip.add(new TranslationTextComponent(descriptionTranslationKey));
        }
        
        tooltip.add(new TranslationTextComponent("imm_ptl.command_stick"));
    }
    
    @Override
    public void fillItemGroup(ItemGroup group, NonNullList<ItemStack> stacks) {
        if (group == ItemGroup.MISC) {
            commandStickTypeRegistry.stream().forEach(data -> {
                ItemStack stack = new ItemStack(instance);
                data.serialize(stack.getOrCreateTag());
                stacks.add(stack);
            });
        }
    }
    
    @Override
    public String getTranslationKey(ItemStack stack) {
        Data data = Data.deserialize(stack.getOrCreateTag());
        return data.nameTranslationKey;
    }
    
    public static void sendMessage(PlayerEntity player, ITextComponent message) {
        ((ServerPlayerEntity) player).func_241151_a_(message, ChatType.GAME_INFO, Util.field_240973_b_);
    }
    
    public static void init() {
        PortalCommand.createCommandStickCommandSignal.connect((player, command) -> {
            ItemStack itemStack = new ItemStack(instance, 1);
            Data data = new Data(
                command,
                command, new ArrayList<>()
            );
            data.serialize(itemStack.getOrCreateTag());
            
            player.inventory.addItemStackToInventory(itemStack);
            player.container.detectAndSendChanges();
        });
    }
}
