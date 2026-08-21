package dev.amble.timelordregen.data.datagen.providers;

import dev.amble.timelordregen.block.RegenerationModBlocks;
import dev.amble.timelordregen.data.datagen.RegenerationTags;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.ItemTags;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class RegenerationItemTagProvider extends FabricTagProvider<Item> {

    public RegenerationItemTagProvider(FabricDataOutput output,
                                       @Nullable CompletableFuture<RegistryWrapper.WrapperLookup> completableFuture) {
        super(output, RegistryKeys.ITEM, completableFuture);
    }

    @Override
    protected void configure(RegistryWrapper.WrapperLookup wrapperLookup) {
        getOrCreateTagBuilder(RegenerationTags.Items.CADON_LOGS)
                .add(RegenerationModBlocks.CADON_LOG.asItem());

        getOrCreateTagBuilder(ItemTags.PLANKS)
                .add(RegenerationModBlocks.CADON_PLANKS.asItem());

        getOrCreateTagBuilder(ItemTags.LEAVES)
                .add(RegenerationModBlocks.CADON_LEAVES.asItem());

        getOrCreateTagBuilder(ItemTags.LOGS_THAT_BURN)
                .add(RegenerationModBlocks.CADON_LOG.asItem())
                .add(RegenerationModBlocks.CADON_WOOD.asItem())
                .add(RegenerationModBlocks.STRIPPED_CADON_LOG.asItem())
                .add(RegenerationModBlocks.STRIPPED_CADON_WOOD.asItem())
                .add(RegenerationModBlocks.CADON_PLANKS.asItem())
                .add(RegenerationModBlocks.CADON_SLAB.asItem())
                .add(RegenerationModBlocks.CADON_STAIRS.asItem())
                .add(RegenerationModBlocks.CADON_FENCE.asItem())
                .add(RegenerationModBlocks.CADON_DOOR.asItem())
                .add(RegenerationModBlocks.CADON_BUTTON.asItem())
                .add(RegenerationModBlocks.CADON_TRAPDOOR.asItem())
                .add(RegenerationModBlocks.CADON_FENCE_GATE.asItem());

        getOrCreateTagBuilder(ItemTags.TRAPDOORS)
                .add(RegenerationModBlocks.CADON_TRAPDOOR.asItem());

        getOrCreateTagBuilder(ItemTags.WOODEN_DOORS)
                .add(RegenerationModBlocks.CADON_DOOR.asItem());

        getOrCreateTagBuilder(ItemTags.WOODEN_STAIRS)
                .add(RegenerationModBlocks.CADON_STAIRS.asItem());

        getOrCreateTagBuilder(ItemTags.WOODEN_SLABS)
                .add(RegenerationModBlocks.CADON_SLAB.asItem());

        getOrCreateTagBuilder(ItemTags.WOODEN_BUTTONS)
                .add(RegenerationModBlocks.CADON_BUTTON.asItem());

        getOrCreateTagBuilder(ItemTags.WOODEN_FENCES)
                .add(RegenerationModBlocks.CADON_FENCE.asItem())
                .add(RegenerationModBlocks.CADON_FENCE_GATE.asItem());

        getOrCreateTagBuilder(ItemTags.WOODEN_PRESSURE_PLATES)
                .add(RegenerationModBlocks.CADON_PRESSURE_PLATE.asItem());
    }
}
