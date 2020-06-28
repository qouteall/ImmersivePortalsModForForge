package com.qouteall.immersive_portals.commands;

import com.google.common.collect.Streams;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.datafixers.util.Pair;
import com.qouteall.hiding_in_the_bushes.O_O;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.portal.GeometryPortalShape;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.PortalManipulation;
import com.qouteall.immersive_portals.portal.global_portals.BorderBarrierFiller;
import com.qouteall.immersive_portals.portal.global_portals.GlobalTrackedPortal;
import com.qouteall.immersive_portals.portal.global_portals.VerticalConnectingPortal;
import com.qouteall.immersive_portals.portal.global_portals.WorldWrappingPortal;
import net.minecraft.client.renderer.Quaternion;
import net.minecraft.client.renderer.Vector3f;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.command.arguments.ColumnPosArgument;
import net.minecraft.command.arguments.ComponentArgument;
import net.minecraft.command.arguments.DimensionArgument;
import net.minecraft.command.arguments.EntityArgument;
import net.minecraft.command.arguments.NBTCompoundTagArgument;
import net.minecraft.command.arguments.Vec3Argument;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.ColumnPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class PortalCommand {
    public static void registerClientDebugCommand(
        CommandDispatcher<CommandSource> dispatcher
    ) {
        ClientDebugCommand.register(dispatcher);
    }
    
    private static void registerGlobalPortalCommands(
        LiteralArgumentBuilder<CommandSource> builder
    ) {
        builder.then(Commands.literal("create_inward_wrapping")
            .then(Commands.argument("p1", ColumnPosArgument.columnPos())
                .then(Commands.argument("p2", ColumnPosArgument.columnPos())
                    .executes(context -> {
                        ColumnPos p1 = ColumnPosArgument.fromBlockPos(context, "p1");
                        ColumnPos p2 = ColumnPosArgument.fromBlockPos(context, "p2");
                        WorldWrappingPortal.invokeAddWrappingZone(
                            context.getSource().getWorld(),
                            p1.x, p1.z, p2.x, p2.z,
                            true,
                            text -> context.getSource().sendFeedback(text, false)
                        );
                        return 0;
                    })
                )
            )
        );
        
        builder.then(Commands.literal("create_outward_wrapping")
            .then(Commands.argument("p1", ColumnPosArgument.columnPos())
                .then(Commands.argument("p2", ColumnPosArgument.columnPos())
                    .executes(context -> {
                        ColumnPos p1 = ColumnPosArgument.fromBlockPos(context, "p1");
                        ColumnPos p2 = ColumnPosArgument.fromBlockPos(context, "p2");
                        WorldWrappingPortal.invokeAddWrappingZone(
                            context.getSource().getWorld(),
                            p1.x, p1.z, p2.x, p2.z,
                            false,
                            text -> context.getSource().sendFeedback(text, false)
                        );
                        return 0;
                    })
                )
            )
        );
        
        builder.then(Commands.literal("remove_wrapping_zone")
            .executes(context -> {
                WorldWrappingPortal.invokeRemoveWrappingZone(
                    context.getSource().getWorld(),
                    context.getSource().getPos(),
                    text -> context.getSource().sendFeedback(text, false)
                );
                return 0;
            })
            .then(Commands.argument("id", IntegerArgumentType.integer())
                .executes(context -> {
                    int id = IntegerArgumentType.getInteger(context, "id");
                    WorldWrappingPortal.invokeRemoveWrappingZone(
                        context.getSource().getWorld(),
                        id,
                        text -> context.getSource().sendFeedback(text, false)
                    );
                    return 0;
                })
            )
        );
        
        builder.then(Commands.literal("view_wrapping_zones")
            .executes(context -> {
                WorldWrappingPortal.invokeViewWrappingZones(
                    context.getSource().getWorld(),
                    text -> context.getSource().sendFeedback(text, false)
                );
                return 0;
            })
        );
        
        builder.then(Commands.literal("clear_wrapping_border")
            .executes(context -> {
                BorderBarrierFiller.onCommandExecuted(
                    context.getSource().asPlayer()
                );
                return 0;
            })
            .then(Commands.argument("id", IntegerArgumentType.integer())
                .executes(context -> {
                    int id = IntegerArgumentType.getInteger(context, "id");
                    BorderBarrierFiller.onCommandExecuted(
                        context.getSource().asPlayer(),
                        id
                    );
                    return 0;
                })
            )
        );
        
        builder.then(Commands.literal("connect_floor")
            .then(Commands.argument("from", DimensionArgument.getDimension())
                .then(
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
        
        builder.then(Commands.literal("connect_ceil")
            .then(Commands.argument("from", DimensionArgument.getDimension())
                .then(Commands.argument("to", DimensionArgument.getDimension())
                    .executes(
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
            .then(Commands.argument("dim", DimensionArgument.getDimension())
                .executes(
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
            .then(Commands.argument("dim", DimensionArgument.getDimension())
                .executes(
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
        
        builder.then(Commands.literal("view_global_portals")
            .executes(context -> {
                ServerPlayerEntity player = context.getSource().asPlayer();
                sendMessage(
                    context,
                    Helper.myToString(McHelper.getGlobalPortals(player.world).stream())
                );
                return 0;
            })
        );
    }
    
    private static void registerPortalTargetedCommands(
        LiteralArgumentBuilder<CommandSource> builder
    ) {
        builder.then(Commands.literal("view_portal_data")
            .executes(context -> processPortalTargetedCommand(
                context,
                (portal) -> {
                    sendPortalInfo(context, portal);
                }
            ))
        );
        
        builder.then(Commands.literal("set_portal_custom_name")
            .then(Commands
                .argument("name", ComponentArgument.component())
                .executes(context -> processPortalTargetedCommand(
                    context,
                    portal -> {
                        ITextComponent name = ComponentArgument.getComponent(context, "name");
                        portal.setCustomName(name);
                    }
                ))
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
        
        builder.then(Commands.literal("set_portal_nbt")
            .then(Commands.argument("nbt", NBTCompoundTagArgument.nbt())
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
                        
                        UUID uuid = portal.getUniqueID();
                        portal.read(portalNbt);
                        portal.setUniqueId(uuid);
                        
                        reloadPortal(portal);
                        
                        sendPortalInfo(context, portal);
                    }
                ))
            )
        );
        
        builder.then(Commands.literal("set_portal_destination")
            .then(Commands.argument("dim", DimensionArgument.getDimension())
                .then(Commands.argument("dest", Vec3Argument.vec3(false))
                    .executes(
                        context -> processPortalTargetedCommand(
                            context,
                            portal -> {
                                invokeSetPortalDestination(context, portal);
                            }
                        )
                    )
                )
            )
        );
        
        builder.then(Commands.literal("set_portal_rotation")
            .then(Commands.argument("rotatingAxis", Vec3Argument.vec3(false))
                .then(Commands.argument("angleDegrees", DoubleArgumentType.doubleArg())
                    .executes(context -> processPortalTargetedCommand(
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
        
        builder.then(Commands.literal("rotate_portal_body")
            .then(Commands.argument("rotatingAxis", Vec3Argument.vec3(false))
                .then(Commands.argument("angleDegrees", DoubleArgumentType.doubleArg())
                    .executes(context -> processPortalTargetedCommand(
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
        
        builder.then(Commands.literal("rotate_portal_rotation")
            .then(Commands.argument("rotatingAxis", Vec3Argument.vec3(false))
                .then(Commands.argument("angleDegrees", DoubleArgumentType.doubleArg())
                    .executes(context -> processPortalTargetedCommand(
                        context,
                        portal -> {
                            try {
                                Vec3d rotatingAxis = Vec3Argument.getVec3(
                                    context, "rotatingAxis"
                                ).normalize();
                                
                                double angleDegrees = DoubleArgumentType.getDouble(
                                    context, "angleDegrees"
                                );
                                
                                Quaternion rot = new Quaternion(
                                    new Vector3f(
                                        (float) rotatingAxis.x,
                                        (float) rotatingAxis.y,
                                        (float) rotatingAxis.z
                                    ),
                                    (float) angleDegrees,
                                    true
                                );
                                
                                if (portal.rotation == null) {
                                    portal.rotation = rot;
                                }
                                else {
                                    portal.rotation.multiply(rot);
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
        
        builder.then(Commands.literal("complete_bi_way_portal")
            .executes(context -> processPortalTargetedCommand(
                context,
                portal -> {
                    invokeCompleteBiWayPortal(context, portal);
                }
            ))
        );
        
        builder.then(Commands.literal("complete_bi_faced_portal")
            .executes(context -> processPortalTargetedCommand(
                context,
                portal -> {
                    invokeCompleteBiFacedPortal(context, portal);
                }
            ))
        );
        
        builder.then(Commands.literal("complete_bi_way_bi_faced_portal")
            .executes(context -> processPortalTargetedCommand(
                context,
                portal ->
                    invokeCompleteBiWayBiFacedPortal(context, portal)
            ))
        );
        
        builder.then(Commands.literal("remove_connected_portals")
            .executes(context -> processPortalTargetedCommand(
                context,
                portal -> {
                    PortalManipulation.removeConnectedPortals(
                        portal,
                        p -> sendMessage(context, "Removed " + p)
                    );
                }
            ))
        );
        
        
        builder.then(Commands.literal("move_portal")
            .then(Commands.argument("distance", DoubleArgumentType.doubleArg())
                .executes(context -> processPortalTargetedCommand(
                    context, portal -> {
                        try {
                            double distance =
                                DoubleArgumentType.getDouble(context, "distance");
                            
                            ServerPlayerEntity player = context.getSource().asPlayer();
                            Vec3d viewVector = player.getLookVec();
                            Direction facing = Direction.getFacingFromVector(
                                viewVector.x, viewVector.y, viewVector.z
                            );
                            Vec3d offset = new Vec3d(facing.getDirectionVec()).scale(distance);
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
            )
        );
    
        builder.then(Commands.literal("move_portal_destination")
            .then(Commands.argument("distance", DoubleArgumentType.doubleArg())
                .executes(context -> processPortalTargetedCommand(
                    context, portal -> {
                        try {
                            double distance =
                                DoubleArgumentType.getDouble(context, "distance");
                        
                            ServerPlayerEntity player = context.getSource().asPlayer();
                            Vec3d viewVector = player.getLookVec();
                            Direction facing = Direction.getFacingFromVector(
                                viewVector.x, viewVector.y, viewVector.z
                            );
                            Vec3d offset = new Vec3d(facing.getDirectionVec()).scale(distance);
    
                            portal.destination = portal.destination.add(
                                portal.transformLocalVec(offset)
                            );
                            reloadPortal(portal);
                        }
                        catch (CommandSyntaxException e) {
                            sendMessage(context, "This command can only be invoked by player");
                        }
                    }
                ))
            )
        );
        
        
        builder.then(Commands.literal("set_portal_specific_accessor")
            .executes(context -> processPortalTargetedCommand(
                context,
                portal -> {
                    removeSpecificAccessor(context, portal);
                }
            ))
            .then(Commands.argument("player", EntityArgument.player())
                .executes(context -> processPortalTargetedCommand(
                    context,
                    portal -> {
                        setSpecificAccessor(context, portal,
                            EntityArgument.getEntity(context, "player")
                        );
                    }
                ))
            )
        );
        
        
        builder.then(Commands.literal("multidest")
            .then(Commands.argument("player", EntityArgument.player())
                .executes(context -> processPortalTargetedCommand(
                    context,
                    portal -> {
                        removeMultidestEntry(
                            context, portal, EntityArgument.getPlayer(context, "player")
                        );
                    }
                ))
                .then(Commands.argument("dimension", DimensionArgument.getDimension())
                    .then(Commands.argument("destination", Vec3Argument.vec3(false))
                        .then(Commands.argument("isBiFaced", BoolArgumentType.bool())
                            .then(Commands.argument("isBiWay", BoolArgumentType.bool())
                                .executes(context -> processPortalTargetedCommand(
                                    context,
                                    portal -> {
                                        setMultidestEntry(
                                            context,
                                            portal,
                                            EntityArgument.getPlayer(context, "player"),
                                            DimensionArgument.getDimensionArgument(
                                                context,
                                                "dimension"
                                            ),
                                            Vec3Argument.getVec3(context, "destination"),
                                            BoolArgumentType.getBool(context, "isBiFaced"),
                                            BoolArgumentType.getBool(context, "isBiWay")
                                        );
                                    }
                                ))
                            )
                        )
                    )
                )
            )
        );
        
        builder.then(Commands.literal("make_portal_round")
            .executes(context -> processPortalTargetedCommand(
                context,
                portal -> {
                    makePortalRound(portal);
                    reloadPortal(portal);
                }
            ))
        );
    }
    
    private static void registerCBPortalCommands(
        LiteralArgumentBuilder<CommandSource> builder
    ) {
        builder.then(Commands.literal("cb_set_portal_destination")
            .then(Commands.argument("portal", EntityArgument.entities())
                .then(Commands.argument("dim", DimensionArgument.getDimension())
                    .then(Commands.argument("dest", Vec3Argument.vec3(false))
                        .executes(
                            context -> processPortalArgumentedCommand(
                                context,
                                (portal) -> invokeSetPortalDestination(context, portal)
                            )
                        )
                    )
                )
            )
        );
        
        builder.then(Commands.literal("cb_complete_bi_way_portal")
            .then(Commands.argument("portal", EntityArgument.entities())
                .executes(context -> processPortalArgumentedCommand(
                    context,
                    portal -> {
                        PortalManipulation.removeConnectedPortals(
                            portal,
                            p -> sendMessage(context, "Removed " + p)
                        );
                    }
                ))
            )
        );
        
        
        builder.then(Commands.literal("cb_complete_bi_faced_portal")
            .then(Commands.argument("portal", EntityArgument.entities())
                .executes(context -> processPortalArgumentedCommand(
                    context,
                    portal -> {
                        invokeCompleteBiFacedPortal(context, portal);
                    }
                ))
            )
        );
        
        builder.then(Commands.literal("cb_complete_bi_way_bi_faced_portal")
            .then(Commands.argument("portal", EntityArgument.entities())
                .executes(context -> processPortalArgumentedCommand(
                    context,
                    portal -> {
                        invokeCompleteBiWayBiFacedPortal(context, portal);
                    }
                ))
            )
        );
        
        
        builder.then(Commands.literal("cb_remove_connected_portals")
            .then(Commands.argument("portal", EntityArgument.entities())
                .executes(context -> processPortalArgumentedCommand(
                    context,
                    portal -> {
                        PortalManipulation.removeConnectedPortals(
                            portal,
                            p -> sendMessage(context, "Removed " + p)
                        );
                    }
                ))
            )
        );
        
        
        builder.then(Commands.literal("cb_set_portal_specific_accessor")
            .then(Commands.argument("portal", EntityArgument.entities())
                .executes(context -> {
                    EntityArgument.getEntities(context, "portal")
                        .stream().filter(e -> e instanceof Portal)
                        .forEach(p -> removeSpecificAccessor(context, ((Portal) p)));
                    return 0;
                })
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(context -> {
                        Entity player = EntityArgument.getEntity(context, "player");
                        EntityArgument.getEntities(context, "portal")
                            .stream().filter(e -> e instanceof Portal)
                            .forEach(p -> {
                                setSpecificAccessor(context, ((Portal) p), player);
                            });
                        return 0;
                    })
                )
            )
        );
    }
    
    private static void registerUtilityCommands(
        LiteralArgumentBuilder<CommandSource> builder
    ) {
        builder.then(Commands.literal("tpme")
            .then(Commands.argument("target", EntityArgument.entity())
                .executes(context -> {
                    Entity entity = EntityArgument.getEntity(context, "target");
                    
                    Global.serverTeleportationManager.invokeTpmeCommand(
                        context.getSource().asPlayer(),
                        entity.world.dimension.getType(),
                        entity.getPositionVec()
                    );
                    
                    context.getSource().sendFeedback(
                        new TranslationTextComponent(
                            "imm_ptl.command.tpme.success",
                            entity.getDisplayName()
                        ),
                        true
                    );
                    
                    return 1;
                })
            )
            .then(Commands.argument("dest", Vec3Argument.vec3())
                .executes(context -> {
                    Vec3d dest = Vec3Argument.getVec3(context, "dest");
                    ServerPlayerEntity player = context.getSource().asPlayer();
                    
                    Global.serverTeleportationManager.invokeTpmeCommand(
                        player,
                        player.world.dimension.getType(),
                        dest
                    );
                    
                    context.getSource().sendFeedback(
                        new TranslationTextComponent(
                            "imm_ptl.command.tpme.success",
                            dest.toString()
                        ),
                        true
                    );
                    
                    return 1;
                })
            )
            .then(Commands.argument("dim", DimensionArgument.getDimension())
                .then(Commands.argument("dest", Vec3Argument.vec3())
                    .executes(context -> {
                        DimensionType dim = DimensionArgument.getDimensionArgument(
                            context,
                            "dim"
                        );
                        Vec3d dest = Vec3Argument.getVec3(context, "dest");
                        
                        Global.serverTeleportationManager.invokeTpmeCommand(
                            context.getSource().asPlayer(),
                            dim,
                            dest
                        );
                        
                        context.getSource().sendFeedback(
                            new TranslationTextComponent(
                                "imm_ptl.command.tpme.success",
                                McHelper.dimensionTypeId(dim).toString() + dest.toString()
                            ),
                            true
                        );
                        
                        return 1;
                    })
                )
            )
        );
        
        builder.then(Commands.literal("tp")
            .then(Commands.argument("from", EntityArgument.entities())
                .then(Commands.argument("to", EntityArgument.entity())
                    .executes(context -> {
                        Collection<? extends Entity> entities =
                            EntityArgument.getEntities(context, "from");
                        Entity target = EntityArgument.getEntity(context, "to");
                        
                        int numTeleported = teleport(
                            entities,
                            target.world.dimension.getType(),
                            target.getPositionVec()
                        );
                        
                        context.getSource().sendFeedback(
                            new TranslationTextComponent(
                                "imm_ptl.command.tp.success",
                                numTeleported,
                                target.getDisplayName()
                            ),
                            true
                        );
                        
                        return numTeleported;
                    })
                )
                .then(Commands.argument("dest", Vec3Argument.vec3())
                    .executes(context -> {
                        Collection<? extends Entity> entities =
                            EntityArgument.getEntities(context, "from");
                        Vec3d dest = Vec3Argument.getVec3(context, "dest");
                        
                        int numTeleported = teleport(
                            entities,
                            context.getSource().getWorld().dimension.getType(),
                            dest
                        );
                        
                        context.getSource().sendFeedback(
                            new TranslationTextComponent(
                                "imm_ptl.command.tp.success",
                                numTeleported,
                                dest.toString()
                            ),
                            true
                        );
                        
                        return numTeleported;
                    })
                )
                .then(Commands.argument("dim", DimensionArgument.getDimension())
                    .then(Commands.argument("dest", Vec3Argument.vec3())
                        .executes(context -> {
                            Collection<? extends Entity> entities =
                                EntityArgument.getEntities(context, "from");
                            DimensionType dim = DimensionArgument.getDimensionArgument(
                                context,
                                "dim"
                            );
                            Vec3d dest = Vec3Argument.getVec3(context, "dest");
                            
                            int numTeleported = teleport(
                                entities,
                                context.getSource().getWorld().dimension.getType(),
                                dest
                            );
                            
                            context.getSource().sendFeedback(
                                new TranslationTextComponent(
                                    "imm_ptl.command.tp.success",
                                    numTeleported,
                                    McHelper.dimensionTypeId(dim).toString() + dest.toString()
                                ),
                                true
                            );
                            
                            return numTeleported;
                        })
                    )
                )
            )
        );
        
        
        // By LoganDark :D
        builder.then(Commands.literal("make_portal")
            .then(Commands.argument("width", DoubleArgumentType.doubleArg())
                .then(Commands.argument("height", DoubleArgumentType.doubleArg())
                    .then(Commands.argument("to", DimensionArgument.getDimension())
                        .then(Commands.argument("dest", Vec3Argument.vec3(false))
                            .executes(PortalCommand::placePortalAbsolute)
                        )
                        .then(Commands.literal("shift")
                            .then(Commands.argument("dist", DoubleArgumentType.doubleArg())
                                .executes(PortalCommand::placePortalShift)
                            )
                        )
                    )
                )
            )
        );
        
        builder.then(Commands.literal("goback")
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
        
        builder.then(Commands.literal("create_small_inward_wrapping")
            .then(Commands.argument("p1", Vec3Argument.vec3(false))
                .then(Commands.argument("p2", Vec3Argument.vec3(false))
                    .executes(context -> {
                        Vec3d p1 = Vec3Argument.getVec3(context, "p1");
                        Vec3d p2 = Vec3Argument.getVec3(context, "p2");
                        AxisAlignedBB box = new AxisAlignedBB(p1, p2);
                        ServerWorld world = context.getSource().getWorld();
                        addSmallWorldWrappingPortals(box, world, true);
                        return 0;
                    })
                )
            )
        );
        
        builder.then(Commands.literal("create_small_outward_wrapping")
            .then(Commands.argument("p1", Vec3Argument.vec3(false))
                .then(Commands.argument("p2", Vec3Argument.vec3(false))
                    .executes(context -> {
                        Vec3d p1 = Vec3Argument.getVec3(context, "p1");
                        Vec3d p2 = Vec3Argument.getVec3(context, "p2");
                        AxisAlignedBB box = new AxisAlignedBB(p1, p2);
                        ServerWorld world = context.getSource().getWorld();
                        addSmallWorldWrappingPortals(box, world, false);
                        return 0;
                    })
                )
            )
        );
    }
    
    private static void addSmallWorldWrappingPortals(AxisAlignedBB box, ServerWorld world, boolean isInward) {
        for (Direction direction : Direction.values()) {
            Portal portal = Portal.entityType.create(world);
            WorldWrappingPortal.initWrappingPortal(
                world, box, direction, isInward, portal
            );
            world.addEntity(portal);
        }
    }
    
    public static void register(
        CommandDispatcher<CommandSource> dispatcher
    ) {
        
        LiteralArgumentBuilder<CommandSource> builder = Commands
            .literal("portal")
            .requires(commandSource -> commandSource.hasPermissionLevel(2));
        
        registerPortalTargetedCommands(builder);
        
        registerCBPortalCommands(builder);
        
        registerUtilityCommands(builder);
        
        LiteralArgumentBuilder<CommandSource> global =
            Commands.literal("global");
        registerGlobalPortalCommands(global);
        builder.then(global);
        
        dispatcher.register(builder);
    }
    
    public static int processPortalArgumentedCommand(
        CommandContext<CommandSource> context,
        PortalConsumerThrowsCommandSyntaxException invoker
    ) throws CommandSyntaxException {
        Collection<? extends Entity> entities = EntityArgument.getEntities(
            context, "portal"
        );
        
        for (Entity portalEntity : entities) {
            if (portalEntity instanceof Portal) {
                Portal portal = (Portal) portalEntity;
                
                invoker.accept(portal);
            }
            else {
                sendMessage(context, "The target should be portal");
            }
        }
        
        return 0;
    }
    
    private static void invokeSetPortalDestination(
        CommandContext<CommandSource> context,
        Portal portal
    ) throws CommandSyntaxException {
        portal.dimensionTo = DimensionArgument.getDimensionArgument(
            context, "dim"
        );
        portal.destination = Vec3Argument.getVec3(
            context, "dest"
        );
        
        reloadPortal(portal);
        
        sendMessage(context, portal.toString());
    }
    
    private static void invokeCompleteBiWayBiFacedPortal(
        CommandContext<CommandSource> context,
        Portal portal
    ) {
        PortalManipulation.completeBiWayBiFacedPortal(
            portal,
            p -> sendMessage(context, "Removed " + p),
            p -> sendMessage(context, "Added " + p), Portal.entityType
        );
    }
    
    private static void invokeCompleteBiFacedPortal(
        CommandContext<CommandSource> context,
        Portal portal
    ) {
        PortalManipulation.removeOverlappedPortals(
            ((ServerWorld) portal.world),
            portal.getPositionVec(),
            portal.getNormal().scale(-1),
            p -> Objects.equals(portal.specificPlayerId, p.specificPlayerId),
            p -> sendMessage(context, "Removed " + p)
        );
        
        Portal result = PortalManipulation.completeBiFacedPortal(
            portal,
            Portal.entityType
        );
        sendMessage(context, "Added " + result);
    }
    
    private static void invokeCompleteBiWayPortal(
        CommandContext<CommandSource> context,
        Portal portal
    ) {
        PortalManipulation.removeOverlappedPortals(
            McHelper.getServer().getWorld(portal.dimensionTo),
            portal.destination,
            portal.transformLocalVec(portal.getNormal().scale(-1)),
            p -> Objects.equals(portal.specificPlayerId, p.specificPlayerId),
            p -> sendMessage(context, "Removed " + p)
        );
        
        Portal result = PortalManipulation.completeBiWayPortal(
            portal,
            Portal.entityType
        );
        sendMessage(context, "Added " + result);
    }
    
    private static void removeSpecificAccessor(
        CommandContext<CommandSource> context,
        Portal portal
    ) {
        portal.specificPlayerId = null;
        sendMessage(context, "This portal can be accessed by all players now");
        sendMessage(context, portal.toString());
    }
    
    private static void setSpecificAccessor(
        CommandContext<CommandSource> context,
        Portal portal, Entity player
    ) {
        
        portal.specificPlayerId = player.getUniqueID();
        
        sendMessage(
            context,
            "This portal can only be accessed by " +
                player.getName().getUnformattedComponentText() + " now"
        );
        sendMessage(context, portal.toString());
    }
    
    private static void removeMultidestEntry(
        CommandContext<CommandSource> context,
        Portal pointedPortal,
        ServerPlayerEntity player
    ) {
        PortalManipulation.getPortalClutter(
            pointedPortal.world,
            pointedPortal.getPositionVec(),
            pointedPortal.getNormal(),
            p -> true
        ).stream().filter(
            portal -> player.getUniqueID().equals(portal.specificPlayerId) || portal.specificPlayerId == null
        ).forEach(
            portal -> {
                PortalManipulation.removeConnectedPortals(
                    portal,
                    (p) -> sendMessage(context, "removed " + p.toString())
                );
                sendMessage(context, "removed " + portal.toString());
                portal.remove();
            }
        );
    }
    
    private static void setMultidestEntry(
        CommandContext<CommandSource> context,
        Portal pointedPortal,
        ServerPlayerEntity player,
        DimensionType dimension,
        Vec3d destination,
        boolean biFaced,
        boolean biWay
    ) {
        Portal newPortal = PortalManipulation.copyPortal(
            pointedPortal, Portal.entityType
        );
        
        removeMultidestEntry(context, pointedPortal, player);
        
        newPortal.dimensionTo = dimension;
        newPortal.destination = destination;
        newPortal.specificPlayerId = player.getUniqueID();
        
        newPortal.world.addEntity(newPortal);
        
        if (biFaced && biWay) {
            PortalManipulation.completeBiWayBiFacedPortal(
                newPortal,
                p -> {
                },
                p -> {
                },
                Portal.entityType
            );
        }
        else if (biFaced) {
            PortalManipulation.completeBiFacedPortal(newPortal, Portal.entityType);
        }
        else if (biWay) {
            PortalManipulation.completeBiWayPortal(newPortal, Portal.entityType);
        }
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
        portal.updateCache();
        McHelper.getIEStorage(portal.dimension).resendSpawnPacketToTrackers(portal);
    }
    
    public static void sendMessage(CommandContext<CommandSource> context, String message) {
        context.getSource().sendFeedback(
            new StringTextComponent(message),
            false
        );
    }
    
    /**
     * Gets success message based on the portal {@code portal}.
     *
     * @param portal The portal to send the success message for.
     * @return The success message, as a {@link Text}.
     * @author LoganDark
     */
    private static ITextComponent getMakePortalSuccess(Portal portal) {
        return new TranslationTextComponent(
            "imm_ptl.command.make_portal.success",
            Double.toString(portal.width),
            Double.toString(portal.height),
            McHelper.dimensionTypeId(portal.world.dimension.getType()).toString(),
            portal.getPositionVec().toString(),
            McHelper.dimensionTypeId(portal.dimensionTo).toString(),
            portal.destination.toString()
        );
    }
    
    // By LoganDark :D
    private static int placePortalAbsolute(CommandContext<CommandSource> context) throws CommandSyntaxException {
        double width = DoubleArgumentType.getDouble(context, "width");
        double height = DoubleArgumentType.getDouble(context, "height");
        DimensionType to = DimensionArgument.getDimensionArgument(context, "to");
        Vec3d dest = Vec3Argument.getVec3(context, "dest");
        
        Portal portal = Helper.placePortal(width, height, context.getSource().asPlayer());
        
        if (portal == null) {
            return 0;
        }
        
        portal.dimensionTo = to;
        portal.destination = dest;
        portal.world.addEntity(portal);
        
        context.getSource().sendFeedback(getMakePortalSuccess(portal), true);
        
        return 1;
    }
    
    // By LoganDark :D
    private static int placePortalShift(CommandContext<CommandSource> context) throws CommandSyntaxException {
        double width = DoubleArgumentType.getDouble(context, "width");
        double height = DoubleArgumentType.getDouble(context, "height");
        DimensionType to = DimensionArgument.getDimensionArgument(context, "to");
        double dist = DoubleArgumentType.getDouble(context, "dist");
        
        Portal portal = Helper.placePortal(width, height, context.getSource().asPlayer());
        
        if (portal == null) {
            return 0;
        }
        
        // unsafe to use getContentDirection before the destination is fully set
        portal.dimensionTo = to;
        portal.destination = portal.getPositionVec().add(portal.axisW.crossProduct(portal.axisH).scale(-dist));
        portal.world.addEntity(portal);
        
        context.getSource().sendFeedback(getMakePortalSuccess(portal), true);
        
        return 1;
    }
    
    private static int teleport(
        Collection<? extends Entity> entities,
        DimensionType targetDim,
        Vec3d targetPos
    ) {
        ServerWorld targetWorld = McHelper.getServer().getWorld(targetDim);
        
        int numTeleported = 0;
        
        for (Entity entity : entities) {
            if (entity instanceof ServerPlayerEntity) {
                Global.serverTeleportationManager.invokeTpmeCommand(
                    (ServerPlayerEntity) entity, targetDim, targetPos
                );
            }
            else {
                if (targetWorld == entity.world) {
                    entity.setLocationAndAngles(
                        targetPos.x,
                        targetPos.y,
                        targetPos.z,
                        entity.rotationYaw,
                        entity.rotationPitch
                    );
                    entity.setRotationYawHead(entity.rotationYaw);
                }
                else {
                    O_O.segregateServerEntity((ServerWorld) entity.world, entity);
                    McHelper.setPosAndLastTickPos(entity, targetPos, targetPos);
                    McHelper.updateBoundingBox(entity);
                    entity.world = targetWorld;
                    entity.dimension = targetWorld.dimension.getType();
                    targetWorld.func_217460_e(entity);
                }
            }
            
            numTeleported++;
        }
        
        return numTeleported;
    }
    
    public static interface PortalConsumerThrowsCommandSyntaxException {
        void accept(Portal portal) throws CommandSyntaxException;
    }
    
    public static int processPortalTargetedCommand(
        CommandContext<CommandSource> context,
        PortalConsumerThrowsCommandSyntaxException processCommand
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
            portalStream = Streams.concat(
                portalStream,
                globalPortals.stream()
            );
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
    
    private static void makePortalRound(Portal portal) {
        GeometryPortalShape shape = new GeometryPortalShape();
        final int triangleNum = 30;
        double twoPi = Math.PI * 2;
        shape.triangles = IntStream.range(0, triangleNum)
            .mapToObj(i -> new GeometryPortalShape.TriangleInPlane(
                0, 0,
                portal.width * 0.5 * Math.cos(twoPi * ((double) i) / triangleNum),
                portal.height * 0.5 * Math.sin(twoPi * ((double) i) / triangleNum),
                portal.width * 0.5 * Math.cos(twoPi * ((double) i + 1) / triangleNum),
                portal.height * 0.5 * Math.sin(twoPi * ((double) i + 1) / triangleNum)
            )).collect(Collectors.toList());
        portal.specialShape = shape;
        portal.cullableXStart = 0;
        portal.cullableXEnd = 0;
        portal.cullableYStart = 0;
        portal.cullableYEnd = 0;
    }
}
