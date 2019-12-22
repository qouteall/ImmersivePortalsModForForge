package com.qouteall.immersive_portals;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.global_portals.BorderPortal;
import com.qouteall.immersive_portals.portal.global_portals.EndFloorPortal;
import com.qouteall.immersive_portals.portal.nether_portal.NewNetherPortalEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.ComponentArgument;
import net.minecraft.command.arguments.DimensionArgument;
import net.minecraft.command.arguments.NBTCompoundTagArgument;
import net.minecraft.command.arguments.Vec3Argument;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.server.ServerWorld;

import java.util.Comparator;
import java.util.UUID;
import java.util.function.Consumer;

public class MyCommandServer {
    public static void register(
        CommandDispatcher<CommandSource> dispatcher
    ) {
        LiteralArgumentBuilder<CommandSource> builder = Commands
            .literal("portal")
            .requires(commandSource -> commandSource.hasPermissionLevel(2));
        
        builder.then(Commands
            .literal("border_set")
            .then(Commands
                .argument("x1", IntegerArgumentType.integer())
                .then(Commands
                    .argument("y1", IntegerArgumentType.integer())
                    .then(Commands
                        .argument("x2", IntegerArgumentType.integer())
                        .then(Commands
                            .argument("y2", IntegerArgumentType.integer())
                            .executes(context -> {
                                BorderPortal.setBorderPortal(
                                    context.getSource().getWorld(),
                                    IntegerArgumentType.getInteger(context, "x1"),
                                    IntegerArgumentType.getInteger(context, "y1"),
                                    IntegerArgumentType.getInteger(context, "x2"),
                                    IntegerArgumentType.getInteger(context, "y2")
                                );
                                return 0;
                            })
                        )
                    )
                )
            )
        );
        builder.then(Commands
            .literal("border_remove")
            .executes(context -> {
                BorderPortal.removeBorderPortal(context.getSource().getWorld());
                return 0;
            })
        );
        builder.then(Commands
            .literal("end_floor_enable")
            .executes(context -> {
                EndFloorPortal.enableFloor();
                return 0;
            })
        );
        builder.then(Commands
            .literal("end_floor_remove")
            .executes(context -> {
                EndFloorPortal.removeFloor();
                return 0;
            })
        );
    
        builder.then(Commands
            .literal("stabilize_nearby_nether_portal")
            .executes(context -> {
                ServerWorld world = context.getSource().getWorld();
                McHelper.getEntitiesNearby(
                    world,
                    context.getSource().getPos(),
                    NewNetherPortalEntity.class,
                    5
                ).forEach(
                    portal -> {
                        portal.unbreakable = true;
                        context.getSource().sendFeedback(
                            new StringTextComponent("Stabilized " + portal),
                            true
                        );
                    }
                );
                return 0;
            })
        );
    
    
        builder.then(Commands
            .literal("view_portal_data")
            .executes(context -> {
                return processPortalTargetedCommand(
                    context,
                    (portal) -> {
                        sendPortalInfo(context, portal);
                    }
                );
            })
        );
    
        builder.then(Commands
            .literal("set_portal_custom_name")
            .then(Commands
                .argument(
                    "name",
                    ComponentArgument.component()
                ).executes(context -> {
                    return processPortalTargetedCommand(
                        context,
                        portal -> {
                            ITextComponent name = ComponentArgument.getComponent(context, "name");
                            portal.setCustomName(name);
                        }
                    );
                })
            )
        );
    
        builder.then(Commands
            .literal("delete_portal")
            .executes(context -> processPortalTargetedCommand(
                context,
                portal -> {
                    sendMessage(context, "deleted " + portal);
                    portal.remove();
                }
            ))
        );
    
        builder.then(Commands
            .literal("set_portal_nbt")
            .then(Commands
                .argument("nbt", NBTCompoundTagArgument.func_218043_a())
                .executes(context -> processPortalTargetedCommand(
                    context,
                    portal -> {
                        CompoundNBT newNbt = NBTCompoundTagArgument.func_218042_a(
                            context, "nbt"
                        );
                    
                        CompoundNBT portalNbt = portal.serializeNBT();
                    
                        newNbt.keySet().forEach(
                            key -> portalNbt.put(key, newNbt.get(key))
                        );
                    
                        //portalNbt.copyFrom(newNbt);
                    
                        UUID uuid = portal.getUniqueID();
                        portal.deserializeNBT(portalNbt);
                        portal.setUniqueId(uuid);
                    
                        reloadPortal(portal);
                    
                        sendPortalInfo(context, portal);
                    }
                ))
            )
        );
    
        builder.then(Commands
            .literal("set_portal_destination")
            .then(
                Commands.argument(
                    "dim",
                    DimensionArgument.getDimension()
                ).then(
                    Commands.argument(
                        "dest",
                        Vec3Argument.vec3()
                    ).executes(
                        context -> processPortalTargetedCommand(
                            context,
                            portal -> {
                                try {
                                    portal.dimensionTo = DimensionArgument.func_212592_a(
                                        context, "dim"
                                    );
                                    portal.destination = Vec3Argument.getVec3(
                                        context, "dest"
                                    );
                                
                                    reloadPortal(portal);
                                
                                    sendMessage(context, portal.toString());
                                }
                                catch (CommandSyntaxException ignored) {
                                    ignored.printStackTrace();
                                }
                            }
                        )
                    )
                )
            )
        );
        
        dispatcher.register(builder);
    }
    
    
    public static void sendPortalInfo(CommandContext<CommandSource> context, Portal portal) {
        context.getSource().sendFeedback(
            portal.serializeNBT().toFormattedComponent(),
            false
        );
        
        sendMessage(
            context,
            "\n\n" + portal.toString()
        );
    }
    
    public static void reloadPortal(Portal portal) {
        portal.remove();
        
        Helper.SimpleBox<Integer> counter = new Helper.SimpleBox<>(0);
        ModMain.serverTaskList.addTask(() -> {
            if (counter.obj < 3) {
                counter.obj++;
                return false;
            }
            portal.removed = false;
            portal.world.addEntity(portal);
            return true;
        });
    }
    
    public static void sendMessage(CommandContext<CommandSource> context, String message) {
        context.getSource().sendFeedback(
            new StringTextComponent(
                message
            ),
            false
        );
    }
    
    public static int processPortalTargetedCommand(
        CommandContext<CommandSource> context,
        Consumer<Portal> processCommand
    ) throws CommandSyntaxException {
        CommandSource source = context.getSource();
        ServerPlayerEntity player = source.asPlayer();
        if (player == null) {
            source.sendFeedback(
                new StringTextComponent("Only player can use this command"),
                true
            );
            return 0;
        }
        
        Portal portal = getPlayerPointingPortal(player);
        
        if (portal == null) {
            source.sendFeedback(
                new StringTextComponent("You are not pointing to any portal"),
                true
            );
            return 0;
        }
        else {
            processCommand.accept(portal);
        }
        return 0;
    }
    
    private static Portal getPlayerPointingPortal(
        ServerPlayerEntity player
    ) {
        Vec3d from = player.getEyePosition(1);
        Vec3d to = from.add(player.getLookVec().scale(100));
        Pair<Portal, Vec3d> result = McHelper.getEntitiesNearby(
            player,
            Portal.class,
            100
        ).map(
            portal -> new Pair<Portal, Vec3d>(
                portal, portal.rayTrace(from, to)
            )
        ).filter(
            portalAndHitPos -> portalAndHitPos.getSecond() != null
        ).min(
            Comparator.comparingDouble(
                portalAndHitPos -> portalAndHitPos.getSecond().squareDistanceTo(from)
            )
        ).orElse(null);
        if (result != null) {
            return result.getFirst();
        }
        else {
            return null;
        }
    }
}
