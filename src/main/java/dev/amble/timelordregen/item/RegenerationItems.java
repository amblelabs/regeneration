package dev.amble.timelordregen.item;

import dev.amble.timelordregen.item.data.ElixirOfLifeItem;
import dev.amble.timelordregen.item.data.PocketWatchItem;
import dev.amble.lib.container.impl.ItemContainer;
import dev.amble.lib.datagen.util.NoEnglish;
import dev.amble.lib.item.AItemSettings;
import net.minecraft.item.Item;

public class RegenerationItems extends ItemContainer {

    @NoEnglish
    public static final Item ELIXIR_OF_LIFE = new ElixirOfLifeItem(new AItemSettings().group(RegenerationItemGroups.REGEN).maxCount(16));

	@NoEnglish
	public static final Item POCKET_WATCH = new PocketWatchItem(new AItemSettings().group(RegenerationItemGroups.REGEN));
}
