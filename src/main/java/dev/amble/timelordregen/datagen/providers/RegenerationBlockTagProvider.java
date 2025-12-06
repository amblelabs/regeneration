package dev.amble.timelordregen.datagen.providers;

import dev.amble.timelordregen.core.RegenerationModBlocks;
import dev.amble.timelordregen.core.RegenerationTags;
import dev.amble.lib.datagen.tag.AmbleBlockTagProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.BlockTags;

import java.util.concurrent.CompletableFuture;

public class RegenerationBlockTagProvider extends AmbleBlockTagProvider {
    public RegenerationBlockTagProvider(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
        super(output, registriesFuture);
    }

    protected void configure(RegistryWrapper.WrapperLookup wrapperLookup) {
        super.configure(wrapperLookup);

        getOrCreateTagBuilder(RegenerationTags.Blocks.CADON_LOGS)
                .add(RegenerationModBlocks.CADON_LOG);

        getOrCreateTagBuilder(BlockTags.PLANKS)
                .add(RegenerationModBlocks.CADON_PLANKS);

        getOrCreateTagBuilder(BlockTags.LEAVES)
                .add(RegenerationModBlocks.CADON_LEAVES);

        getOrCreateTagBuilder(BlockTags.LOGS_THAT_BURN)
                .add(RegenerationModBlocks.CADON_LOG)
                .add(RegenerationModBlocks.CADON_WOOD)
                .add(RegenerationModBlocks.STRIPPED_CADON_LOG)
                .add(RegenerationModBlocks.STRIPPED_CADON_WOOD)
                .add(RegenerationModBlocks.CADON_PLANKS)
                .add(RegenerationModBlocks.CADON_SLAB)
                .add(RegenerationModBlocks.CADON_STAIRS)
                .add(RegenerationModBlocks.CADON_FENCE)
                .add(RegenerationModBlocks.CADON_DOOR)
                .add(RegenerationModBlocks.CADON_BUTTON)
                .add(RegenerationModBlocks.CADON_TRAPDOOR)
                .add(RegenerationModBlocks.CADON_FENCE_GATE);

        getOrCreateTagBuilder(BlockTags.TRAPDOORS)
                .add(RegenerationModBlocks.CADON_TRAPDOOR);

        getOrCreateTagBuilder(BlockTags.WOODEN_DOORS)
                .add(RegenerationModBlocks.CADON_DOOR);

        getOrCreateTagBuilder(BlockTags.WOODEN_STAIRS)
                .add(RegenerationModBlocks.CADON_STAIRS);

        getOrCreateTagBuilder(BlockTags.WOODEN_SLABS)
                .add(RegenerationModBlocks.CADON_SLAB);

        getOrCreateTagBuilder(BlockTags.WOODEN_BUTTONS)
                .add(RegenerationModBlocks.CADON_BUTTON);

        getOrCreateTagBuilder(BlockTags.WOODEN_FENCES)
                .add(RegenerationModBlocks.CADON_FENCE)
                .add(RegenerationModBlocks.CADON_FENCE_GATE);

        getOrCreateTagBuilder(BlockTags.WOODEN_PRESSURE_PLATES)
                .add(RegenerationModBlocks.CADON_PRESSURE_PLATE);
    }
}
