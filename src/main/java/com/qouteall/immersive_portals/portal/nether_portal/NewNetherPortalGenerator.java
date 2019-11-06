package com.qouteall.immersive_portals.portal.nether_portal;

import com.qouteall.immersive_portals.my_util.Helper;
import com.qouteall.immersive_portals.my_util.IntegerAABBInclusive;
import com.qouteall.immersive_portals.portal.PortalPlaceholderBlock;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

public class NewNetherPortalGenerator {
    public static class Info {
        DimensionType from;
        DimensionType to;
        NetherPortalShape fromShape;
        NetherPortalShape toShape;
        
        public Info(
            DimensionType from,
            DimensionType to,
            NetherPortalShape fromShape,
            NetherPortalShape toShape
        ) {
            this.from = from;
            this.to = to;
            this.fromShape = fromShape;
            this.toShape = toShape;
        }
    }
    
    //only one nether portal should be generating
    //public static final Object lock = new Object();
    
    //return null for not found
    //executed on main server thread
    public static boolean onFireLit(
        ServerWorld fromWorld,
        BlockPos firePos
    ) {
        DimensionType fromDimension = fromWorld.dimension.getType();
        
        DimensionType toDimension = NetherPortalGenerator.getDestinationDimension(fromDimension);
        
        if (toDimension == null) return false;
        
        NetherPortalShape foundShape = Arrays.stream(Direction.Axis.values())
            .map(
                axis -> NetherPortalShape.findArea(
                    firePos,
                    axis,
                    blockPos -> NetherPortalMatcher.isAirOrFire(
                        fromWorld, blockPos
                    ),
                    blockPos -> NetherPortalMatcher.isObsidian(
                        fromWorld, blockPos
                    )
                )
            ).filter(
                Objects::nonNull
            ).findFirst().orElse(null);
        
        if (foundShape == null) {
            return false;
        }
        
        //avoid lighting portal again when generating portal
        //fillInPlaceHolderBlock(fromWorld, foundShape);
        
        //TODO spawn loading indicator
        
        NetherPortalShape fromShape = foundShape;
        ServerWorld toWorld = Helper.getServer().getWorld(toDimension);
        
        BlockPos fromPos = fromShape.innerAreaBox.getCenter();
        
        BlockPos toPos = NetherPortalGenerator.getPosInOtherDimension(
            fromPos,
            fromWorld.dimension.getType(),
            toWorld.dimension.getType()
        );
        
        //avoid blockpos object creation
        BlockPos.MutableBlockPos temp = new BlockPos.MutableBlockPos();
        
        IntegerAABBInclusive toWorldHeightLimit =
            NetherPortalMatcher.getHeightLimit(toWorld.dimension.getType());
        
        Iterator<NetherPortalShape> iterator =
            NetherPortalMatcher.fromNearToFarWithinHeightLimitForMutable(
                toPos,
                150,
                toWorldHeightLimit
            ).map(
                blockPos -> {
                    if (!toWorld.isAirBlock(blockPos)) {
                        return null;
                    }
                    return fromShape.matchShape(
                        toWorld::isAirBlock,
                        p -> NetherPortalMatcher.isObsidian(toWorld, p),
                        blockPos,
                        temp
                    );
                }
            ).iterator();
        
        Helper.performSplitedFindingTaskOnServer(
            iterator,
            Objects::nonNull,
            50000,
            (i) -> {
                Helper.log("Progress " + i);
                return true;
            },
            toShape -> {
                finishGeneratingPortal(new Info(
                    fromDimension, toDimension, fromShape, toShape
                ));
            },
            () -> {
                IntegerAABBInclusive airCubePlacement = NetherPortalGenerator.findAirCubePlacement(
                    toWorld, toPos, toWorldHeightLimit,
                    fromShape.totalAreaBox.getSize(), fromShape.axis
                );
                
                NetherPortalShape toShape = fromShape.getShapeWithMovedAnchor(
                    airCubePlacement.l.subtract(
                        fromShape.totalAreaBox.l
                    ).add(fromShape.anchor)
                );
                
                finishGeneratingPortal(new Info(
                    fromDimension, toDimension, fromShape, toShape
                ));
            }
        );
        
        return true;
    }
    
    private static void fillInPlaceHolderBlock(
        ServerWorld fromWorld,
        NetherPortalShape netherPortalShape
    ) {
        assert false;
    }
    
    private static boolean recheckTheFrameThatIsBeingLighted(
        ServerWorld fromWorld,
        NetherPortalShape foundShape
    ) {
        return foundShape.isPortalIntact(
            blockPos -> {
                Block block = fromWorld.getBlockState(blockPos).getBlock();
                return block == Blocks.AIR ||
                    block == Blocks.FIRE ||
                    block == PortalPlaceholderBlock.instance;
            },
            blockPos -> NetherPortalMatcher.isObsidian(fromWorld, blockPos)
        );
    }
    
    //executed on server worker thread
    //return null for failed
    private static NetherPortalShape getPortalPlacementAsync(
        ServerWorld fromWorld,
        ServerWorld toWorld,
        NetherPortalShape fromShape
    ) {
        if (!recheckTheFrameThatIsBeingLighted(fromWorld, fromShape)) {
            Helper.log(
                "Nether Portal Generation Aborted." +
                    "This Could Be Caused By Breaking The Portal After Generation Started"
            );
            return null;
        }
        
        BlockPos fromPos = fromShape.innerAreaBox.getCenter();
        
        BlockPos toPos = NetherPortalGenerator.getPosInOtherDimension(
            fromPos,
            fromWorld.dimension.getType(),
            toWorld.dimension.getType()
        );
        
        //avoid blockpos object creation
        BlockPos.MutableBlockPos temp = new BlockPos.MutableBlockPos();
        
        IntegerAABBInclusive toWorldHeightLimit =
            NetherPortalMatcher.getHeightLimit(toWorld.dimension.getType());
        
        NetherPortalShape toFrame = NetherPortalMatcher.fromNearToFarWithinHeightLimitForMutable(
            toPos,
            150,
            toWorldHeightLimit
        ).filter(
            toWorld::isAirBlock
        ).map(
            blockPos -> fromShape.matchShape(
                toWorld::isAirBlock,
                p -> NetherPortalMatcher.isObsidian(toWorld, p),
                blockPos,
                temp
            )
        ).filter(
            Objects::nonNull
        ).findFirst().orElse(null);
        
        if (toFrame != null) {
            return toFrame;
        }
        
        IntegerAABBInclusive airCubePlacement = NetherPortalGenerator.findAirCubePlacement(
            toWorld,
            toPos,
            toWorldHeightLimit,
            fromShape.totalAreaBox.getSize(),
            fromShape.axis
        );
        
        NetherPortalShape result = fromShape.getShapeWithMovedAnchor(
            airCubePlacement.l.subtract(
                fromShape.totalAreaBox.l
            ).add(fromShape.anchor)
        );
        return result;
        
    }
    
    private static void generatePortalWithNewFrame(
        ServerWorld fromWorld,
        ServerWorld toWorld,
        NetherPortalShape fromShape
    ) {
    
    }
    
    //create portal entity and generate obsidian blocks and placeholder blocks
    //the portal blocks will be placed on both sides because the obsidian may break while generating
    //executed on server main thread
    private static void finishGeneratingPortal(
        Info info
    ) {
        ServerWorld fromWorld = Helper.getServer().getWorld(info.from);
        ServerWorld toWorld = Helper.getServer().getWorld(info.to);
        
        NewNetherPortalEntity[] portalArray = new NewNetherPortalEntity[]{
            NewNetherPortalEntity.entityType.create(fromWorld),
            NewNetherPortalEntity.entityType.create(fromWorld),
            NewNetherPortalEntity.entityType.create(toWorld),
            NewNetherPortalEntity.entityType.create(toWorld)
        };
        
        info.fromShape.initPortalPosAxisShape(
            portalArray[0], false
        );
        info.fromShape.initPortalPosAxisShape(
            portalArray[1], true
        );
        info.toShape.initPortalPosAxisShape(
            portalArray[2], false
        );
        info.toShape.initPortalPosAxisShape(
            portalArray[3], true
        );
        
        portalArray[0].dimensionTo = info.to;
        portalArray[1].dimensionTo = info.to;
        portalArray[2].dimensionTo = info.from;
        portalArray[3].dimensionTo = info.from;
        
        Vec3d offset = new Vec3d(info.toShape.innerAreaBox.l.subtract(
            info.fromShape.innerAreaBox.l
        ));
        portalArray[0].destination = portalArray[0].getPositionVec().add(offset);
        portalArray[1].destination = portalArray[1].getPositionVec().add(offset);
        portalArray[2].destination = portalArray[2].getPositionVec().subtract(offset);
        portalArray[3].destination = portalArray[3].getPositionVec().subtract(offset);
        
        portalArray[0].netherPortalShape = info.fromShape;
        portalArray[1].netherPortalShape = info.fromShape;
        portalArray[2].netherPortalShape = info.toShape;
        portalArray[3].netherPortalShape = info.toShape;
        
        fromWorld.addEntity(portalArray[0]);
        fromWorld.addEntity(portalArray[1]);
        toWorld.addEntity(portalArray[2]);
        toWorld.addEntity(portalArray[3]);
    }
}
