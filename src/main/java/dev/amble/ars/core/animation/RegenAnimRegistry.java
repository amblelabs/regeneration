package dev.amble.ars.core.animation;

import dev.amble.lib.register.datapack.SimpleDatapackRegistry;
import dev.amble.ars.RegenerationMod;
import net.minecraft.util.Identifier;

public class RegenAnimRegistry extends SimpleDatapackRegistry<AnimationTemplate> {
	private static final RegenAnimRegistry INSTANCE = new RegenAnimRegistry();

	private RegenAnimRegistry() {
		super(AnimationTemplate::fromInputStream, AnimationTemplate.CODEC, "regen_template", true, RegenerationMod.MOD_ID);
	}

	public static RegenAnimRegistry getInstance() {
		return INSTANCE;
	}

	@Override
	public AnimationTemplate fallback() {
		return this.get(Identifier.of("sad_regen", "regen_template"));
	}

	@Override
	protected void defaults() {

	}
}
