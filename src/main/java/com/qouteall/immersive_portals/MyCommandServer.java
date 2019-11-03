package com.qouteall.immersive_portals;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.qouteall.immersive_portals.portal.global_portals.BorderPortal;
import com.qouteall.immersive_portals.portal.global_portals.EndFloorPortal;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;

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
        
        dispatcher.register(builder);
    }
    
    
}
