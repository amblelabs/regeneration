package ars;

import ars.advancement.RegenerationCriterions;
import ars.api.RegenerationEvents;
import ars.commands.RegenCommand;
import ars.compat.Compat;
import ars.core.*;
import ars.particle_effects.RegenParticleEffect;
import ars.data.Attachments;
import ars.network.Networking;
import ars.animation.RegenAnimRegistry;
import dev.amble.lib.container.RegistryContainer;
import dev.amble.lib.register.AmbleRegistries;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.DefaultParticleType;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RegenerationMod implements ModInitializer {

    public static final String MOD_ID = "timelordregen";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final Random RANDOM = Random.create();

    /*public static final Identifier REGEN_SOUND_ID = new Identifier(MOD_ID, "regeneration");
    public static final SoundEvent REGEN_SOUND = Registry.register(
            Registries.SOUND_EVENT,
            REGEN_SOUND_ID,
            SoundEvent.of(REGEN_SOUND_ID)
    );*/


	public static final ParticleType<RegenParticleEffect> RIGHT_REGEN_PARTICLE = FabricParticleTypes.complex(true, RegenParticleEffect.PARAMETERS_FACTORY);

	public static final DefaultParticleType REGEN_HEAD_PARTICLE = FabricParticleTypes.simple();

    @Override
    public void onInitialize() {
	    LOGGER.info("E Cineribus Resurgam.");

	    Attachments.init();
        RegenerationModDimensions.init();
        Networking.registerServerReceivers();
        RegistryContainer.register(RegenerationModItemGroups.class, MOD_ID);
        RegistryContainer.register(RegenerationModBlocks.class, MOD_ID);
        RegistryContainer.register(RegenerationModItems.class, MOD_ID);
	    RegenerationSounds.init();
        RegenerationEvents.registerListeners();

	    AmbleRegistries.getInstance().registerAll(RegenAnimRegistry.getInstance());

		// Register particles
		registerParticles();

        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            RegenCommand.register(dispatcher);
        });

        // Init regeneration manager
		RegenerationCore.init();
	    Compat.init();
	    RegenerationCriterions.init();

	}


	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}

	public void registerParticles() {
		Registry.register(Registries.PARTICLE_TYPE, id("right_regen_particle"), RIGHT_REGEN_PARTICLE);
		Registry.register(Registries.PARTICLE_TYPE, id("regen_head_particle"), REGEN_HEAD_PARTICLE);
	}
}