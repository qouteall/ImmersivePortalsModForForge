package com.qouteall.immersive_portals.commands;

import com.google.common.collect.Streams;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.qouteall.immersive_portals.CGlobal;
import com.qouteall.immersive_portals.ClientWorldLoader;
import com.qouteall.immersive_portals.Global;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.chunk_loading.ChunkVisibility;
import com.qouteall.immersive_portals.chunk_loading.NewChunkTrackingGraph;
import com.qouteall.immersive_portals.ducks.IEEntity;
import com.qouteall.immersive_portals.ducks.IEWorldRenderer;
import com.qouteall.immersive_portals.my_util.MyTaskList;
import com.qouteall.immersive_portals.network.McRemoteProcedureCall;
import com.qouteall.immersive_portals.optifine_compatibility.UniformReport;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.render.MyBuiltChunkStorage;
import com.qouteall.immersive_portals.render.PortalRenderInfo;
import com.qouteall.immersive_portals.render.PortalRenderingGroup;
import com.qouteall.immersive_portals.render.context_management.RenderStates;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.IntNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.DimensionType;
import net.minecraft.world.LightType;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.EmptyChunk;
import net.minecraft.world.chunk.IChunk;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.settings.DimensionGeneratorSettings;
import net.minecraft.world.server.ServerWorldLightManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import java.lang.ref.Reference;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@OnlyIn(Dist.CLIENT)
public class ClientDebugCommand {
    
    public static void register(
        CommandDispatcher<CommandSource> dispatcher
    ) {
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
            .literal("list_portals")
            .executes(context -> listPortals(context))
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
                        ClientDebugCommand::isClientChunkLoaded
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
                            IChunk chunk = McHelper.getServer()
                                .getWorld(player.world.func_234923_W_())
                                .getChunk(
                                    chunkX, chunkZ,
                                    ChunkStatus.FULL, false
                                );
                            McHelper.serverLog(
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
            .literal("report_server_entities")
            .executes(context -> {
                ServerPlayerEntity player = context.getSource().asPlayer();
                List<Entity> entities = player.world.getEntitiesWithinAABB(
                    Entity.class,
                    new AxisAlignedBB(player.getPositionVec(), player.getPositionVec()).grow(32),
                    e -> true
                );
                McHelper.serverLog(player, entities.toString());
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("report_resource_consumption")
            .executes(ClientDebugCommand::reportResourceConsumption)
        );
        builder = builder.then(Commands
            .literal("report_render_info_num")
            .executes(context -> {
                String str = Helper.myToString(CGlobal.renderInfoNumMap.entrySet().stream());
                context.getSource().asPlayer().sendStatusMessage(new StringTextComponent(str), false);
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("get_player_colliding_portal_client")
            .executes(context -> {
                Portal collidingPortal =
                    ((IEEntity) Minecraft.getInstance().player).getCollidingPortal();
                McHelper.serverLog(
                    context.getSource().asPlayer(),
                    collidingPortal != null ? collidingPortal.toString() : "null"
                );
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("report_rendering")
            .executes(context -> {
                String str = RenderStates.lastPortalRenderInfos
                    .stream()
                    .map(
                        list -> list.stream()
                            .map(Reference::get)
                            .collect(Collectors.toList())
                    )
                    .collect(Collectors.toList())
                    .toString();
                McHelper.serverLog(context.getSource().asPlayer(), str);
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("vanilla_chunk_culling_enable")
            .executes(context -> {
                Minecraft.getInstance().renderChunksMany = true;
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("vanilla_chunk_culling_disable")
            .executes(context -> {
                Minecraft.getInstance().renderChunksMany = false;
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("render_mode_normal")
            .executes(context -> {
                Global.renderMode = Global.RenderMode.normal;
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("render_mode_compatibility")
            .executes(context -> {
                Global.renderMode = Global.RenderMode.compatibility;
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("render_mode_debug")
            .executes(context -> {
                Global.renderMode = Global.RenderMode.debug;
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("render_mode_none")
            .executes(context -> {
                Global.renderMode = Global.RenderMode.none;
                return 0;
            })
        );
        builder.then(Commands
            .literal("report_chunk_loaders")
            .executes(context -> {
                ServerPlayerEntity player = context.getSource().asPlayer();
                ChunkVisibility.getBaseChunkLoaders(
                    player
                ).forEach(
                    loader -> McHelper.serverLog(
                        player, loader.toString()
                    )
                );
                return 0;
            })
        );
        builder.then(Commands
            .literal("check_client_light")
            .executes(context -> {
                Minecraft client = Minecraft.getInstance();
                client.execute(() -> {
                    client.world.getChunkProvider().getLightManager().updateSectionStatus(
                        SectionPos.from(new BlockPos(client.player.getPositionVec())),
                        false
                    );
                });
                return 0;
            })
        );
        builder.then(Commands
            .literal("check_server_light")
            .executes(context -> {
                McHelper.getServer().execute(() -> {
                    ServerPlayerEntity player = McHelper.getRawPlayerList().get(0);
                    
                    BlockPos.getAllInBox(
                        player.func_233580_cy_().add(-2, -2, -2),
                        player.func_233580_cy_().add(2, 2, 2)
                    ).forEach(blockPos -> {
                        player.world.getLightManager().checkBlock(blockPos);
                    });
                });
                return 0;
            })
        );
        builder.then(Commands
                .literal("update_server_light")
                .executes(context -> {
                    McHelper.getServer().execute(() -> {
                        ServerPlayerEntity player = McHelper.getRawPlayerList().get(0);
                        
                        ServerWorldLightManager lightingProvider = (ServerWorldLightManager) player.world.getLightManager();
                        lightingProvider.lightChunk(
                            player.world.getChunk(player.chunkCoordX, player.chunkCoordZ),
                            false
                        );
//                    lightingProvider.light(
//                        player.world.getChunk(player.chunkX, player.chunkZ),
//                        true
//                    );
                    });
                    return 0;
                })
        );
        builder = builder.then(Commands
            .literal("uniform_report_textured")
            .executes(context -> {
                UniformReport.launchUniformReport(
                    new String[]{
                        "gbuffers_textured", "gbuffers_textured_lit"
                    },
                    s -> context.getSource().sendFeedback(
                        new StringTextComponent(s), true
                    )
                );
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("uniform_report_terrain")
            .executes(context -> {
                UniformReport.launchUniformReport(
                    new String[]{
                        "gbuffers_terrain", "gbuffers_terrain_solid"
                    },
                    s -> context.getSource().sendFeedback(
                        new StringTextComponent(s), true
                    )
                );
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal("uniform_report_shadow")
            .executes(context -> {
                UniformReport.launchUniformReport(
                    new String[]{
                        "shadow_solid", "shadow"
                    },
                    s -> context.getSource().sendFeedback(
                        new StringTextComponent(s), true
                    )
                );
                return 0;
            })
        );
        builder.then(Commands
            .literal("erase_chunk")
            .then(Commands.argument("rChunks", IntegerArgumentType.integer())
                .executes(context -> {
                    ServerPlayerEntity player = context.getSource().asPlayer();
                    
                    ChunkPos center = new ChunkPos(new BlockPos(player.getPositionVec()));
                    
                    invokeEraseChunk(
                        player.world, center,
                        IntegerArgumentType.getInteger(context, "rChunks"),
                        0, 256
                    );
                    
                    return 0;
                })
                .then(Commands.argument("downY", IntegerArgumentType.integer())
                    .then(Commands.argument("upY", IntegerArgumentType.integer())
                        .executes(context -> {
                            
                            ServerPlayerEntity player = context.getSource().asPlayer();
                            
                            ChunkPos center = new ChunkPos(new BlockPos(player.getPositionVec()));
                            
                            invokeEraseChunk(
                                player.world, center,
                                IntegerArgumentType.getInteger(context, "rChunks"),
                                IntegerArgumentType.getInteger(context, "downY"),
                                IntegerArgumentType.getInteger(context, "upY")
                            );
                            return 0;
                        })
                    )
                )
            )
        );
        builder.then(Commands
            .literal("report_rebuild_status")
            .executes(context -> {
                ServerPlayerEntity player = context.getSource().asPlayer();
                Minecraft.getInstance().execute(() -> {
                    ClientWorldLoader.getClientWorlds().forEach((world) -> {
                        MyBuiltChunkStorage builtChunkStorage = (MyBuiltChunkStorage) ((IEWorldRenderer)
                            ClientWorldLoader.getWorldRenderer(world.func_234923_W_()))
                            .getBuiltChunkStorage();
                        McHelper.serverLog(
                            player,
                            world.func_234923_W_().func_240901_a_().toString() + builtChunkStorage.getDebugString()
                        );
                    });
                });
                
                return 0;
            })
        );
        builder.then(Commands
            .literal("test")
            .executes(context -> {
//                ServerWorld serverWorld = context.getSource().getWorld();
//                Portal portal = Portal.entityType.create(serverWorld);
//
//                portal.setOriginPos(new Vec3d(0, 70, 0));
//                portal.setDestinationDimension(World.NETHER);
//                portal.setDestination(new Vec3d(100, 70, 100));
//                portal.setOrientationAndSize(
//                    new Vec3d(1, 0, 0),
//                    new Vec3d(0, 1, 0),
//                    4,
//                    4
//                );
//
//                portal.world.spawnEntity(portal);
//
//                ServerPlayerEntity serverPlayerEntity = context.getSource().getPlayer();
//
//                PortalAPI.addChunkLoaderForPlayer(
//                    serverPlayerEntity, new ChunkLoader(
//                        new DimensionalChunkPos(
//                            World.OVERWORLD,
//                            100,//x
//                            100//z
//                        ),
//                        3//radius
//                    )
//                );
                
                return 0;
            })
        );
        builder.then(Commands
            .literal("print_generator_config")
            .executes(context -> {
                McHelper.getServer().getWorlds().forEach(world -> {
                    ChunkGenerator generator = world.getChunkProvider().getChunkGenerator();
                    Helper.log(world.func_234923_W_().func_240901_a_());
                    Helper.log(McHelper.serializeToJson(generator, ChunkGenerator.field_235948_a_));
                    Helper.log(McHelper.serializeToJson(
                        world.func_230315_m_(),
                        DimensionType.field_235997_a_.stable()
                    ));
                });
                
                DimensionGeneratorSettings options = McHelper.getServer().func_240793_aU_().func_230418_z_();
                
                Helper.log(McHelper.serializeToJson(options, DimensionGeneratorSettings.field_236201_a_));
                
                return 0;
            })
        );
        builder.then(Commands
            .literal("report_portal_groups")
            .executes(context -> {
                for (ClientWorld clientWorld : ClientWorldLoader.getClientWorlds()) {
                    Map<Optional<PortalRenderingGroup>, List<Portal>> result =
                        Streams.stream(clientWorld.getAllEntities())
                            .flatMap(
                                entity -> entity instanceof Portal ?
                                    Stream.of(((Portal) entity)) : Stream.empty()
                            )
                            .collect(Collectors.groupingBy(
                                p -> Optional.ofNullable(PortalRenderInfo.getGroupOf(p))
                            ));
                    final ServerPlayerEntity player = context.getSource().asPlayer();
                    McHelper.serverLog(player, "\n" + clientWorld.func_234923_W_().func_240901_a_().toString());
                    result.forEach((g, l) -> {
                        McHelper.serverLog(player, "\n" + g.toString());
                        McHelper.serverLog(player, l.stream()
                            .map(Portal::toString).collect(Collectors.joining("\n"))
                        );
                    });
                }
                return 0;
            })
        );
        builder.then(Commands
            .literal("report_client_light_status")
            .executes(context -> {
                Minecraft.getInstance().execute(() -> {
                    ClientPlayerEntity player = Minecraft.getInstance().player;
                    NibbleArray lightSection = player.world.getLightManager().getLightEngine(LightType.BLOCK).getData(
                        SectionPos.of(player.chunkCoordX, player.chunkCoordY, player.chunkCoordZ)
                    );
                    if (lightSection != null) {
                        boolean uninitialized = lightSection.isEmpty();
                        
                        byte[] byteArray = lightSection.getData();
                        boolean allZero = true;
                        for (byte b : byteArray) {
                            if (b != 0) {
                                allZero = false;
                                break;
                            }
                        }
                        
                        context.getSource().sendFeedback(
                            new StringTextComponent(
                                "has light section " +
                                    (allZero ? "all zero" : "not all zero") +
                                    (uninitialized ? " uninitialized" : " fine")
                            ),
                            false
                        );
                    }
                    else {
                        context.getSource().sendFeedback(
                            new StringTextComponent("does not have light section"), false
                        );
                    }
                });
                return 0;
            })
        );
        builder.then(Commands
            .literal("remote_procedure_call_test")
            .executes(context -> {
                testRemoteProcedureCall(context.getSource().asPlayer());
                return 0;
            })
        );
        registerSwitchCommand(
            builder,
            "front_clipping",
            cond -> CGlobal.useFrontClipping = cond
        );
        registerSwitchCommand(
            builder,
            "gl_check_error",
            cond -> Global.doCheckGlError = cond
        );
        registerSwitchCommand(
            builder,
            "smooth_chunk_unload",
            cond -> CGlobal.smoothChunkUnload = cond
        );
        
        registerSwitchCommand(
            builder,
            "early_light_update",
            cond -> CGlobal.earlyClientLightUpdate = cond
        );
        registerSwitchCommand(
            builder,
            "super_advanced_frustum_culling",
            cond -> CGlobal.useSuperAdvancedFrustumCulling = cond
        );
        
        registerSwitchCommand(
            builder,
            "teleportation_debug",
            cond -> Global.teleportationDebugEnabled = cond
        );
        registerSwitchCommand(
            builder,
            "cross_portal_entity_rendering",
            cond -> Global.correctCrossPortalEntityRendering = cond
        );
        registerSwitchCommand(
            builder,
            "loose_visible_chunk_iteration",
            cond -> Global.looseVisibleChunkIteration = cond
        );
        registerSwitchCommand(
            builder,
            "early_cull_portal",
            cond -> CGlobal.earlyFrustumCullingPortal = cond
        );
        registerSwitchCommand(
            builder,
            "cache_gl_buffer",
            cond -> Global.cacheGlBuffer = cond
        );
        registerSwitchCommand(
            builder,
            "add_custom_ticket_for_direct_loading_delayed",
            cond -> NewChunkTrackingGraph.addCustomTicketForDirectLoadingDelayed = cond
        );
        registerSwitchCommand(
            builder,
            "server_smooth_loading",
            cond -> Global.serverSmoothLoading = cond
        );
        registerSwitchCommand(
            builder,
            "secondary_vertex_consumer",
            cond -> Global.useSecondaryEntityVertexConsumer = cond
        );
        registerSwitchCommand(
            builder,
            "cull_sections_behind",
            cond -> Global.cullSectionsBehind = cond
        );
        registerSwitchCommand(
            builder,
            "offset_occlusion_query",
            cond -> Global.offsetOcclusionQuery = cond
        );
        registerSwitchCommand(
            builder,
            "cloud_optimization",
            cond -> Global.cloudOptimization = cond
        );
        registerSwitchCommand(
            builder,
            "cross_portal_collision",
            cond -> Global.crossPortalCollision = cond
        );
        registerSwitchCommand(
            builder,
            "light_logging",
            cond -> Global.lightLogging = cond
        );
        registerSwitchCommand(
            builder,
            "disable_fog",
            cond -> Global.debugDisableFog = cond
        );
        
        builder.then(Commands
            .literal("print_class_path")
            .executes(context -> {
                printClassPath();
                return 0;
            })
        );
        
        dispatcher.register(builder);
        
        Helper.log("Successfully initialized command /immersive_portals_debug");
    }
    
    public static void invokeEraseChunk(World world, ChunkPos center, int r, int downY, int upY) {
        ArrayList<ChunkPos> poses = new ArrayList<>();
        for (int x = -r; x <= r; x++) {
            for (int z = -r; z <= r; z++) {
                poses.add(new ChunkPos(x + center.x, z + center.z));
            }
        }
        poses.sort(Comparator.comparingDouble(c ->
            Vector3d.func_237491_b_(center.asBlockPos()).distanceTo(Vector3d.func_237491_b_(c.asBlockPos()))
        ));
        
        ModMain.serverTaskList.addTask(MyTaskList.chainTasks(
            poses.stream().map(chunkPos -> (MyTaskList.MyTask) () -> {
                eraseChunk(
                    chunkPos, world, downY, upY
                );
                return true;
            }).iterator()
        ));
    }
    
    public static void eraseChunk(ChunkPos chunkPos, World world, int yStart, int yEnd) {
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = yStart; y < yEnd; y++) {
                    world.setBlockState(
                        new BlockPos(
                            chunkPos.getXStart() + x,
                            y,
                            chunkPos.getZStart() + z
                        ),
                        Blocks.AIR.getDefaultState()
                    );
                }
            }
        }
    }
    
    private static void printClassPath() {
        System.out.println(
            Arrays.stream(
                ((URLClassLoader) ClassLoader.getSystemClassLoader()).getURLs()
            ).map(
                url -> "\"" + url.getFile().substring(1).replace("%20", " ") + "\""
            ).collect(Collectors.joining(",\n"))
        );
    }
    
    private static void registerSwitchCommand(
        LiteralArgumentBuilder<CommandSource> builder,
        String name,
        Consumer<Boolean> setFunction
    ) {
        builder = builder.then(Commands
            .literal(name + "_enable")
            .executes(context -> {
                setFunction.accept(true);
                return 0;
            })
        );
        builder = builder.then(Commands
            .literal(name + "_disable")
            .executes(context -> {
                setFunction.accept(false);
                return 0;
            })
        );
    }
    
    private static int reportResourceConsumption(CommandContext<CommandSource> context) throws CommandSyntaxException {
        StringBuilder str = new StringBuilder();
        
        str.append("Client Chunk:\n");
        ClientWorldLoader.getClientWorlds().forEach(world -> {
            str.append(String.format(
                "%s %s\n",
                world.func_234923_W_().func_240901_a_(),
                world.getChunkProvider().getLoadedChunksCount()
            ));
        });
        
        
        str.append("Chunk Mesh Sections:\n");
        ClientWorldLoader.worldRendererMap.forEach(
            (dimension, worldRenderer) -> {
                str.append(String.format(
                    "%s %s\n",
                    dimension.func_240901_a_(),
                    ((MyBuiltChunkStorage) ((IEWorldRenderer) worldRenderer)
                        .getBuiltChunkStorage()
                    ).getManagedChunkNum()
                ));
            }
        );
        
        str.append("Server Chunks:\n");
        McHelper.getServer().getWorlds().forEach(
            world -> {
                str.append(String.format(
                    "%s %s\n",
                    world.func_234923_W_().func_240901_a_(),
                    NewChunkTrackingGraph.getLoadedChunkNum(world.func_234923_W_())
                ));
            }
        );
        
        String result = str.toString();
        
        Helper.log(str);
        
        context.getSource().asPlayer().sendStatusMessage(new StringTextComponent(result), false);
        
        return 0;
    }
    
    private static int isClientChunkLoaded(CommandContext<CommandSource> context) throws CommandSyntaxException {
        int chunkX = IntegerArgumentType.getInteger(context, "chunkX");
        int chunkZ = IntegerArgumentType.getInteger(context, "chunkZ");
        IChunk chunk = Minecraft.getInstance().world.getChunk(
            chunkX, chunkZ
        );
        McHelper.serverLog(
            context.getSource().asPlayer(),
            chunk != null && !(chunk instanceof EmptyChunk) ? "yes" : "no"
        );
        return 0;
    }
    
    private static int setMaxPortalLayer(int m) {
        Global.maxPortalLayer = m;
        return 0;
    }
    
    private static int listPortals(CommandContext<CommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity playerServer = context.getSource().asPlayer();
        ClientPlayerEntity playerClient = Minecraft.getInstance().player;
        
        StringBuilder result = new StringBuilder();
        
        result.append("Server Portals\n");
        
        playerServer.getServer().getWorlds().forEach(world -> {
            result.append(world.func_234923_W_().func_240901_a_().toString() + "\n");
            for (Entity e : world.func_241136_z_()) {
                if (e instanceof Portal) {
                    result.append(e.toString());
                    result.append("\n");
                }
            }
        });
        
        result.append("Client Portals\n");
        ClientWorldLoader.getClientWorlds().forEach((world) -> {
            result.append(world.func_234923_W_().func_240901_a_().toString() + "\n");
            for (Entity e : world.getAllEntities()) {
                if (e instanceof Portal) {
                    result.append(e.toString());
                    result.append("\n");
                }
            }
        });
        
        McHelper.serverLog(playerServer, result.toString());
        
        return 0;
    }
    
    private static int reportPlayerStatus(CommandContext<CommandSource> context) throws CommandSyntaxException {
        //only invoked on single player
        
        ServerPlayerEntity playerMP = context.getSource().asPlayer();
        ClientPlayerEntity playerSP = Minecraft.getInstance().player;
        
        McHelper.serverLog(
            playerMP,
            String.format(
                "On Server %s %s removed:%s added:%s age:%s chunk:%s %s",
                playerMP.world.func_234923_W_().func_240901_a_(),
                playerMP.getPositionVec(),
                playerMP.removed,
                playerMP.world.getEntityByID(playerMP.getEntityId()) != null,
                playerMP.ticksExisted,
                playerMP.chunkCoordX, playerMP.chunkCoordZ
            )
        );
        
        McHelper.serverLog(
            playerMP,
            String.format(
                "On Client %s %s removed:%s added:%s age:%s chunk:%s %s",
                playerSP.world.func_234923_W_().func_240901_a_(),
                playerSP.getPositionVec(),
                playerSP.removed,
                playerSP.world.getEntityByID(playerSP.getEntityId()) != null,
                playerSP.ticksExisted,
                playerSP.chunkCoordX, playerSP.chunkCoordZ
            )
        );
        return 0;
    }
    
    public static class TestRemoteCallable {
        public static void serverToClient(
            String str, int integer, double doubleNum, ResourceLocation identifier,
            RegistryKey<World> dimension, RegistryKey<Biome> biomeKey,
            BlockPos blockPos, Vector3d vec3d
        ) {
            Helper.log(str + integer + doubleNum + identifier + dimension + biomeKey + blockPos + vec3d);
        }
        
        public static void clientToServer(
            ServerPlayerEntity player,
            UUID uuid,
            Block block, BlockState blockState,
            Item item, ItemStack itemStack,
            CompoundNBT compoundTag, ITextComponent text, int[] intArray
        ) {
            Helper.log(
                player.getName().getUnformattedComponentText() + uuid + block + blockState + item + itemStack
                    + compoundTag + text + Arrays.toString(intArray)
            );
        }
    }
    
    private static void testRemoteProcedureCall(ServerPlayerEntity player) {
        Minecraft.getInstance().execute(() -> {
            CompoundNBT compoundTag = new CompoundNBT();
            compoundTag.put("test", IntNBT.valueOf(7));
            McRemoteProcedureCall.tellServerToInvoke(
                "com.qouteall.immersive_portals.commands.ClientDebugCommand.TestRemoteCallable.clientToServer",
                new UUID(3, 3),
                Blocks.ACACIA_PLANKS,
                Blocks.NETHER_PORTAL.getDefaultState().with(NetherPortalBlock.AXIS, Direction.Axis.Z),
                Items.COMPASS,
                new ItemStack(Items.ACACIA_LOG, 2),
                compoundTag,
                new StringTextComponent("test"),
                new int[]{777, 765}
            );
        });
        
        McHelper.getServer().execute(() -> {
            McRemoteProcedureCall.tellClientToInvoke(
                player,
                "com.qouteall.immersive_portals.commands.ClientDebugCommand.TestRemoteCallable.serverToClient",
                "string", 2, 3.5, new ResourceLocation("imm_ptl:oops"),
                World.field_234919_h_, Biomes.JUNGLE,
                new BlockPos(3, 5, 4),
                new Vector3d(7, 4, 1)
            );
        });
    }
}
