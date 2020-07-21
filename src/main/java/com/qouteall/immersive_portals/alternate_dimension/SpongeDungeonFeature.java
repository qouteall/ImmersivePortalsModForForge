package com.qouteall.immersive_portals.alternate_dimension;

import com.mojang.serialization.Codec;
import com.qouteall.immersive_portals.Helper;
import com.qouteall.immersive_portals.block_manipulation.HandReachTweak;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.monster.SkeletonEntity;
import net.minecraft.entity.monster.SlimeEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.INBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.potion.PotionUtils;
import net.minecraft.potion.Potions;
import net.minecraft.tileentity.MobSpawnerTileEntity;
import net.minecraft.tileentity.ShulkerBoxTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.WeightedSpawnerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.registry.DefaultedRegistry;
import net.minecraft.util.registry.Registry;
import net.minecraft.world.ISeedReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.gen.ChunkGenerator;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.NoFeatureConfig;
import net.minecraft.world.gen.feature.structure.StructureManager;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SpongeDungeonFeature extends Feature<NoFeatureConfig> {
    //no need to register it
    public static final Feature<NoFeatureConfig> instance =
        new SpongeDungeonFeature(NoFeatureConfig.field_236558_a_);
    
    private static RandomSelector<BlockState> spawnerShieldSelector =
        new RandomSelector.Builder<BlockState>()
            .add(20, Blocks.OBSIDIAN.getDefaultState())
            .add(10, Blocks.SPONGE.getDefaultState())
            .add(5, Blocks.CLAY.getDefaultState())
            .build();
    
    private static RandomSelector<Function<Random, Integer>> heightSelector =
        new RandomSelector.Builder<Function<Random, Integer>>()
            .add(20, random -> (int) (random.nextDouble() * 50 + 3))
            .add(10, random -> random.nextInt(150) + 3)
            .build();
    
    private static RandomSelector<BiFunction<World, Random, Entity>> entitySelector =
        new RandomSelector.Builder<BiFunction<World, Random, Entity>>()
            .add(10, SpongeDungeonFeature::randomMonster)
            .add(60, SpongeDungeonFeature::randomRidingMonster)
            .build();
    
    private static RandomSelector<EntityType<?>> monsterTypeSelector =
        new RandomSelector.Builder<EntityType<?>>()
            .add(10, EntityType.ZOMBIFIED_PIGLIN)
            .add(10, EntityType.HUSK)
            .add(30, EntityType.SKELETON)
            .add(10, EntityType.WITHER_SKELETON)
            .add(20, EntityType.SHULKER)
            .add(10, EntityType.SILVERFISH)
            .add(30, EntityType.SLIME)
            .add(20, EntityType.GHAST)
            .add(10, EntityType.SPIDER)
            .add(10, EntityType.CAVE_SPIDER)
            .add(10, EntityType.GIANT)
            .add(10, EntityType.MAGMA_CUBE)
//            .add(10, EntityType.GUARDIAN)
            .add(10, EntityType.ENDERMAN)
            .add(10, EntityType.SNOW_GOLEM)
            .add(10, EntityType.ARMOR_STAND)
            .add(10, EntityType.PILLAGER)
            .add(10, EntityType.PUFFERFISH)
            .add(10, EntityType.RAVAGER)
            .add(1, EntityType.WITHER)
            .build();
    
    private static RandomSelector<EntityType<?>> vehicleTypeSelector =
        new RandomSelector.Builder<EntityType<?>>()
            .add(20, EntityType.BAT)
            .add(20, EntityType.PHANTOM)
            .add(20, EntityType.GHAST)
            .add(10, EntityType.SLIME)
            .add(20, EntityType.SPIDER)
            .add(20, EntityType.PARROT)
            .add(10, EntityType.SHULKER)
            .build();
    
    private static RandomSelector<RandomSelector<EntityType<?>>> typeSelectorSelector =
        new RandomSelector.Builder<RandomSelector<EntityType<?>>>()
            .add(40, monsterTypeSelector)
            .add(10, vehicleTypeSelector)
            .build();
    
    private static final BlockPos[] shieldPoses = new BlockPos[]{
        new BlockPos(0, -2, 0),
        new BlockPos(1, -1, 0),
        new BlockPos(-1, -1, 0),
        new BlockPos(0, -1, 1),
        new BlockPos(0, -1, -1),
        new BlockPos(1, 0, 0),
        new BlockPos(-1, 0, 0),
        new BlockPos(0, 0, 1),
        new BlockPos(0, 0, -1),
        new BlockPos(0, 1, 0),
    };
    
    private static List<Block> shulkerBoxes = Registry.BLOCK.stream()
        .filter(block -> block instanceof ShulkerBoxBlock)
        .collect(Collectors.toList());
    
    public SpongeDungeonFeature(Codec<NoFeatureConfig> codec) {
        super(codec);
    }
    
    public void generateOnce(IWorld world, Random random, ChunkPos chunkPos) {
        int height = heightSelector.select(random).apply(random);
        BlockPos spawnerPos = chunkPos.getBlock(
            random.nextInt(16),
            height,
            random.nextInt(16)
        );
        
        for (int dx = -7; dx <= 7; dx++) {
            for (int dz = -7; dz <= 7; dz++) {
                world.setBlockState(
                    spawnerPos.add(dx, -2, dz),
                    Blocks.SPONGE.getDefaultState(),
                    2
                );
            }
        }
        
        BlockState spawnerShieldBlock = spawnerShieldSelector.select(random);
        for (BlockPos shieldPos : shieldPoses) {
            world.setBlockState(
                spawnerPos.add(shieldPos),
                spawnerShieldBlock,
                2
            );
        }
        
        world.setBlockState(spawnerPos, Blocks.SPAWNER.getDefaultState(), 2);
        initSpawnerBlockEntity(world, random, spawnerPos);
        
        BlockPos shulkerBoxPos = spawnerPos.down();
        initShulkerBoxTreasure(world, random, shulkerBoxPos);
    }
    
    public void initShulkerBoxTreasure(
        IWorld world,
        Random random,
        BlockPos shulkerBoxPos
    ) {
        Block randomShulkerBox = shulkerBoxes.get(random.nextInt(shulkerBoxes.size()));
        world.setBlockState(shulkerBoxPos, randomShulkerBox.getDefaultState(), 2);
        
        TileEntity blockEntity = world.getTileEntity(shulkerBoxPos);
        if (!(blockEntity instanceof ShulkerBoxTileEntity)) {
            //Helper.err("No Spawner Block Entity???");
            return;
        }
        
        treasureSelector.select(random).accept(random, ((ShulkerBoxTileEntity) blockEntity));
    }
    
    public void initSpawnerBlockEntity(IWorld world, Random random, BlockPos spawnerPos) {
        TileEntity blockEntity = world.getTileEntity(spawnerPos);
        if (!(blockEntity instanceof MobSpawnerTileEntity)) {
            //Helper.err("No Spawner Block Entity???");
            return;
        }
        
        MobSpawnerTileEntity mobSpawner = (MobSpawnerTileEntity) blockEntity;
        Entity spawnedEntity = entitySelector.select(random).apply(world.getWorld(), random);
        Validate.isTrue(!spawnedEntity.isPassenger());
        CompoundNBT tag = new CompoundNBT();
        spawnedEntity.writeUnlessPassenger(tag);
        
        removeUnnecessaryTag(tag);
        
        mobSpawner.getSpawnerBaseLogic().setNextSpawnData(
            new WeightedSpawnerEntity(100, tag)
        );
        mobSpawner.getSpawnerBaseLogic().setEntityType(spawnedEntity.getType());
        
        CompoundNBT logicTag = mobSpawner.getSpawnerBaseLogic().write(new CompoundNBT());
        logicTag.putShort("RequiredPlayerRange", (short) 32);
        //logicTag.putShort("MinSpawnDelay",(short) 10);
        //logicTag.putShort("MaxSpawnDelay",(short) 100);
        //logicTag.putShort("MaxNearbyEntities",(short) 200);
        mobSpawner.getSpawnerBaseLogic().read(logicTag);
    }
    
    private static void removeUnnecessaryTag(INBT tag) {
        if (tag instanceof CompoundNBT) {
            ((CompoundNBT) tag).remove("Pos");
            ((CompoundNBT) tag).remove("UUIDMost");
            ((CompoundNBT) tag).remove("UUIDLeast");
            ((CompoundNBT) tag).keySet().forEach(key -> {
                INBT currTag = ((CompoundNBT) tag).get(key);
                removeUnnecessaryTag((currTag));
                
            });
        }
        if (tag instanceof ListNBT) {
            ((ListNBT) tag).stream().forEach(SpongeDungeonFeature::removeUnnecessaryTag);
        }
    }
    
    private static Entity randomMonster(World world, Random random) {
        EntityType<?> entityType = monsterTypeSelector.select(random);
        return initRandomEntity(world, random, entityType);
    }
    
    private static Entity initRandomEntity(World world, Random random, EntityType<?> entityType) {
        Entity entity = entityType.create(world);
        if (entity instanceof SlimeEntity) {
            CompoundNBT tag = entity.writeWithoutTypeId(new CompoundNBT());
            tag.putInt("Size", random.nextInt(20));
            entity.read(tag);
        }
        if (entity instanceof LivingEntity) {
            entityPostprocessorSelector.select(random).accept(random, ((LivingEntity) entity));
        }
        if (entity instanceof SkeletonEntity) {
            ItemStack stack = new ItemStack(() -> Items.BOW);
            entity.setItemStackToSlot(EquipmentSlotType.MAINHAND, stack);
            entity.setItemStackToSlot(EquipmentSlotType.OFFHAND, stack.copy());
            ItemStack pumpkin = new ItemStack(() -> Items.PUMPKIN);
            entity.setItemStackToSlot(EquipmentSlotType.HEAD, pumpkin);
        }
        return entity;
    }
    
    private static Entity randomRidingMonster(World world, Random random) {
        
        int layer = random.nextInt(9) + 1;
        
        Entity vehicle = vehicleTypeSelector.select(random).create(world);
        
        Helper.reduce(
            vehicle,
            IntStream.range(0, layer)
                .mapToObj(i -> initRandomEntity(
                    world, random,
                    typeSelectorSelector.select(random).select(random)
                    )
                ),
            (down, up) -> {
                up.startRiding(down, true);
                return up;
            }
        );
        
        return vehicle;
    }
    
    private static RandomSelector<BiConsumer<Random, LivingEntity>> entityPostprocessorSelector =
        new RandomSelector.Builder<BiConsumer<Random, LivingEntity>>()
            .add(10, (random, entity) -> {
                //nothing
            })
            .add(15, (random, entity) ->
                entity.addPotionEffect(new EffectInstance(
                    Effects.SPEED, 120, 2
                ))
            )
            .add(20, (random, entity) ->
                entity.addPotionEffect(new EffectInstance(
                    Effects.JUMP_BOOST, 120, 3
                ))
            )
            .add(5, (random, entity) -> {
                ItemStack stack = new ItemStack(() -> Items.GOLDEN_CHESTPLATE);
                stack.addEnchantment(Enchantments.PROTECTION, 6);
                entity.setItemStackToSlot(EquipmentSlotType.CHEST, stack);
            })
            .add(5, (random, entity) -> {
                ItemStack stack = new ItemStack(() -> Items.GOLDEN_HELMET);
                stack.addEnchantment(Enchantments.PROTECTION, 6);
                entity.setItemStackToSlot(EquipmentSlotType.HEAD, stack);
            })
            .add(5, (random, entity) -> {
                ItemStack stack = new ItemStack(() -> Items.GOLDEN_LEGGINGS);
                stack.addEnchantment(Enchantments.PROTECTION, 6);
                entity.setItemStackToSlot(EquipmentSlotType.LEGS, stack);
            })
            .add(5, (random, entity) -> {
                ItemStack stack = new ItemStack(() -> Items.GOLDEN_BOOTS);
                stack.addEnchantment(Enchantments.PROTECTION, 6);
                entity.setItemStackToSlot(EquipmentSlotType.FEET, stack);
            })
            .add(5, (random, entity) -> {
                ItemStack stack = new ItemStack(() -> Items.GOLDEN_PICKAXE);
                stack.addEnchantment(Enchantments.EFFICIENCY, 6);
                entity.setItemStackToSlot(EquipmentSlotType.MAINHAND, stack);
            })
            .add(5, (random, entity) -> {
                ItemStack stack = new ItemStack(() -> Items.GOLDEN_AXE);
                stack.addEnchantment(Enchantments.EFFICIENCY, 6);
                entity.setItemStackToSlot(EquipmentSlotType.MAINHAND, stack);
            })
            .add(5, (random, entity) -> {
                ItemStack stack = new ItemStack(() -> Items.GOLDEN_SWORD);
                stack.addEnchantment(Enchantments.KNOCKBACK, 6);
                entity.setItemStackToSlot(EquipmentSlotType.MAINHAND, stack);
            })
            .build();
    
    private static RandomSelector<ItemStack> filledTreasureSelector =
        new RandomSelector.Builder<ItemStack>()
            .add(60, new ItemStack(() -> Items.DIRT, 64))
            .add(60, new ItemStack(() -> Items.SAND, 64))
            .add(60, new ItemStack(() -> Items.TERRACOTTA, 64))
            .add(60, new ItemStack(() -> Items.GRAVEL, 64))
            .add(40, new ItemStack(() -> Items.PACKED_ICE, 64))
            .add(60, new ItemStack(() -> Items.GLASS, 64))
            .add(60, new ItemStack(() -> Items.COBBLESTONE, 64))
            .add(10, new ItemStack(() -> Items.FEATHER, 64))
            .add(10, new ItemStack(() -> Items.INK_SAC, 64))
            .add(10, new ItemStack(() -> Items.LADDER, 64))
            .add(10, new ItemStack(() -> Items.POISONOUS_POTATO, 64))
            .add(10, new ItemStack(() -> Items.BIRCH_BUTTON, 64))
            .add(10, new ItemStack(() -> Items.LOOM, 64))
            .add(10, new ItemStack(() -> Items.DAYLIGHT_DETECTOR, 64))
            .add(10, new ItemStack(() -> Items.ACTIVATOR_RAIL, 64))
            .add(10, new ItemStack(() -> Items.SEA_PICKLE, 64))
            .add(10, new ItemStack(() -> Items.GRASS_PATH, 64))
            .add(10, new ItemStack(() -> Items.FLOWER_POT, 64))
            .add(10, new ItemStack(() -> Items.FLETCHING_TABLE, 64))
            .add(10, new ItemStack(() -> Items.BRICK_STAIRS, 64))
            .add(10, new ItemStack(() -> Items.COBWEB, 64))
            .add(10, new ItemStack(() -> Items.GRASS, 64))
            .add(10, new ItemStack(() -> Items.BELL, 64))
            .add(10, new ItemStack(() -> Items.JUNGLE_FENCE_GATE, 64))
            .add(10, new ItemStack(() -> Items.LILY_PAD, 64))
            .add(10, new ItemStack(() -> Items.JUKEBOX, 64))
            .add(10, new ItemStack(() -> Items.LEAD, 64))
            .add(10, new ItemStack(() -> Items.DRIED_KELP, 64))
            .add(10, new ItemStack(() -> Items.SPIDER_EYE, 64))
            .add(10, new ItemStack(() -> Items.LECTERN, 64))
            .add(10, new ItemStack(() -> Items.FARMLAND, 64))
            .add(10, new ItemStack(() -> Items.END_STONE_BRICK_STAIRS, 64))
            .add(10, new ItemStack(() -> Items.STRIPPED_OAK_WOOD, 64))
            .add(10, new ItemStack(() -> Items.ENCHANTING_TABLE, 64))
            .add(10, new ItemStack(() -> Items.ENDER_CHEST, 64))
            .add(10, new ItemStack(() -> Items.FERN, 64))
            .add(10, new ItemStack(() -> Items.DEAD_BUSH, 64))
            .add(10, new ItemStack(() -> Items.SEAGRASS, 64))
            .add(10, new ItemStack(() -> Items.CACTUS, 64))
            .add(10, new ItemStack(() -> Items.VINE, 64))
            .add(10, new ItemStack(() -> Items.OBSERVER, 64))
            .add(10, new ItemStack(() -> Items.COMPARATOR, 64))
            .add(10, new ItemStack(() -> Items.REPEATER, 64))
            .add(10, new ItemStack(() -> Items.DIAMOND_HOE, 1))
            .add(10, new ItemStack(() -> Items.FLINT_AND_STEEL, 1))
            .add(10, new ItemStack(() -> Items.COMPASS, 1))
            .add(10, new ItemStack(() -> Items.ACACIA_BOAT, 1))
            .add(10, new ItemStack(() -> Items.CARROT_ON_A_STICK, 1))
            .add(10, new ItemStack(() -> Items.PUFFERFISH_BUCKET, 1))
            .add(10, new ItemStack(() -> Items.MILK_BUCKET, 1))
            .add(10, new ItemStack(() -> Items.LEATHER_HORSE_ARMOR, 1))
            .add(10, new ItemStack(() -> Items.LEATHER_BOOTS, 1))
            .add(10, new ItemStack(() -> Items.SLIME_BLOCK, 1))
            .add(10, Helper.makeIntoExpression(
                new ItemStack(() -> Items.ENCHANTED_BOOK, 1),
                itemStack -> itemStack.addEnchantment(Enchantments.BINDING_CURSE, 1)
            ))
            .add(10, Helper.makeIntoExpression(
                new ItemStack(() -> Items.WOODEN_PICKAXE, 1),
                itemStack -> itemStack.addEnchantment(Enchantments.UNBREAKING, 5)
            ))
            .add(10, Helper.makeIntoExpression(
                new ItemStack(() -> Items.GOLDEN_AXE, 1),
                itemStack -> itemStack.addEnchantment(Enchantments.EFFICIENCY, 10)
            ))
            .add(10, Helper.makeIntoExpression(
                new ItemStack(() -> Items.GOLDEN_PICKAXE, 1),
                itemStack -> itemStack.addEnchantment(Enchantments.EFFICIENCY, 10)
            ))
            .add(10, Helper.makeIntoExpression(
                new ItemStack(() -> Items.POTION, 1),
                itemStack -> PotionUtils.addPotionToItemStack(itemStack, Potions.MUNDANE)
            ))
            .add(80, Helper.makeIntoExpression(
                new ItemStack(() -> Items.POTION, 1),
                itemStack -> PotionUtils.addPotionToItemStack(itemStack, HandReachTweak.longerReachPotion)
            ))
            .build();
    
    private static RandomSelector<Function<Random, ItemStack>> singleTreasureSelector =
        new RandomSelector.Builder<Function<Random, ItemStack>>()
            .add(2, random -> {
                ItemStack stack = new ItemStack(() -> Items.STICK, 1);
                stack.addEnchantment(Enchantments.KNOCKBACK, 7);
                return stack;
            })
            .add(10, random -> {
                ItemStack stack = new ItemStack(() -> Items.LINGERING_POTION, 1);
                ArrayList<EffectInstance> effects = new ArrayList<>();
                effects.add(new EffectInstance(
                    ((DefaultedRegistry<Effect>) Registry.EFFECTS).getRandom(random),
                    1200, 4
                ));
                PotionUtils.appendEffects(stack, effects);
                return stack;
            })
            .add(10, random -> {
                ItemStack stack = new ItemStack(() -> Items.ENCHANTED_BOOK, 1);
                stack.addEnchantment(((DefaultedRegistry<Enchantment>) Registry.ENCHANTMENT).getRandom(random), 5);
                return stack;
            })
            .build();
    
    private static RandomSelector<BiConsumer<Random, IInventory>> treasureSelector =
        new RandomSelector.Builder<BiConsumer<Random, IInventory>>()
            .add(20, (random, shulker) -> {
                ItemStack toFill = filledTreasureSelector.select(random);
                IntStream.range(
                    0, shulker.getSizeInventory()
                ).forEach(i -> shulker.setInventorySlotContents(i, toFill.copy()));
            })
            .add(5, (random, shulker) -> {
                shulker.setInventorySlotContents(
                    random.nextInt(shulker.getSizeInventory()),
                    singleTreasureSelector.select(random).apply(random)
                );
            })
            .build();
    
    @Override
    public boolean func_230362_a_(
        ISeedReader serverWorldAccess,
        StructureManager accessor,
        ChunkGenerator generator,
        Random random,
        BlockPos pos,
        NoFeatureConfig config
    ) {
        ChunkPos chunkPos = new ChunkPos(pos);
    
        random.setSeed(chunkPos.asLong() + random.nextInt(2333));
    
        if (random.nextDouble() < 0.03) {
            generateOnce(serverWorldAccess, random, chunkPos);
        }
    
        return true;
    }
    
}
