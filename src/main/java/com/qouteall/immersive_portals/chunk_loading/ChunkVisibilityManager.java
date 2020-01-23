package com.qouteall.immersive_portals.chunk_loading;

import com.google.common.collect.Streams;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.portal.Portal;
import com.qouteall.immersive_portals.portal.global_portals.GlobalPortalStorage;
import com.qouteall.immersive_portals.portal.global_portals.GlobalTrackedPortal;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.dimension.DimensionType;

import java.util.Objects;
import java.util.stream.Stream;

public class ChunkVisibilityManager {
    public static final int portalLoadingRange = 48;
    public static final int secondaryPortalLoadingRange = 16;
    
    public static interface ChunkPosConsumer {
        void consume(DimensionType dimensionType, int x, int z, int distanceToSource);
    }
    
    //the players and portals are chunk loaders
    public static class ChunkLoader {
        public DimensionalChunkPos center;
        public int radius;
        
        public ChunkLoader(DimensionalChunkPos center, int radius) {
            this.center = center;
            this.radius = radius;
        }
        
        public void foreachChunkPos(ChunkPosConsumer func) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    func.consume(
                        center.dimension,
                        center.x + dx,
                        center.z + dz,
                        Math.max(Math.abs(dx), Math.abs(dz))
                    );
                }
            }
        }
        
        @Override
        public String toString() {
            return "{" +
                "center=" + center +
                ", radius=" + radius +
                '}';
        }
    
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ChunkLoader that = (ChunkLoader) o;
            return radius == that.radius &&
                center.equals(that.center);
        }
    
        @Override
        public int hashCode() {
            return Objects.hash(center, radius);
        }
    }
    
    private static ChunkLoader playerDirectLoader(ServerPlayerEntity player) {
        return new ChunkLoader(
            new DimensionalChunkPos(
                player.dimension,
                player.chunkCoordX, player.chunkCoordZ
            ),
            getRenderDistanceOnServer()
        );
    }
    
    private static ChunkLoader portalDirectLoader(Portal portal) {
        int renderDistance = getRenderDistanceOnServer();
        return new ChunkLoader(
            new DimensionalChunkPos(
                portal.dimensionTo,
                new ChunkPos(new BlockPos(portal.destination))
            ),
            portal.loadFewerChunks ? (renderDistance / 3) : renderDistance
        );
    }
    
    private static ChunkLoader portalIndirectLoader(Portal portal) {
        int renderDistance = getRenderDistanceOnServer();
        return new ChunkLoader(
            new DimensionalChunkPos(
                portal.dimensionTo,
                new ChunkPos(new BlockPos(portal.destination))
            ),
            (renderDistance / 3)
        );
    }
    
    private static ChunkLoader globalPortalDirectLoader(
        ServerPlayerEntity player,
        GlobalTrackedPortal portal
    ) {
        int renderDistance = getRenderDistanceOnServer();
        return new ChunkLoader(
            new DimensionalChunkPos(
                portal.dimensionTo,
                new ChunkPos(new BlockPos(
                    portal.applyTransformationToPoint(player.getPositionVec())
                ))
            ),
            renderDistance
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
                remotePortal.dimensionTo,
                new ChunkPos(new BlockPos(
                    remotePortal.applyTransformationToPoint(
                        outerPortal.applyTransformationToPoint(player.getPositionVec())
                    )
                ))
            ),
            renderDistance
        );
    }
    
    private static Stream<GlobalTrackedPortal> getGlobalPortals(
        DimensionType dimension
    ) {
        return GlobalPortalStorage.get(
            McHelper.getServer().getWorld(dimension)
        ).data.stream();
    }
    
    //includes:
    //1.player direct loader
    //2.portal direct loader
    //3.portal secondary loader
    //4.global portal direct loader
    //5.global portal secondary loader
    public static Stream<ChunkLoader> getChunkLoaders(
        ServerPlayerEntity player
    ) {
        return Streams.concat(
            Stream.of(playerDirectLoader(player)),
            
            McHelper.getEntitiesNearby(
                player,
                Portal.class,
                portalLoadingRange
            ).filter(
                portal -> portal.canBeSeenByPlayer(player)
            ).flatMap(
                portal -> Streams.concat(
                    Stream.of(portalDirectLoader(portal)),
                    
                    McHelper.getEntitiesNearby(
                        McHelper.getServer().getWorld(portal.dimensionTo),
                        portal.destination,
                        Portal.class,
                        secondaryPortalLoadingRange
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
                    portal -> Streams.concat(
                        Stream.of(globalPortalDirectLoader(
                            player, portal
                        )),
                        
                        getGlobalPortals(
                            portal.dimensionTo
                        ).filter(
                            remotePortal ->
                                remotePortal.getDistanceToNearestPointInPortal(
                                    portal.applyTransformationToPoint(player.getPositionVec())
                                ) < 64
                        ).map(
                            remotePortal -> globalPortalIndirectLoader(
                                player, portal, remotePortal
                            )
                        )
                    )
                )
        ).distinct();
    }
    
    public static int getRenderDistanceOnServer() {
        return McHelper.getIEStorage(DimensionType.OVERWORLD).getWatchDistance();
    }
}
