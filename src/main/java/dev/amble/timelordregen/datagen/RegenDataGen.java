package dev.amble.timelordregen.datagen;

import dev.amble.timelordregen.core.RegenerationModBlocks;
import dev.amble.timelordregen.core.RegenerationModItemGroups;
import dev.amble.timelordregen.core.RegenerationModItems;
import dev.amble.timelordregen.datagen.providers.*;
import dev.amble.lib.datagen.lang.AmbleLanguageProvider;
import dev.amble.lib.datagen.lang.LanguageType;
import dev.amble.lib.datagen.loot.AmbleBlockLootTable;
import dev.amble.lib.datagen.sound.AmbleSoundProvider;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.minecraft.registry.RegistryBuilder;
import net.minecraft.registry.RegistryKeys;

public class RegenDataGen implements DataGeneratorEntrypoint {
	@Override
	public void onInitializeDataGenerator(FabricDataGenerator gen) {
		FabricDataGenerator.Pack pack = gen.createPack();

		genLang(pack);
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
	private void genLang(FabricDataGenerator.Pack pack) {
		genEnglish(pack);
	}

	private void genEnglish(FabricDataGenerator.Pack pack) {
		pack.addProvider((((output, registry) -> {
			AmbleLanguageProvider provider = new AmbleLanguageProvider(output, LanguageType.EN_US);

            // Commands
            provider.addTranslation("command.regen.name","regen");
            provider.addTranslation("command.regenui.name","regenui");
            provider.addTranslation("command.regen.data.error","Regeneration data not found.");
            provider.addTranslation("command.regen.triggered","Regeneration triggered!");
            provider.addTranslation("command.regen.fail","No regenerations left or already regenerating.");

            // GUI
            provider.addTranslation("gui.regen.settings.title","Regeneration Settings");
            provider.addTranslation("gui.regen.settings.remaining","Remaining Regenerations: %s");

            // Item Groups
            provider.addTranslation(RegenerationModItemGroups.REGEN,"Regeneration Mod");

			// Items
            provider.addTranslation(RegenerationModItems.ELIXIR_OF_LIFE,"Elixir of Life");
            provider.addTranslation(RegenerationModItems.POCKET_WATCH,"Chameleon Arch");

            // Blocks
            provider.addTranslation(RegenerationModBlocks.CADON_LOG, "Cadon Log");
            provider.addTranslation(RegenerationModBlocks.STRIPPED_CADON_LOG, "Stripped Cadon Log");
            provider.addTranslation(RegenerationModBlocks.CADON_WOOD, "Cadon Wood");
            provider.addTranslation(RegenerationModBlocks.STRIPPED_CADON_WOOD, "Stripped Cadon Wood");
            provider.addTranslation(RegenerationModBlocks.CADON_PLANKS, "Cadon Planks");
            provider.addTranslation(RegenerationModBlocks.CADON_LEAVES, "Cadon Leaves");
            provider.addTranslation(RegenerationModBlocks.CADON_STAIRS, "Cadon Stairs");
            provider.addTranslation(RegenerationModBlocks.CADON_SLAB, "Cadon Slab");
            provider.addTranslation(RegenerationModBlocks.CADON_FENCE, "Cadon Fence");
            provider.addTranslation(RegenerationModBlocks.CADON_FENCE_GATE, "Cadon Fence Gate");
            provider.addTranslation(RegenerationModBlocks.CADON_DOOR, "Cadon Door");
            provider.addTranslation(RegenerationModBlocks.CADON_TRAPDOOR, "Cadon Trapdoor");
            provider.addTranslation(RegenerationModBlocks.CADON_PRESSURE_PLATE, "Cadon Pressure Plate");
            provider.addTranslation(RegenerationModBlocks.CADON_BUTTON, "Cadon Button");
            provider.addTranslation(RegenerationModBlocks.GALLIFREY_GRASS_BLOCK, "Gallifrey Grass Block");

			// Advancements
			provider.addTranslation("achievement.timelordregen.title.regeneration", "Change, my dear.");
			provider.addTranslation("achievement.timelordregen.description.regeneration", "Regenerate for the first time!");


			provider.addTranslation("achievement.timelordregen.title.watch", "More than just a fobwatch!");
			provider.addTranslation("achievement.timelordregen.description.watch", "Obtain a Chameleon Arch - a container for regeneration essence.");

			provider.addTranslation("achievement.timelordregen.title.delay", "I don't want to go..");
			provider.addTranslation("achievement.timelordregen.description.delay", "Delay your regeneration");

			// Origins

			provider.addTranslation("origin.timelordregen.timelord.name", "Timelord");
			provider.addTranslation("origin.timelordregen.timelord.description", "You are a Timelord, an alien race from the planet Gallifrey which can escape death via regeneration.");

			provider.addTranslation("origin.timelordregen.regeneration.name", "Regeneration");
			provider.addTranslation("origin.timelordregen.regeneration.description", "Your body has a way of cheating death by regenerating every cell in your body, but only a few times. On your last regeneration you will become a new origin.");

			return provider;
		})));
	}

    public void generateRecipes(FabricDataGenerator.Pack pack) {
        pack.addProvider((((output, registriesFuture) -> {
            RegenerationRecipeProvider provider = new RegenerationRecipeProvider(output);
            return provider;

        })));
    }
}
