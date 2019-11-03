package com.qouteall.immersive_portals;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.qouteall.immersive_portals.chunk_loading.MyClientChunkManager;
import com.qouteall.immersive_portals.ducks.*;
import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.DimensionRenderHelper;
import com.qouteall.immersive_portals.render.MyRenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.lang.ref.Reference;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@OnlyIn(Dist.CLIENT)
public class MyCommandClient {
    
    public static void register(
        CommandDispatcher<CommandSource> dispatcher
    ) {
        //for composite command arguments, put into then() 's bracket
        //for parallel command arguments, put behind then()
        
        LiteralArgumentBuilder<CommandSource> builder = Commands
            .literal("immersive_portals_debug")
            .requires(commandSource -> true)
            .then(Commands
                .literal("set_max_portal_layer")
                .then(Commands
                    .argument(
                        "argMaxPortalLayer", IntegerArgumentType.integer()
                    )
                    .executes(context -> setMaxPortalLayer(
                        IntegerArgumentType.getInteger(context, "argMaxPortalLayer")
                    ))
                )
            );
        builder = builder.then(Commands
            .literal("list_nearby_portals")
            .executes(context -> listNearbyPortals(context))
        );
        builder = builder.then(Commands
            .literal("is_client_chunk_loaded")
            .then(Commands
                .argument(
                    "chunkX", IntegerArgumentType.integer()
                )
                .then(Commands
                    .argument(
                        "chunkZ", IntegerArgumentType.integer()
                    )
                    .executes(
                        MyCommandClient::isClientChunkLoaded
                    )
                )
            )
        );
        builder = builder.then(Commands
            .literal("is_server_chunk_loaded")
            .then(Commands
                .argument(
                    "chunkX", IntegerArgumentType.integer()
                )
                .then(Commands
                    .argument(
                        "chunkZ", IntegerArgumentType.integer()
                    )
                    .executes(
                        context -> {
                            int chunkX = IntegerArgumentType.getInteger(context, "chunkX");
                            int chunkZ = IntegerArgumentType.getInteger(context, "chunkZ");
                            ServerPlayerEntity player = context.getSource().asPlayer();
                            IChunk chunk = Helper.getServer()
                                .getWorld(player.dimension)
                                .getChunk(
                                    chunkX, chunkZ,
                                    ChunkStatus.FULL, false
                                );
                            Helper.serverLog(
                                player,
                                chunk != null && !(chunk instanceof EmptyChunk) ? "yes" : "no"
                            );
                            return 0;
                        }
                    )
                )
            )
        );
        builder = builder.then(Commands
            .literal("add_portal")
            .executes(context -> addPortal(context))
        );
        builder = builder.then(Commands
            .literal("report_player_status")
            .executes(context -> reportPlayerStatus(context))
        );
        builder = builder.then(Commands
            .literal("client_remote_ticking_enable")
            .executes(context -> {
                CGlobal.isClientRemoteTickingEnabled = true;
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("client_remote_ticking_disable")
            .executes(context -> {
                CGlobal.isClientRemoteTickingEnabled = false;
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("advanced_frustum_culling_enable")
            .executes(context -> {
                CGlobal.doUseAdvancedFrustumCulling = true;
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("advanced_frustum_culling_disable")
            .executes(context -> {
                CGlobal.doUseAdvancedFrustumCulling = false;
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("multithreaded_chunk_loading_enable")
            .executes(context -> {
                SGlobal.isChunkLoadingMultiThreaded = true;
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("multithreaded_chunk_loading_disable")
            .executes(context -> {
                SGlobal.isChunkLoadingMultiThreaded = false;
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("hacked_chunk_render_dispatcher_enable")
            .executes(context -> {
                CGlobal.useHackedChunkRenderDispatcher = true;
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("hacked_chunk_render_dispatcher_disable")
            .executes(context -> {
                CGlobal.useHackedChunkRenderDispatcher = false;
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("front_culling_enable")
            .executes(context -> {
                CGlobal.useFrontCulling = true;
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("front_culling_disable")
            .executes(context -> {
                CGlobal.useFrontCulling = false;
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("switch_to_normal_renderer")
            .executes(context -> {
                Minecraft.getInstance().execute(() -> {
                    CGlobal.renderer = CGlobal.rendererUsingStencil;
                });
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("report_server_entities")
            .executes(context -> {
                ServerPlayerEntity player = context.getSource().asPlayer();
                List<Entity> entities = player.world.getEntitiesWithinAABB(
                    Entity.class,
                    new AxisAlignedBB(player.getPosition()).grow(32)
                );
                Helper.serverLog(player, entities.toString());
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("report_resource_consumption")
            .executes(MyCommandClient::reportResourceConsumption)
        );
        builder = builder.then(Commands
            .literal("report_render_info_num")
            .executes(context -> {
                String str = Helper.myToString(CGlobal.renderInfoNumMap.entrySet().stream());
                context.getSource().asPlayer().sendMessage(new StringTextComponent(str));
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("rebuild_all")
            .executes(context -> {
                Minecraft.getInstance().execute(() -> {
                    ((IEChunkRenderDispatcher)
                        ((IEWorldRenderer) Minecraft.getInstance().worldRenderer)
                            .getChunkRenderDispatcher()
                    ).rebuildAll();
                });
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("shader_debug_enable")
            .executes(context -> {
                CGlobal.isRenderDebugMode = true;
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("shader_debug_disable")
            .executes(context -> {
                CGlobal.isRenderDebugMode = false;
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("get_player_colliding_portal_client")
            .executes(context -> {
                Portal collidingPortal =
                    ((IEEntity) Minecraft.getInstance().player).getCollidingPortal();
                Helper.serverLog(context.getSource().asPlayer(), collidingPortal.toString());
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("report_rendering")
            .executes(context -> {
                String str = MyRenderHelper.lastPortalRenderInfos
                    .stream()
                    .map(
                        list -> list.stream()
                            .map(Reference::get)
                            .collect(Collectors.toList())
                    )
                    .collect(Collectors.toList())
                    .toString();
                Helper.serverLog(context.getSource().asPlayer(), str);
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("debug_mirror_mode_enable")
            .executes(context -> {
                CGlobal.debugMirrorMode = true;
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("debug_mirror_mode_disable")
            .executes(context -> {
                CGlobal.debugMirrorMode = false;
                return 0;
            })
        );
        
        dispatcher.register(builder);
        
        Helper.log("Successfully initialized command /immersive_portals_debug");
    }
    
    private static int reportFogColor(CommandContext<CommandSource> context) throws CommandSyntaxException {
        StringBuilder str = new StringBuilder();
        
        CGlobal.clientWorldLoader.clientWorldMap.values().forEach(world -> {
            DimensionRenderHelper helper =
                CGlobal.clientWorldLoader.getDimensionRenderHelper(
                    world.dimension.getType()
                );
            str.append(String.format(
                "%s %s %s %s\n",
                world.dimension.getType(),
                helper.fogRenderer,
                helper.getFogColor(),
                ((IEBackgroundRenderer) helper.fogRenderer).getDimensionConstraint()
            ));
        });
        
        FogRenderer currentFogRenderer = ((IEGameRenderer) Minecraft.getInstance()
            .gameRenderer
        ).getBackgroundRenderer();
        str.append(String.format(
            "current: %s %s \n switched %s \n",
            currentFogRenderer,
            ((IEBackgroundRenderer) currentFogRenderer).getDimensionConstraint(),
            CGlobal.switchedFogRenderer
        ));
        
        String result = str.toString();
        
        Helper.log(str);
    
        context.getSource().asPlayer().sendMessage(new StringTextComponent(result));
        
        return 0;
    }
    
    private static int reportResourceConsumption(CommandContext<CommandSource> context) throws CommandSyntaxException {
        StringBuilder str = new StringBuilder();
        
        str.append("Client Chunk:\n");
        CGlobal.clientWorldLoader.clientWorldMap.values().forEach(world -> {
            str.append(String.format(
                "%s %s\n",
                world.dimension.getType(),
                ((MyClientChunkManager) world.getChunkProvider()).getChunkNum()
            ));
        });
        
        
        str.append("Chunk Renderers:\n");
        CGlobal.clientWorldLoader.worldRendererMap.forEach(
            (dimension, worldRenderer) -> {
                str.append(String.format(
                    "%s %s\n",
                    dimension,
                    ((IEChunkRenderDispatcher) ((IEWorldRenderer) worldRenderer)
                        .getChunkRenderDispatcher()
                    ).getEmployedRendererNum()
                ));
            }
        );
    
        str.append("Server Chunks:\n");
        Helper.getServer().getWorlds().forEach(
            world -> {
                str.append(String.format(
                    "%s %s\n",
                    world.dimension.getType(),
                    world.getForcedChunks().size()
                ));
            }
        );
        
        String result = str.toString();
        
        Helper.log(str);
    
        context.getSource().asPlayer().sendMessage(new StringTextComponent(result));
        
        return 0;
    }
    
    private static int isClientChunkLoaded(CommandContext<CommandSource> context) throws CommandSyntaxException {
        int chunkX = IntegerArgumentType.getInteger(context, "chunkX");
        int chunkZ = IntegerArgumentType.getInteger(context, "chunkZ");
        IChunk chunk = Minecraft.getInstance().world.getChunk(
            chunkX, chunkZ
        );
        Helper.serverLog(
            context.getSource().asPlayer(),
            chunk != null && !(chunk instanceof EmptyChunk) ? "yes" : "no"
        );
        return 0;
    }
    
    private static int setMaxPortalLayer(int m) {
        CGlobal.maxPortalLayer = m;
        return 0;
    }
    
    private static int listNearbyPortals(CommandContext<CommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity playerServer = context.getSource().asPlayer();
        ClientPlayerEntity playerClient = Minecraft.getInstance().player;
        
        Helper.serverLog(playerServer, "Server Portals");
        Helper.serverLog(
            playerServer,
            Helper.myToString(
                Helper.getEntitiesNearby(
                    playerServer, Portal.class, 64
                )
            )
        );
        
        Helper.serverLog(playerServer, "Client Portals");
        Helper.serverLog(
            playerServer,
            Helper.myToString(
                Helper.getEntitiesNearby(
                    playerClient, Portal.class, 64
                )
            )
        );
        
        return 0;
    }
    
    private static Consumer<ServerPlayerEntity> originalAddPortalFunctionality;
    private static Consumer<ServerPlayerEntity> addPortalFunctionality;
    
    static {
        originalAddPortalFunctionality = (player) -> {
            Vec3d fromPos = player.getPositionVec();
            Vec3d fromNormal = player.getLookVec().scale(-1);
            ServerWorld fromWorld = ((ServerWorld) player.world);
            
            addPortalFunctionality = (playerEntity) -> {
                Vec3d toPos = playerEntity.getPositionVec();
                DimensionType toDimension = player.dimension;
    
                Portal portal = Portal.entityType.create(fromWorld);
                portal.posX = fromPos.x;
                portal.posY = fromPos.y;
                portal.posZ = fromPos.z;
    
                portal.axisH = new Vec3d(0, 4, 0);
                portal.axisW = portal.axisH.crossProduct(fromNormal).normalize().scale(4);
                
                portal.dimensionTo = toDimension;
                portal.destination = toPos;
                
                assert portal.isPortalValid();
    
                fromWorld.addEntity(portal);
                
                addPortalFunctionality = originalAddPortalFunctionality;
            };
        };
        
        addPortalFunctionality = originalAddPortalFunctionality;
    }
    
    private static int addPortal(CommandContext<CommandSource> context) {
        try {
            addPortalFunctionality.accept(context.getSource().asPlayer());
        }
        catch (CommandSyntaxException e) {
            e.printStackTrace();
        }
        return 0;
    }
    
    private static int reportPlayerStatus(CommandContext<CommandSource> context) throws CommandSyntaxException {
        //only invoked on single player
    
        ServerPlayerEntity playerMP = context.getSource().asPlayer();
        ClientPlayerEntity playerSP = Minecraft.getInstance().player;
        
        Helper.serverLog(
            playerMP,
            "On Server " + playerMP.dimension + " " + playerMP.getPosition()
        );
        Helper.serverLog(
            playerMP,
            "On Client " + playerSP.dimension + " " + playerSP.getPosition()
        );
        return 0;
    }
}
