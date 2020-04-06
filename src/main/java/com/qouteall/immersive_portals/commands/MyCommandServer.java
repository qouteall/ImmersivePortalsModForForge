package com.qouteall.immersive_portals.commands;

import com.google.common.collect.Streams;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.PortalManipulation;
import com.qouteall.immersive_portals.portal.global_portals.BorderBarrierFiller;
import com.qouteall.immersive_portals.portal.global_portals.BorderPortal;
import com.qouteall.immersive_portals.portal.global_portals.GlobalTrackedPortal;
import com.qouteall.immersive_portals.portal.global_portals.VerticalConnectingPortal;
import net.minecraft.client.renderer.Quaternion;
import net.minecraft.client.renderer.Vector3f;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.ComponentArgument;
import net.minecraft.command.arguments.DimensionArgument;
import net.minecraft.command.arguments.NBTCompoundTagArgument;
import net.minecraft.command.arguments.Vec3Argument;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class MyCommandServer {
    public static void registerClientDebugCommand(
        CommandDispatcher<CommandSource> dispatcher
    ) {
        MyCommandClient.register(dispatcher);
    }
    
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
                    .argument("z1", IntegerArgumentType.integer())
                    .then(Commands
                        .argument("x2", IntegerArgumentType.integer())
                        .then(Commands
                            .argument("z2", IntegerArgumentType.integer())
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
            .literal("fill_border_with_barrier")
            .executes(context -> {
                BorderBarrierFiller.onCommandExecuted(
                    context.getSource().asPlayer()
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
                .argument("nbt", NBTCompoundTagArgument.nbt())
                .executes(context -> processPortalTargetedCommand(
                    context,
                    portal -> {
                        CompoundNBT newNbt = NBTCompoundTagArgument.getNbt(
                            context, "nbt"
                        );
                        
                        CompoundNBT portalNbt = portal.writeWithoutTypeId(new CompoundNBT());
                        
                        newNbt.keySet().forEach(
                            key -> portalNbt.put(key, newNbt.get(key))
                        );
                        
                        //portalNbt.copyFrom(newNbt);
                        
                        UUID uuid = portal.getUniqueID();
                        portal.read(portalNbt);
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
                        Vec3Argument.vec3(false)
                    ).executes(
                        context -> processPortalTargetedCommand(
                            context,
                            portal -> {
                                try {
                                    portal.dimensionTo = DimensionArgument.getDimensionArgument(
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
        
        builder.then(Commands
            .literal("set_portal_rotation")
            .then(
                Commands.argument(
                    "rotatingAxis",
                    Vec3Argument.vec3(false)
                ).then(
                    Commands.argument(
                        "angleDegrees",
                        DoubleArgumentType.doubleArg()
                    ).executes(
                        context -> processPortalTargetedCommand(
                            context,
                            portal -> {
                                try {
                                    Vec3d rotatingAxis = Vec3Argument.getVec3(
                                        context, "rotatingAxis"
                                    ).normalize();
                                    
                                    double angleDegrees = DoubleArgumentType.getDouble(
                                        context, "angleDegrees"
                                    );
                                    
                                    if (angleDegrees != 0) {
                                        portal.rotation = new Quaternion(
                                            new Vector3f(
                                                (float) rotatingAxis.x,
                                                (float) rotatingAxis.y,
                                                (float) rotatingAxis.z
                                            ),
                                            (float) angleDegrees,
                                            true
                                        );
                                    }
                                    else {
                                        portal.rotation = null;
                                    }
                                    
                                    reloadPortal(portal);
                                    
                                    
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
        
        builder.then(Commands
            .literal("rotate_portal_body")
            .then(
                Commands.argument(
                    "rotatingAxis",
                    Vec3Argument.vec3(false)
                ).then(
                    Commands.argument(
                        "angleDegrees",
                        DoubleArgumentType.doubleArg()
                    ).executes(
                        context -> processPortalTargetedCommand(
                            context,
                            portal -> {
                                try {
                                    Vec3d rotatingAxis = Vec3Argument.getVec3(
                                        context, "rotatingAxis"
                                    ).normalize();
                                    
                                    double angleDegrees = DoubleArgumentType.getDouble(
                                        context, "angleDegrees"
                                    );
                                    
                                    PortalManipulation.rotatePortalBody(
                                        portal,
                                        new Quaternion(
                                            new Vector3f(rotatingAxis),
                                            (float) angleDegrees,
                                            true
                                        )
                                    );
                                    
                                    reloadPortal(portal);
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
        
        builder.then(Commands
            .literal("tpme")
            .then(
                Commands.argument(
                    "dim",
                    DimensionArgument.getDimension()
                ).then(
                    Commands.argument(
                        "dest",
                        Vec3Argument.vec3()
                    ).executes(
                        context -> {
                            DimensionType dimension = DimensionArgument.getDimensionArgument(
                                context, "dim"
                            );
                            Vec3d pos = Vec3Argument.getVec3(
                                context, "dest"
                            );
                            
                            ServerPlayerEntity player = context.getSource().asPlayer();
                            Global.serverTeleportationManager.invokeTpmeCommand(
                                player, dimension, pos
                            );
                            return 0;
                        }
                    )
                )
            )
        );
        
        builder.then(Commands
            .literal("complete_bi_way_portal")
            .executes(context -> processPortalTargetedCommand(
                context,
                portal -> {
                    PortalManipulation.removeOverlappedPortals(
                        McHelper.getServer().getWorld(portal.dimensionTo),
                        portal.destination,
                        portal.transformLocalVec(portal.getNormal().scale(-1)),
                        p -> sendMessage(context, "Removed " + p)
                    );
                    
                    Portal result = PortalManipulation.completeBiWayPortal(
                        portal,
                        Portal.entityType
                    );
                    sendMessage(context, "Added " + result);
                }
            ))
        );
        
        builder.then(Commands
            .literal("complete_bi_faced_portal")
            .executes(context -> processPortalTargetedCommand(
                context,
                portal -> {
                    PortalManipulation.removeOverlappedPortals(
                        ((ServerWorld) portal.world),
                        portal.getPositionVec(),
                        portal.getNormal().scale(-1),
                        p -> sendMessage(context, "Removed " + p)
                    );
                    
                    Portal result = PortalManipulation.completeBiFacedPortal(
                        portal,
                        Portal.entityType
                    );
                    sendMessage(context, "Added " + result);
                }
            ))
        );
        
        builder.then(Commands
            .literal("complete_bi_way_bi_faced_portal")
            .executes(context -> processPortalTargetedCommand(
                context,
                portal ->
                    PortalManipulation.completeBiWayBiFacedPortal(
                        portal,
                        p -> sendMessage(context, "Removed " + p),
                        p -> sendMessage(context, "Added " + p), Portal.entityType
                    )
            ))
        );
        
        builder.then(Commands
            .literal("remove_connected_portals")
            .executes(context -> processPortalTargetedCommand(
                context,
                portal -> {
                    Consumer<Portal> removalInformer = p -> sendMessage(context, "Removed " + p);
                    PortalManipulation.removeConnectedPortals(portal, removalInformer);
                }
            ))
        );
        
        builder.then(Commands
            .literal("move_portal_half_block")
            .executes(context -> processPortalTargetedCommand(
                context, portal -> {
                    try {
                        ServerPlayerEntity player = context.getSource().asPlayer();
                        Vec3d viewVector = player.getLookVec();
                        Direction facing = Direction.getFacingFromVector(
                            viewVector.x, viewVector.y, viewVector.z
                        );
                        Vec3d offset = new Vec3d(facing.getDirectionVec()).scale(0.5);
                        portal.setPosition(
                            portal.getPosX() + offset.x,
                            portal.getPosY() + offset.y,
                            portal.getPosZ() + offset.z
                        );
                    }
                    catch (CommandSyntaxException e) {
                        sendMessage(context, "This command can only be invoked by player");
                    }
                }
            ))
        );
        
        builder.then(Commands
            .literal("connect_floor")
            .then(
                Commands.argument(
                    "from",
                    DimensionArgument.getDimension()
                ).then(
                    Commands.argument(
                        "to",
                        DimensionArgument.getDimension()
                    ).executes(
                        context -> {
                            DimensionType from = DimensionArgument.getDimensionArgument(
                                context, "from"
                            );
                            DimensionType to = DimensionArgument.getDimensionArgument(
                                context, "to"
                            );
                            
                            VerticalConnectingPortal.connect(
                                from, VerticalConnectingPortal.ConnectorType.floor, to
                            );
                            return 0;
                        }
                    )
                )
            )
        );
        
        builder.then(Commands
            .literal("connect_ceil")
            .then(
                Commands.argument(
                    "from",
                    DimensionArgument.getDimension()
                ).then(
                    Commands.argument(
                        "to",
                        DimensionArgument.getDimension()
                    ).executes(
                        context -> {
                            DimensionType from = DimensionArgument.getDimensionArgument(
                                context, "from"
                            );
                            DimensionType to = DimensionArgument.getDimensionArgument(
                                context, "to"
                            );
                            
                            VerticalConnectingPortal.connect(
                                from, VerticalConnectingPortal.ConnectorType.ceil, to
                            );
                            return 0;
                        }
                    )
                )
            )
        );
        
        builder.then(Commands
            .literal("connection_floor_remove")
            .then(
                Commands.argument(
                    "dim",
                    DimensionArgument.getDimension()
                ).executes(
                    context -> {
                        DimensionType dim = DimensionArgument.getDimensionArgument(
                            context, "dim"
                        );
                        
                        VerticalConnectingPortal.removeConnectingPortal(
                            VerticalConnectingPortal.ConnectorType.floor, dim
                        );
                        return 0;
                    }
                )
            )
        );
        
        builder.then(Commands
            .literal("connection_ceil_remove")
            .then(
                Commands.argument(
                    "dim",
                    DimensionArgument.getDimension()
                ).executes(
                    context -> {
                        DimensionType dim = DimensionArgument.getDimensionArgument(
                            context, "dim"
                        );
                        
                        VerticalConnectingPortal.removeConnectingPortal(
                            VerticalConnectingPortal.ConnectorType.ceil, dim
                        );
                        return 0;
                    }
                )
            )
        );
        
        builder.then(Commands
            .literal("goback")
            .executes(context -> {
                ServerPlayerEntity player = context.getSource().asPlayer();
                net.minecraft.util.Tuple<DimensionType, Vec3d> lastPos =
                    Global.serverTeleportationManager.lastPosition.get(player);
                if (lastPos == null) {
                    sendMessage(context, "You haven't teleported");
                }
                else {
                    Global.serverTeleportationManager.invokeTpmeCommand(
                        player, lastPos.getA(), lastPos.getB()
                    );
                }
                return 0;
            })
        );
        
        dispatcher.register(builder);
    }
    
    public static void sendPortalInfo(CommandContext<CommandSource> context, Portal portal) {
        context.getSource().sendFeedback(
            portal.writeWithoutTypeId(new CompoundNBT()).toFormattedComponent(),
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
            if (counter.obj < 2) {
                counter.obj++;
                return false;
            }
            portal.removed = false;
            portal.updateCache();
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
    
    public static Portal getPlayerPointingPortal(
        ServerPlayerEntity player
    ) {
        return getPlayerPointingPortalRaw(player, 1, 100, false)
            .map(Pair::getFirst).orElse(null);
    }
    
    public static Optional<Pair<Portal, Vec3d>> getPlayerPointingPortalRaw(
        PlayerEntity player, float tickDelta, double maxDistance, boolean includeGlobalPortal
    ) {
        Vec3d from = player.getEyePosition(tickDelta);
        Vec3d to = from.add(player.getLook(tickDelta).scale(maxDistance));
        Stream<Portal> portalStream = McHelper.getEntitiesNearby(
            player,
            Portal.class,
            maxDistance
        );
        if (includeGlobalPortal) {
            List<GlobalTrackedPortal> globalPortals = McHelper.getGlobalPortals(player.world);
            if (globalPortals != null) {
                portalStream = Streams.concat(
                    portalStream,
                    globalPortals.stream()
                );
            }
        }
        return portalStream.map(
            portal -> new Pair<Portal, Vec3d>(
                portal, portal.pick(from, to)
            )
        ).filter(
            portalAndHitPos -> portalAndHitPos.getSecond() != null
        ).min(
            Comparator.comparingDouble(
                portalAndHitPos -> portalAndHitPos.getSecond().squareDistanceTo(from)
            )
        );
    }
    
}
