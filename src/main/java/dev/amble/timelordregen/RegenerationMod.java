package dev.amble.timelordregen;

import dev.amble.timelordregen.advancement.RegenerationCriterions;
import dev.amble.timelordregen.commands.RegenCommand;
import dev.amble.timelordregen.compat.Compat;
import dev.amble.timelordregen.core.*;
import dev.amble.timelordregen.core.particle_effects.RegenParticleEffect;
import dev.amble.timelordregen.data.Attachments;
import dev.amble.timelordregen.api.RegenerationInfo;
import dev.amble.timelordregen.network.Networking;
import dev.amble.timelordregen.animation.RegenAnimRegistry;
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
        Networking.register();
        RegistryContainer.register(RegenerationModItemGroups.class, MOD_ID);
        RegistryContainer.register(RegenerationModBlocks.class, MOD_ID);
        RegistryContainer.register(RegenerationModItems.class, MOD_ID);
	    RegenerationSounds.init();

	    AmbleRegistries.getInstance().registerAll(RegenAnimRegistry.getInstance());

		// Register particles
		registerParticles();

        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            RegenCommand.register(dispatcher);
        });

        // Init regeneration manager
		RegenerationInfo.init();
	    Compat.init();
	    RegenerationCriterions.init();

        //RegenerationBoats.registerBoats();
	}


	public static Identifier id(String path) {
		return Identifier.of(MOD_ID, path);
	}

	public void registerParticles() {
		Registry.register(Registries.PARTICLE_TYPE, id("right_regen_particle"), RIGHT_REGEN_PARTICLE);
		Registry.register(Registries.PARTICLE_TYPE, id("regen_head_particle"), REGEN_HEAD_PARTICLE);
	}
}