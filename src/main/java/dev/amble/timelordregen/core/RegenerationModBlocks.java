package dev.amble.timelordregen.core;

import dev.amble.lib.block.ABlockSettings;
import dev.amble.lib.container.impl.BlockContainer;
import dev.amble.lib.datagen.util.AxeMineable;
import dev.amble.lib.datagen.util.NoBlockDrop;
import dev.amble.lib.datagen.util.NoEnglish;
import dev.amble.lib.datagen.util.ShovelMineable;
import dev.amble.lib.item.AItemSettings;
import dev.amble.timelordregen.world.tree.CadonSaplingGenerator;
import net.minecraft.block.*;
import net.minecraft.item.Item;
import net.minecraft.sound.BlockSoundGroup;

public class RegenerationModBlocks extends BlockContainer {

    // Cadon Wood
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
    @ShovelMineable(tool = ShovelMineable.Tool.STONE)
    public static final Block GALLIFREY_GRASS_BLOCK = new GrassBlock(ABlockSettings.copy(Blocks.GRASS_BLOCK));

    @NoEnglish
    public static final Block CADON_SAPLING = new SaplingBlock(new CadonSaplingGenerator(),ABlockSettings.copyOf(Blocks.OAK_SAPLING));

    @Override
    public Item.Settings createBlockItemSettings(Block block) {
        return new AItemSettings().group(RegenerationModItemGroups.REGEN);
    }
}
