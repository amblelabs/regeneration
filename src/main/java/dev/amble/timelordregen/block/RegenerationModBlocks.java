package dev.amble.timelordregen.block;

import dev.amble.timelordregen.data.tree.CadonSaplingGenerator;
import dev.amble.timelordregen.core.RegenerationModItemGroups;
import dev.amble.lib.block.ABlockSettings;
import dev.amble.lib.container.impl.BlockContainer;
import dev.amble.lib.container.impl.NoBlockItem;
import dev.amble.lib.datagen.util.NoBlockDrop;
import dev.amble.lib.datagen.util.NoEnglish;
import dev.amble.lib.datagen.util.ShovelMineable;
import dev.amble.lib.item.AItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.registry.CompostingChanceRegistry;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.fabric.api.registry.FlattenableBlockRegistry;
import net.minecraft.block.*;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.WorldView;

public class RegenerationModBlocks extends BlockContainer {

    //GALLIFREY BLOCK / 伽理弗雷方块

    @NoEnglish
    public static final Block GALLIFREY_STONE = new Block(ABlockSettings.copyOf(Blocks.STONE));

    @NoEnglish
    public static final Block CHISELED_GALLIFREY_STONE_BRICK = new Block(ABlockSettings.copyOf(Blocks.CHISELED_STONE_BRICKS));

    @NoEnglish
    public static final Block GALLIFREY_STONE_BRICKS = new Block(ABlockSettings.copyOf(Blocks.STONE_BRICKS));

    @NoEnglish
    public static final Block GALLIFREY_IRON_ORE = new Block(ABlockSettings.copyOf(Blocks.IRON_ORE));

    @NoEnglish
    public static final Block GALLIFREY_COPPER_ORE = new Block(ABlockSettings.copyOf(Blocks.COPPER_ORE));

    @NoEnglish
    public static final Block GALLIFREY_GOLD_ORE = new Block(ABlockSettings.copyOf(Blocks.GOLD_ORE));

    @NoEnglish
    public static final Block GALLIFREY_DIAMOND_ORE = new Block(ABlockSettings.copyOf(Blocks.DIAMOND_ORE));

    @NoEnglish
    public static final Block AZBANTIUM_ORE = new Block(ABlockSettings.copyOf(Blocks.OBSIDIAN));

    @NoEnglish
    @NoBlockDrop
    @ShovelMineable
    public static final Block GALLIFREY_GRASS_BLOCK = new GrassBlock(AbstractBlock.Settings.copy(Blocks.GRASS_BLOCK));

    //CADON / 卡顿木

    @NoEnglish
    public static final Block CADON_LOG = new PillarBlock(ABlockSettings.copyOf(Blocks.DARK_OAK_LOG));

    @NoEnglish
    public static final Block STRIPPED_CADON_LOG = new PillarBlock(ABlockSettings.copyOf(Blocks.STRIPPED_DARK_OAK_LOG));

    @NoEnglish
    public static final Block CADON_WOOD = new PillarBlock(ABlockSettings.copyOf(Blocks.DARK_OAK_WOOD));

    @NoEnglish
    public static final Block STRIPPED_CADON_WOOD = new PillarBlock(ABlockSettings.copyOf(Blocks.STRIPPED_DARK_OAK_WOOD));

    @NoEnglish
    @NoBlockDrop
    public static final Block CADON_LEAVES = new LeavesBlock(ABlockSettings.copyOf(Blocks.DARK_OAK_LEAVES));

    @NoEnglish
    public static final Block CADON_PLANKS = new Block(ABlockSettings.copyOf(Blocks.DARK_OAK_PLANKS));

    @NoEnglish
    public static final Block CADON_SLAB = new SlabBlock(ABlockSettings.copyOf(Blocks.DARK_OAK_SLAB));

    @NoEnglish
    public static final Block CADON_STAIRS = new StairsBlock(CADON_PLANKS.getDefaultState(), ABlockSettings.copyOf(Blocks.DARK_OAK_STAIRS));

    @NoEnglish
    public static final Block CADON_BUTTON = new ButtonBlock(ABlockSettings.copyOf(Blocks.DARK_OAK_BUTTON), BlockSetType.DARK_OAK, 30, true);

    @NoEnglish
    public static final Block CADON_DOOR = new DoorBlock(ABlockSettings.copyOf(Blocks.DARK_OAK_DOOR), BlockSetType.DARK_OAK);

    @NoEnglish
    public static final Block CADON_TRAPDOOR = new TrapdoorBlock(ABlockSettings.copyOf(Blocks.DARK_OAK_TRAPDOOR), BlockSetType.DARK_OAK);

    @NoEnglish
    public static final Block CADON_PRESSURE_PLATE = new PressurePlateBlock(PressurePlateBlock.ActivationRule.EVERYTHING,
            ABlockSettings.copyOf(Blocks.DARK_OAK_PRESSURE_PLATE), BlockSetType.DARK_OAK);

    @NoEnglish
    public static final Block CADON_FENCE = new FenceBlock(ABlockSettings.copyOf(Blocks.DARK_OAK_FENCE));

    @NoEnglish
    public static final Block CADON_FENCE_GATE = new FenceGateBlock(ABlockSettings.copyOf(Blocks.DARK_OAK_FENCE_GATE), WoodType.DARK_OAK);


    @NoEnglish
    public static final Block CADON_SAPLING = new SaplingBlock(new CadonSaplingGenerator(),ABlockSettings.copyOf(Blocks.OAK_SAPLING));

    //PLANT / 植物

    @NoEnglish
    @NoBlockDrop
    public static final Block FLOWER_OF_REMEMBRANCE = new FlowerBlock(StatusEffects.NIGHT_VISION, 5,
            ABlockSettings.create()
                    .mapColor(MapColor.DARK_GREEN).noCollision()
                    .breakInstantly().sounds(BlockSoundGroup.GRASS).offset(AbstractBlock.OffsetType.XZ)
                    .pistonBehavior(PistonBehavior.DESTROY));


    @NoBlockDrop
    @NoBlockItem
    public static final Block POTTED_FLOWER_OF_REMEMBRANCE = new FlowerPotBlock(FLOWER_OF_REMEMBRANCE, FabricBlockSettings
            .copyOf(Blocks.POTTED_POPPY)
            .nonOpaque()
    );


    @NoEnglish
    @NoBlockDrop
    public static final Block MOONLIGHT_BLOOM = new FlowerBlock(StatusEffects.JUMP_BOOST, 5,
            ABlockSettings.create()
                    .mapColor(MapColor.DARK_GREEN).noCollision()
                    .breakInstantly().sounds(BlockSoundGroup.GRASS).offset(AbstractBlock.OffsetType.XZ)
                    .pistonBehavior(PistonBehavior.DESTROY));


    @NoEnglish
    @NoBlockDrop
    public static final Block TYPHA_POD = new FernBlock(ABlockSettings.create()
            .replaceable()
            .noCollision()
            .breakInstantly()
            .sounds(BlockSoundGroup.GRASS)
            .offset(AbstractBlock.OffsetType.XZ)
            .pistonBehavior(PistonBehavior.DESTROY)
            .mapColor(MapColor.DARK_GREEN)
    ) {
        @Override
        public boolean isFertilizable(WorldView world, BlockPos pos, BlockState state, boolean isClient) {
            return false;
        }
    };

    @Override
    public Item.Settings createBlockItemSettings(Block block) {
        return new AItemSettings().group(RegenerationModItemGroups.REGEN);
    }

    @Override
    public void finish() {
        super.finish();

        FlammableBlockRegistry flammable = FlammableBlockRegistry.getDefaultInstance();
        flammable.add(CADON_LOG, 5, 5);
        flammable.add(STRIPPED_CADON_LOG, 5, 5);
        flammable.add(CADON_WOOD, 5, 5);
        flammable.add(STRIPPED_CADON_WOOD, 5, 5);
        flammable.add(CADON_PLANKS, 5, 20);
        flammable.add(CADON_SLAB, 5, 20);
        flammable.add(CADON_STAIRS, 5, 20);
        flammable.add(CADON_FENCE, 5, 20);
        flammable.add(CADON_FENCE_GATE, 5, 20);
        flammable.add(CADON_LEAVES, 30, 60);
        flammable.add(FLOWER_OF_REMEMBRANCE, 60, 100);
        flammable.add(MOONLIGHT_BLOOM, 60, 100);
        flammable.add(TYPHA_POD, 60, 100);

        FlattenableBlockRegistry.register(GALLIFREY_GRASS_BLOCK, Blocks.DIRT_PATH.getDefaultState());

        CompostingChanceRegistry compost = CompostingChanceRegistry.INSTANCE;
        compost.add(CADON_LEAVES, 0.3f);
        compost.add(CADON_SAPLING, 0.3f);
        compost.add(TYPHA_POD, 0.3f);
        compost.add(FLOWER_OF_REMEMBRANCE, 0.65f);
        compost.add(MOONLIGHT_BLOOM, 0.65f);
    }

}
