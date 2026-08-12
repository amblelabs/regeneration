package ars.datagen;

import ars.core.RegenerationModBlocks;
import ars.datagen.providers.*;
import ars.datagen.providers.*;
import dev.amble.lib.datagen.loot.AmbleBlockLootTable;
import dev.amble.lib.datagen.sound.AmbleSoundProvider;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

public class RegenDataGen implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator gen) {
        FabricDataGenerator.Pack pack = gen.createPack();
        genModels(pack);
        generateRecipes(pack);
        generateSoundData(pack);
        generateAchievement(pack);
        genTags(pack);
        genLoot(pack);
        pack.addProvider(RegenerationWorldGenerator::new);
    }

    private void generateAchievement(FabricDataGenerator.Pack pack) {
        pack.addProvider(RegenerationModAchivementProvider::new);
    }

    public void generateSoundData(FabricDataGenerator.Pack pack) {
        pack.addProvider((((output, registriesFuture) -> new AmbleSoundProvider(output))));
    }

    private void genTags(FabricDataGenerator.Pack pack) {
        pack.addProvider(RegenerationBlockTagProvider::new);
        pack.addProvider(RegenerationItemTagProvider::new);
    }

    private void genLoot(FabricDataGenerator.Pack pack) {
        pack.addProvider((((output, registriesFuture) -> new AmbleBlockLootTable(output).withBlocks(RegenerationModBlocks.class))));
    }

    private void genModels(FabricDataGenerator.Pack pack) {
        pack.addProvider(((output, registriesFuture) -> {
            RegenerationModModelGen provider = new RegenerationModModelGen(output);

            // Yes this is confusing but oh well - Loqor
            provider.registerLogBlock(RegenerationModBlocks.CADON_LOG, RegenerationModBlocks.CADON_WOOD);
            provider.registerLogBlock(RegenerationModBlocks.STRIPPED_CADON_LOG, RegenerationModBlocks.STRIPPED_CADON_WOOD);
            provider.registerBlockSet(new BlockSetRecord(
                    RegenerationModBlocks.CADON_PLANKS,
                    RegenerationModBlocks.CADON_STAIRS,
                    RegenerationModBlocks.CADON_SLAB,
                    RegenerationModBlocks.CADON_FENCE,
                    RegenerationModBlocks.CADON_FENCE_GATE,
                    RegenerationModBlocks.CADON_TRAPDOOR,
                    RegenerationModBlocks.CADON_DOOR,
                    RegenerationModBlocks.CADON_PRESSURE_PLATE,
                    RegenerationModBlocks.CADON_BUTTON
            ));
            provider.registerSimpleBlock(RegenerationModBlocks.CADON_LEAVES);

            return provider;
        }));
    }

    public void generateRecipes(FabricDataGenerator.Pack pack) {
        pack.addProvider((((output, registriesFuture) -> {
            RegenerationRecipeProvider provider = new RegenerationRecipeProvider(output);
            // 配方生成逻辑保留，无需修改
            return provider;
        })));
    }
}