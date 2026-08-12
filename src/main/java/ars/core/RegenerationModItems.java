package ars.core;

import dev.amble.lib.container.impl.ItemContainer;
import dev.amble.lib.datagen.util.NoEnglish;
import dev.amble.lib.item.AItemSettings;
import ars.item.ElixirOfLifeItem;
import ars.item.PocketWatchItem;
import net.minecraft.item.Item;

public class RegenerationModItems extends ItemContainer {

    @NoEnglish
    public static final Item ELIXIR_OF_LIFE = new ElixirOfLifeItem(new AItemSettings().group(RegenerationModItemGroups.REGEN).maxCount(16));

	@NoEnglish
	public static final Item POCKET_WATCH = new PocketWatchItem(new AItemSettings().group(RegenerationModItemGroups.REGEN));
}
