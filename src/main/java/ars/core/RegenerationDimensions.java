package ars.core;

import ars.RegenerationMod;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.world.World;

public class RegenerationDimensions {
    public static final RegistryKey<World> GALLIFREY = RegistryKey.of(RegistryKeys.WORLD,
            RegenerationMod.id("gallifrey"));
}

