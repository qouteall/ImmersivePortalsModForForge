package com.qouteall.immersive_portals.portal.nether_portal;

import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.McHelper;
import com.qouteall.immersive_portals.ModMain;
import com.qouteall.immersive_portals.my_util.IntegerAABBInclusive;
import com.qouteall.immersive_portals.portal.LoadingIndicatorEntity;
import com.qouteall.immersive_portals.portal.Portal;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.item.Items;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.server.ServerWorld;

import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

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
    
    //return null for not found
    //executed on main server thread
    public static boolean onFireLit(
        ServerWorld fromWorld,
        BlockPos firePos
    ) {
        DimensionType fromDimension = fromWorld.dimension.getType();
    
        DimensionType toDimension = NetherPortalGenerator.getDestinationDimension(fromDimension);
    
        if (toDimension == null) return false;
    
        ServerWorld toWorld = McHelper.getServer().getWorld(toDimension);
    
        NetherPortalShape thisSideShape = startGeneratingPortal(
            fromWorld,
            firePos,
            toWorld,
            NetherPortalMatcher.findingRadius,
            NetherPortalMatcher.findingRadius,
            (fromPos1) -> NetherPortalGenerator.mapPosition(
                fromPos1,
                fromWorld.dimension.getType(),
                toWorld.dimension.getType()
            ),
            //this side area
            blockPos -> NetherPortalMatcher.isAirOrFire(fromWorld, blockPos),
            //this side frame
            blockPos -> NetherPortalMatcher.isObsidian(fromWorld, blockPos),
            //other side area
            toWorld::isAirBlock,
            //other side frame
            blockPos -> NetherPortalMatcher.isObsidian(toWorld, blockPos),
            (shape) -> embodyNewFrame(toWorld, shape, Blocks.OBSIDIAN.getDefaultState()),
            NewNetherPortalGenerator::generatePortalEntities,
            new TranslationTextComponent(Items.OBSIDIAN.getTranslationKey())
        );
        return thisSideShape != null;
    }
    
    public static boolean activatePortalHelper(
        ServerWorld fromWorld,
        BlockPos firePos
    ) {
        Helper.SimpleBox<NetherPortalShape> thisSideShape = new Helper.SimpleBox<>(null);
        thisSideShape.obj = startGeneratingPortal(
            fromWorld,
            firePos,
            fromWorld,
            NetherPortalMatcher.findingRadius,
            NetherPortalMatcher.findingRadius,
            
            //this side area
            (fromPos1) -> NetherPortalGenerator.getRandomShift().add(fromPos1),
            //this side frame
            blockPos -> NetherPortalMatcher.isAirOrFire(fromWorld, blockPos),
            //that side area
            blockPos -> fromWorld.getBlockState(blockPos).getBlock() == ModMain.portalHelperBlock,
            //that side frame
            fromWorld::isAirBlock,
            blockPos -> fromWorld.getBlockState(blockPos).getBlock() == ModMain.portalHelperBlock,
            (toShape) -> {
                embodyNewFrame(fromWorld, toShape, ModMain.portalHelperBlock.getDefaultState());
            },
            info -> {
                generateHelperPortalEntities(info);
                info.fromShape.frameAreaWithCorner.forEach(blockPos -> {
                    if (fromWorld.getBlockState(blockPos).getBlock() == ModMain.portalHelperBlock) {
                        fromWorld.setBlockState(blockPos, Blocks.AIR.getDefaultState());
                    }
                });
            },
            new TranslationTextComponent(ModMain.portalHelperBlock.getTranslationKey())
        );
        return thisSideShape.obj != null;
    }
    
    //return this side shape if the generation starts
    public static NetherPortalShape startGeneratingPortal(
        ServerWorld fromWorld,
        BlockPos startingPos,
        ServerWorld toWorld,
        int existingFrameSearchingRadius,
        int airCubeSearchingRadius,
        Function<BlockPos, BlockPos> positionMapping,
        Predicate<BlockPos> thisSideAreaPredicate,
        Predicate<BlockPos> thisSideFramePredicate,
        Predicate<BlockPos> otherSideAreaPredicate,
        Predicate<BlockPos> otherSideFramePredicate,
        Consumer<NetherPortalShape> newFrameGeneratedFunc,
        Consumer<Info> portalEntityGeneratingFunc,
        ITextComponent frameBlockText
    ) {
        DimensionType fromDimension = fromWorld.dimension.getType();
        DimensionType toDimension = toWorld.dimension.getType();
        
        NetherPortalShape foundShape = Arrays.stream(Direction.Axis.values())
            .map(
                axis -> {
                    return NetherPortalShape.findArea(
                        startingPos,
                        axis,
                        thisSideAreaPredicate,
                        thisSideFramePredicate
                    );
                }
            ).filter(
                Objects::nonNull
            ).findFirst().orElse(null);
        
        if (foundShape == null) {
            return null;
        }
        
        BlockPos fromPos = foundShape.innerAreaBox.getCenter();
        
        Vec3d indicatorPos = foundShape.innerAreaBox.getCenterVec();
        
        boolean isOtherGenerationRunning = McHelper.getEntitiesNearby(
            fromWorld, indicatorPos, LoadingIndicatorEntity.class, 1
        ).findAny().isPresent();
        if (isOtherGenerationRunning) {
            Helper.log(
                "Aborted Nether Portal Generation Because Another Generation is Running Nearby"
            );
            return null;
        }
        
        BlockPos toPos = positionMapping.apply(fromPos);
        
        //avoid blockpos object creation
        BlockPos.Mutable temp = new BlockPos.Mutable();
        
        IntegerAABBInclusive toWorldHeightLimit =
            NetherPortalMatcher.getHeightLimit(toWorld.dimension.getType());
        
        Iterator<NetherPortalShape> iterator =
            NetherPortalMatcher.fromNearToFarWithinHeightLimit(
                toPos,
                existingFrameSearchingRadius,
                toWorldHeightLimit
            ).map(
                blockPos -> {
                    if (!otherSideAreaPredicate.test(blockPos)) {
                        return null;
                    }
                    
                    return foundShape.matchShape(
                        otherSideAreaPredicate,
                        otherSideFramePredicate,
                        blockPos,
                        temp
                    );
                }
            ).iterator();
        
        LoadingIndicatorEntity indicatorEntity =
            LoadingIndicatorEntity.entityType.create(fromWorld);
        indicatorEntity.isAlive = true;
        indicatorEntity.setPosition(
            indicatorPos.x, indicatorPos.y, indicatorPos.z
        );
        fromWorld.addEntity(indicatorEntity);
        
        McHelper.performSplitedFindingTaskOnServer(
            iterator,
            Objects::nonNull,
            (i) -> {
                boolean isIntact = foundShape.isPortalIntact(
                    thisSideAreaPredicate,
                    thisSideFramePredicate
                );
                
                if (!isIntact) {
                    Helper.log("Nether Portal Generation Aborted");
                    indicatorEntity.remove();
                    return false;
                }
                
                double progress = i / 20000000.0;
                
                indicatorEntity.setText(
                    new TranslationTextComponent(
                        "imm_ptl.searching_for_frame",
                        toWorld.dimension.getType().toString(),
                        String.format("%s %s %s", fromPos.getX(), fromPos.getY(), fromPos.getZ()),
                        frameBlockText,
                        new StringTextComponent((int) (progress * 100) + "%")
                    )
                );
                
                return true;
            },
            toShape -> {
                Info info = new Info(
                    fromDimension, toDimension, foundShape, toShape
                );
                
                portalEntityGeneratingFunc.accept(info);
                indicatorEntity.remove();
            },
            () -> {
                indicatorEntity.setText(new TranslationTextComponent(
                    "imm_ptl.generating_new_frame"
                ));
                
                ModMain.serverTaskList.addTask(() -> {
                    
                    IntegerAABBInclusive airCubePlacement =
                        NetherPortalGenerator.findAirCubePlacement(
                            toWorld, toPos, toWorldHeightLimit,
                            foundShape.axis, foundShape.totalAreaBox.getSize(),
                            airCubeSearchingRadius
                        );
                    
                    NetherPortalShape toShape = foundShape.getShapeWithMovedAnchor(
                        airCubePlacement.l.subtract(
                            foundShape.totalAreaBox.l
                        ).add(foundShape.anchor)
                    );
                    
                    newFrameGeneratedFunc.accept(toShape);
                    
                    Info info = new Info(
                        fromDimension, toDimension, foundShape, toShape
                    );
                    portalEntityGeneratingFunc.accept(info);
                    indicatorEntity.remove();
                    
                    return true;
                });
            }
        );
        
        return foundShape;
    }
    
    private static void embodyNewFrame(
        ServerWorld toWorld,
        NetherPortalShape toShape, BlockState frameBlockState
    ) {
        toShape.frameAreaWithCorner.forEach(blockPos ->
            toWorld.setBlockState(blockPos, frameBlockState)
        );
    }
    
    private static void fillInPlaceHolderBlocks(
        ServerWorld fromWorld,
        NetherPortalShape netherPortalShape
    ) {
        netherPortalShape.area.forEach(
            blockPos -> NetherPortalGenerator.setPortalContentBlock(
                fromWorld, blockPos, netherPortalShape.axis
            )
        );
    }
    
    //create portal entity and generate obsidian blocks and placeholder blocks
    //the portal blocks will be placed on both sides because the obsidian may break while generating
    //executed on server main thread
    private static void generatePortalEntities(
        Info info
    ) {
        ServerWorld fromWorld = McHelper.getServer().getWorld(info.from);
        ServerWorld toWorld = McHelper.getServer().getWorld(info.to);
        
        fillInPlaceHolderBlocks(fromWorld, info.fromShape);
        fillInPlaceHolderBlocks(toWorld, info.toShape);
        
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
        
        portalArray[0].reversePortalId = portalArray[2].getUniqueID();
        portalArray[1].reversePortalId = portalArray[3].getUniqueID();
        portalArray[2].reversePortalId = portalArray[0].getUniqueID();
        portalArray[3].reversePortalId = portalArray[1].getUniqueID();
        
        fromWorld.addEntity(portalArray[0]);
        fromWorld.addEntity(portalArray[1]);
        toWorld.addEntity(portalArray[2]);
        toWorld.addEntity(portalArray[3]);
    }
    
    private static void generateHelperPortalEntities(Info info) {
        ServerWorld fromWorld1 = McHelper.getServer().getWorld(info.from);
        ServerWorld toWorld = McHelper.getServer().getWorld(info.to);
        
        Portal[] portalArray = new Portal[]{
            Portal.entityType.create(fromWorld1),
            Portal.entityType.create(fromWorld1),
            Portal.entityType.create(toWorld),
            Portal.entityType.create(toWorld)
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
        
        fromWorld1.addEntity(portalArray[0]);
        fromWorld1.addEntity(portalArray[1]);
        toWorld.addEntity(portalArray[2]);
        toWorld.addEntity(portalArray[3]);
    }
}
