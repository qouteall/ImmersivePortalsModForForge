package com.qouteall.immersive_portals.chunk_loading;

import com.google.common.collect.Streams;
import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.global_portals.GlobalPortalStorage;
import com.qouteall.immersive_portals.portal.global_portals.GlobalTrackedPortal;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class ChunkVisibilityManager {
    //the players and portals are chunk loaders
    public static class ChunkLoader {
        public DimensionalChunkPos center;
        public int radius;
        public Entity loader;
        
        public ChunkLoader(DimensionalChunkPos center, int radius, Entity loader) {
            this.center = center;
            this.radius = radius;
            this.loader = loader;
        }
        
        public void foreachChunkPos(Consumer<DimensionalChunkPos> func) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    func.accept(new DimensionalChunkPos(
                        center.dimension,
                        center.x + dx,
                        center.z + dz
                    ));
                }
            }
        }
        
        @Override
        public String toString() {
            return "ChunkLoader{" +
                "center=" + center +
                ", radius=" + radius +
                ", loader=" + loader +
                '}';
        }
    }
    
    private static ChunkLoader playerDirectLoader(ServerPlayerEntity player) {
        return new ChunkLoader(
            new DimensionalChunkPos(
                player.dimension,
                player.chunkCoordX, player.chunkCoordZ
            ),
            getRenderDistanceOnServer(),
            player
        );
    }
    
    private static ChunkLoader portalDirectLoader(Portal portal) {
        int renderDistance = getRenderDistanceOnServer();
        return new ChunkLoader(
            new DimensionalChunkPos(
                portal.dimensionTo,
                new ChunkPos(new BlockPos(portal.destination))
            ),
            portal.loadFewerChunks ? (renderDistance / 3) : renderDistance,
            portal
        );
    }
    
    private static ChunkLoader portalIndirectLoader(Portal portal) {
        int renderDistance = getRenderDistanceOnServer();
        return new ChunkLoader(
            new DimensionalChunkPos(
                portal.dimensionTo,
                new ChunkPos(new BlockPos(portal.destination))
            ),
            (renderDistance / 3),
            portal
        );
    }
    
    private static ChunkLoader globalPortalDirectLoader(
        ServerPlayerEntity player,
        GlobalTrackedPortal portal
    ) {
        int renderDistance = getRenderDistanceOnServer();
        return new ChunkLoader(
            new DimensionalChunkPos(
                portal.dimension,
                new ChunkPos(new BlockPos(
                    portal.applyTransformationToPoint(player.getPositionVec())
                ))
            ),
            renderDistance,
            portal
        );
    }
    
    private static ChunkLoader globalPortalIndirectLoader(
        ServerPlayerEntity player,
        GlobalTrackedPortal outerPortal,
        GlobalTrackedPortal remotePortal
    ) {
        int renderDistance = getRenderDistanceOnServer();
        return new ChunkLoader(
            new DimensionalChunkPos(
                remotePortal.dimension,
                new ChunkPos(new BlockPos(
                    remotePortal.applyTransformationToPoint(
                        outerPortal.applyTransformationToPoint(player.getPositionVec())
                    )
                ))
            ),
            renderDistance,
            remotePortal
        );
    }
    
    private static Stream<GlobalTrackedPortal> getGlobalPortals(
        DimensionType dimension
    ) {
        return GlobalPortalStorage.get(
            Helper.getServer().getWorld(dimension)
        ).data.stream();
    }
    
    public static Stream<ChunkLoader> getChunkLoaders(
        ServerPlayerEntity player
    ) {
        return Streams.concat(
            Stream.of(playerDirectLoader(player)),
            
            Helper.getEntitiesNearby(
                player,
                Portal.class,
                ChunkTrackingGraph.portalLoadingRange
            ).filter(
                portal -> portal.canBeSeenByPlayer(player)
            ).flatMap(
                portal -> Streams.concat(
                    Stream.of(portalDirectLoader(portal)),
                    
                    Helper.getEntitiesNearby(
                        Helper.getServer().getWorld(portal.dimensionTo),
                        portal.destination,
                        Portal.class,
                        ChunkTrackingGraph.secondaryPortalLoadingRange
                    ).filter(
                        remotePortal -> remotePortal.canBeSeenByPlayer(player)
                    ).map(
                        remotePortal -> portalIndirectLoader(remotePortal)
                    )
                )
            ),
            
            getGlobalPortals(player.dimension)
                .filter(portal ->
                    portal.getDistanceToNearestPointInPortal(player.getPositionVec()) < 128
                )
                .flatMap(
                    portal -> {
                        Vec3d secondaryPos =
                            portal.applyTransformationToPoint(player.getPositionVec());
                        return Streams.concat(
                            Stream.of(globalPortalDirectLoader(
                                player, portal
                            )),
                            
                            getGlobalPortals(
                                portal.dimensionTo
                            ).filter(
                                remotePortal ->
                                    remotePortal.getDistanceToNearestPointInPortal(secondaryPos) < 64
                            ).map(
                                remotePortal -> globalPortalIndirectLoader(
                                    player, portal, remotePortal
                                )
                            )
                        );
                    }
                )
        );
    }
    
    public static Set<DimensionalChunkPos> getPlayerViewingChunksNew(
        ServerPlayerEntity player
    ) {
        HashSet<DimensionalChunkPos> chunks = new HashSet<>();
        getChunkLoaders(player)
            .forEach(
                loader -> loader.foreachChunkPos(chunks::add)
            );
        return chunks;
    }
    
    public static int getRenderDistanceOnServer() {
        return Helper.getIEStorage(DimensionType.OVERWORLD).getWatchDistance();
    }
}
