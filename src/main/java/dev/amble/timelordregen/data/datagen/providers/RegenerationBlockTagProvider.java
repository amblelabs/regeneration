package dev.amble.timelordregen.data.datagen.providers;

import dev.amble.timelordregen.block.RegenerationModBlocks;
import dev.amble.lib.datagen.tag.AmbleBlockTagProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;

import java.util.concurrent.CompletableFuture;

public class RegenerationBlockTagProvider extends AmbleBlockTagProvider {
    public RegenerationBlockTagProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup wrapperLookup) {
        super.configure(wrapperLookup);

        // ==================== 基础挖掘标签 ====================
        getOrCreateTagBuilder(BlockTags.AXE_MINEABLE)
                .add(RegenerationModBlocks.CADON_LOG)
                .add(RegenerationModBlocks.STRIPPED_CADON_LOG)
                .add(RegenerationModBlocks.CADON_WOOD)
                .add(RegenerationModBlocks.STRIPPED_CADON_WOOD)
                .add(RegenerationModBlocks.CADON_PLANKS)
                .add(RegenerationModBlocks.CADON_STAIRS)
                .add(RegenerationModBlocks.CADON_SLAB)
                .add(RegenerationModBlocks.CADON_FENCE)
                .add(RegenerationModBlocks.CADON_FENCE_GATE)
                .add(RegenerationModBlocks.CADON_DOOR)
                .add(RegenerationModBlocks.CADON_TRAPDOOR)
                .add(RegenerationModBlocks.CADON_PRESSURE_PLATE)
                .add(RegenerationModBlocks.CADON_BUTTON);

        getOrCreateTagBuilder(BlockTags.HOE_MINEABLE)
                .add(RegenerationModBlocks.CADON_LEAVES);

        // ==================== 原木/木头标签 ====================
        getOrCreateTagBuilder(BlockTags.LOGS)
                .add(RegenerationModBlocks.CADON_LOG)
                .add(RegenerationModBlocks.STRIPPED_CADON_LOG)
                .add(RegenerationModBlocks.CADON_WOOD)
                .add(RegenerationModBlocks.STRIPPED_CADON_WOOD);

        getOrCreateTagBuilder(BlockTags.LOGS_THAT_BURN)
                .add(RegenerationModBlocks.CADON_LOG)
                .add(RegenerationModBlocks.STRIPPED_CADON_LOG)
                .add(RegenerationModBlocks.CADON_WOOD)
                .add(RegenerationModBlocks.STRIPPED_CADON_WOOD);

        // ==================== 木板系列标签 ====================
        getOrCreateTagBuilder(BlockTags.PLANKS)
                .add(RegenerationModBlocks.CADON_PLANKS);

        getOrCreateTagBuilder(BlockTags.WOODEN_STAIRS)
                .add(RegenerationModBlocks.CADON_STAIRS);

        getOrCreateTagBuilder(BlockTags.WOODEN_SLABS)
                .add(RegenerationModBlocks.CADON_SLAB);

        getOrCreateTagBuilder(BlockTags.WOODEN_FENCES)
                .add(RegenerationModBlocks.CADON_FENCE);

        getOrCreateTagBuilder(BlockTags.FENCE_GATES)
                .add(RegenerationModBlocks.CADON_FENCE_GATE);

        getOrCreateTagBuilder(BlockTags.WOODEN_DOORS)
                .add(RegenerationModBlocks.CADON_DOOR);

        getOrCreateTagBuilder(BlockTags.WOODEN_TRAPDOORS)
                .add(RegenerationModBlocks.CADON_TRAPDOOR);

        getOrCreateTagBuilder(BlockTags.WOODEN_PRESSURE_PLATES)
                .add(RegenerationModBlocks.CADON_PRESSURE_PLATE);

        getOrCreateTagBuilder(BlockTags.WOODEN_BUTTONS)
                .add(RegenerationModBlocks.CADON_BUTTON);

        // ==================== 树叶和树苗 ====================
        getOrCreateTagBuilder(BlockTags.LEAVES)
                .add(RegenerationModBlocks.CADON_LEAVES);

        getOrCreateTagBuilder(BlockTags.SAPLINGS)
                .add(RegenerationModBlocks.CADON_SAPLING);

        // 注意：BlockTags.FLAMMABLE 在 1.20.1 中不存在，已移除
    }
}