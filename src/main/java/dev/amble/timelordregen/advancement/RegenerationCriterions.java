package dev.amble.timelordregen.advancement;

import dev.amble.lib.advancement.SimpleCriterion;
import dev.amble.timelordregen.RegenerationMod;
import dev.amble.timelordregen.api.RegenerationEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class RegenerationCriterions {
	public static final SimpleCriterion FIRST_REGENERATION = SimpleCriterion.create(Identifier.of(RegenerationMod.MOD_ID, "first_regeneration")).register();
	public static final SimpleCriterion DELAY_REGENERATION = SimpleCriterion.create(Identifier.of(RegenerationMod.MOD_ID, "delay_regeneration")).register();

	public static void init() {
		RegenerationMod.LOGGER.info("Initializing Regeneration Criterions");

		RegenerationEvents.START.register((entity, data) -> {
			if (!(entity instanceof ServerPlayerEntity player)) return;

			FIRST_REGENERATION.trigger(player);
		});

		RegenerationEvents.DELAY_FURTHER.register((entity, data) -> {
			if (!(entity instanceof ServerPlayerEntity player)) return;

			DELAY_REGENERATION.trigger(player);
		});
	}
}
