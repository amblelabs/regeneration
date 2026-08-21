package dev.amble.timelordregen.block;

import dev.amble.timelordregen.data.tree.CadonSaplingGenerator;
import dev.amble.timelordregen.item.RegenerationItemGroups;
import dev.amble.timelordregen.util.ExBoatType;
import dev.amble.lib.block.ABlockSettings;
import dev.amble.lib.container.impl.BlockContainer;
import dev.amble.lib.datagen.util.NoBlockDrop;
import dev.amble.lib.datagen.util.NoEnglish;
import dev.amble.lib.datagen.util.ShovelMineable;
import dev.amble.lib.item.AItemSettings;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.*;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Item;
import net.minecraft.sound.BlockSoundGroup;

public class RegenerationModBlocks extends BlockContainer implements ExBoatType {

    //卡顿木
    @NoEnglish
    public static final Block GALLIFREY_STONE = new PillarBlock(ABlockSettings.copyOf(Blocks.STONE));

    @NoEnglish
    public static final Block AZBANTIUM_ORE = new PillarBlock(ABlockSettings.copyOf(Blocks.DIAMOND_ORE));

    @NoEnglish
    public static final Block CHISELED_GALLIFREY_STONE_BRICK = new PillarBlock(ABlockSettings.copyOf(Blocks.CHISELED_STONE_BRICKS));

    public static final Block GALLIFREY_STONE_BRICKS = new PillarBlock(ABlockSettings.copyOf(Blocks.STONE_BRICKS));

    @NoEnglish
    public static final Block GALLIFREY_COPPER_ORE = new PillarBlock(ABlockSettings.copyOf(Blocks.COPPER_ORE));

    @NoEnglish
    public static final Block GALLIFREY_DIAMOND_ORE = new PillarBlock(ABlockSettings.copyOf(Blocks.DIAMOND_ORE));

    @NoEnglish
    public static final Block GALLIFREY_GOLD_ORE = new PillarBlock(ABlockSettings.copyOf(Blocks.GOLD_ORE));

    @NoEnglish
    public static final Block GALLIFREY_IRON_ORE = new PillarBlock(ABlockSettings.copyOf(Blocks.IRON_ORE));

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
    public static final Block CADON_BUTTON = new ButtonBlock(ABlockSettings.copyOf(Blocks.DARK_OAK_BUTTON), BlockSetType.DARK_OAK, 10, true);

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
    @NoBlockDrop
    @ShovelMineable
    public static final Block GALLIFREY_GRASS_BLOCK =
            new GrassBlock(AbstractBlock.Settings.copy(Blocks.GRASS_BLOCK));


    @NoEnglish
    public static final Block CADON_SAPLING = new SaplingBlock(new CadonSaplingGenerator(),ABlockSettings.copyOf(Blocks.OAK_SAPLING));


    @NoEnglish
    @NoBlockDrop
    public static final Block FLOWER_OF_REMEMBRANCE = new FlowerBlock(StatusEffects.NIGHT_VISION, 5,
            ABlockSettings.create()
                    .mapColor(MapColor.DARK_GREEN).noCollision()
                    .breakInstantly().sounds(BlockSoundGroup.GRASS).offset(AbstractBlock.OffsetType.XZ)
                    .pistonBehavior(PistonBehavior.DESTROY));


    @NoBlockDrop
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
    );

    @Override
    public Item.Settings createBlockItemSettings(Block block) {
        if (block == RegenerationModBlocks.POTTED_FLOWER_OF_REMEMBRANCE) {
            return new AItemSettings();
        }
        return new AItemSettings().group(RegenerationItemGroups.REGEN);
    }

}
