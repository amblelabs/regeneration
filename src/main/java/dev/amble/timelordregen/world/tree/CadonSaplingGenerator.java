package dev.amble.timelordregen.world.tree;

import net.minecraft.block.sapling.SaplingGenerator;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import org.jetbrains.annotations.Nullable;

public class CadonSaplingGenerator extends SaplingGenerator {

    public static final RegistryKey<ConfiguredFeature<?, ?>> CADON_SMALL_OAK =
            RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, new Identifier("timelordregen", "small_cadon_oak"));

    public static final RegistryKey<ConfiguredFeature<?, ?>> CADON_BIG_OAK =
            RegistryKey.of(RegistryKeys.CONFIGURED_FEATURE, new Identifier("timelordregen", "big_cadon_oak"));

    @Nullable
    @Override
    protected RegistryKey<ConfiguredFeature<?, ?>> getTreeFeature(Random random, boolean bees) {
        if (random.nextInt(10) == 0) return CADON_BIG_OAK;
        return CADON_SMALL_OAK;
    }
}
