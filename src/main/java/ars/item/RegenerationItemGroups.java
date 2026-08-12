package ars.item;

import ars.RegenerationMod;
import dev.amble.lib.container.impl.ItemGroupContainer;
import dev.amble.lib.itemgroup.AItemGroup;
import net.minecraft.item.ItemStack;

public class RegenerationItemGroups implements ItemGroupContainer {

    public static final AItemGroup REGEN = AItemGroup.builder(RegenerationMod.id("item_group"))
            .icon(() -> new ItemStack(RegenerationItems.ELIXIR_OF_LIFE))
            .entries(
                    (displayContext, entries) -> {
                    }
            )
            .build();
}
