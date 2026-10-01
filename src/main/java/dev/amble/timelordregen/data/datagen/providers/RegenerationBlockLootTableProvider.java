package dev.amble.timelordregen.data.datagen.providers;

import dev.amble.timelordregen.block.RegenerationModBlocks;
import dev.amble.lib.datagen.loot.AmbleBlockLootTable;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;

public class RegenerationBlockLootTableProvider extends AmbleBlockLootTable {
    public RegenerationBlockLootTableProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generate() {
        super.generate();

        addDrop(RegenerationModBlocks.CADON_DOOR, this::doorDrops);
        addDrop(RegenerationModBlocks.CADON_SLAB, this::slabDrops);
    }
}
