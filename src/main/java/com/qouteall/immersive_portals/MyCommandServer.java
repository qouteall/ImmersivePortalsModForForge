package com.qouteall.immersive_portals;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.qouteall.immersive_portals.portal.global_portals.BorderPortal;
import com.qouteall.immersive_portals.portal.global_portals.EndFloorPortal;
import com.qouteall.immersive_portals.portal.nether_portal.NewNetherPortalEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.server.ServerWorld;

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
        
        dispatcher.register(builder);
    }
    
    
}
