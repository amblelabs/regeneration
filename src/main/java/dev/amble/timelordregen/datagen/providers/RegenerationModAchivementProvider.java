package dev.amble.timelordregen.datagen.providers;

import dev.amble.ait.core.AITItemGroups;
import dev.amble.ait.core.AITItems;
import dev.amble.timelordregen.RegenerationMod;
import dev.amble.timelordregen.advancement.RegenerationCriterions;
import dev.amble.timelordregen.core.RegenerationModItems;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancement.Advancement;
import net.minecraft.advancement.AdvancementFrame;
import net.minecraft.advancement.criterion.InventoryChangedCriterion;
import net.minecraft.advancement.criterion.RecipeCraftedCriterion;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

import java.util.function.Consumer;

public class RegenerationModAchivementProvider extends FabricAdvancementProvider {
    public RegenerationModAchivementProvider(FabricDataOutput output) {
        super(output);
    }

    @Override
    public void generateAdvancement(Consumer<Advancement> consumer) {
		Advancement regeneration = Advancement.Builder.create()
				.display(RegenerationModItems.ELIXIR_OF_LIFE, Text.translatable("achievement.timelordregen.title.regeneration"),
						Text.translatable("achievement.timelordregen.description.regeneration"),
						null, AdvancementFrame.GOAL, true, true, false)
				.criterion("first_regeneration", RegenerationCriterions.FIRST_REGENERATION.conditions())
				.build(consumer, RegenerationMod.MOD_ID + "/first_regeneration");


	    Advancement delay = Advancement.Builder.create()
			    .display(RegenerationModItems.ELIXIR_OF_LIFE, Text.translatable("achievement.timelordregen.title.delay"),
					    Text.translatable("achievement.timelordregen.description.delay"),
					    null, AdvancementFrame.GOAL, true, true, false)
			    .criterion("delay_regeneration", RegenerationCriterions.DELAY_REGENERATION.conditions())
			    .build(consumer, RegenerationMod.MOD_ID + "/delay_regeneration");

		Advancement watch = Advancement.Builder.create()
				.parent(regeneration)
				.display(RegenerationModItems.POCKET_WATCH, Text.translatable("achievement.timelordregen.title.watch"),
						Text.translatable("achievement.timelordregen.description.watch"),
						null, AdvancementFrame.GOAL, true, true, false)
				.criterion("get_watch", InventoryChangedCriterion.Conditions.items(RegenerationModItems.POCKET_WATCH))
				.build(consumer, RegenerationMod.MOD_ID + "/get_watch");
    }
}
