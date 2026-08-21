package dev.amble.timelordregen.data.tree;

import dev.amble.timelordregen.RegenerationMod;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

public class RegenerationSounds {
	public static final SoundEvent SAD_REGEN_START = register("sad_regen_start");
	public static final SoundEvent SAD_REGEN_LOOP = register("sad_regen_loop");
	public static final SoundEvent SAD_REGEN_END = register("sad_regen_end");
	public static final SoundEvent KNEEL_REGEN_START = register("kneel_regen_start");
	public static final SoundEvent KNEEL_REGEN_LOOP = register("kneel_regen_loop");
	public static final SoundEvent KNEEL_REGEN_END = register("kneel_regen_end");
	public static final SoundEvent ELEVEN_REGEN_START = register("eleven_regen_start");
	public static final SoundEvent ELEVEN_REGEN_LOOP = register("eleven_regen_loop");
	public static final SoundEvent ELEVEN_REGEN_END = register("eleven_regen_end");
	public static final SoundEvent SWING_REGEN_START = register("swing_regen_start");
	public static final SoundEvent SWING_REGEN_LOOP = register("swing_regen_loop");
	public static final SoundEvent SWING_REGEN_END = register("swing_regen_end");

    private static SoundEvent register(String name) {
        Identifier id = RegenerationMod.id(name);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }
    public static void init() {
    }
}
