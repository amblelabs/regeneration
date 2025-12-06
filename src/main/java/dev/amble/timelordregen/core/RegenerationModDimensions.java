package dev.amble.timelordregen.core;

import dev.amble.timelordregen.RegenerationMod;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.World;

public class RegenerationModDimensions {
    public static final RegistryKey<World> GALLIFREY = RegistryKey.of(RegistryKeys.WORLD,
            RegenerationMod.id("gallifrey"));

    public static void init() {}
}
