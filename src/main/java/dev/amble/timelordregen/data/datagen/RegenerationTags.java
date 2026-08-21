package dev.amble.timelordregen.data.datagen;

import dev.amble.timelordregen.RegenerationMod;
import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;

public class RegenerationTags {

    public static class Blocks {
        public static final TagKey<Block> PREVENTS_REGENERATION = createTag("prevents_regeneration");
        public static final TagKey<Block> CADON_LOGS = createTag("cadon_logs");

        private static TagKey<Block> createTag(String name) {
            return TagKey.of(RegistryKeys.BLOCK, RegenerationMod.id(name));
        }
    }

    public static class Items {
        public static final TagKey<Item> CADON_LOGS = createTag("cadon_logs");

        private static TagKey<Item> createTag(String name) {
            return TagKey.of(RegistryKeys.ITEM, RegenerationMod.id(name));
        }
    }
}
