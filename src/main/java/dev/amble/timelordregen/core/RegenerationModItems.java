package dev.amble.timelordregen.core;

import dev.amble.lib.container.impl.ItemContainer;
import dev.amble.lib.datagen.util.AutomaticModel;
import dev.amble.lib.datagen.util.NoEnglish;
import dev.amble.lib.item.AItemSettings;
import dev.amble.timelordregen.api.boat.ABoatItem;
import dev.amble.timelordregen.core.item.ElixirOfLifeItem;
import dev.amble.timelordregen.core.item.PocketWatchItem;
import net.minecraft.item.Item;

public class RegenerationModItems extends ItemContainer {

    @NoEnglish
    public static final Item ELIXIR_OF_LIFE = new ElixirOfLifeItem(new AItemSettings().group(RegenerationModItemGroups.REGEN).maxCount(16));


	@NoEnglish
	public static final Item POCKET_WATCH = new PocketWatchItem(new AItemSettings().group(RegenerationModItemGroups.REGEN));

/// Disabled cus they crash when placed. idk why and neither does theo - ADDIE

    //@AutomaticModel
   // @NoEnglish
    //public static final Item CADON_BOAT = new ABoatItem(false, RegenerationModBoatTypes.CADON, new AItemSettings());

    //@AutomaticModel
    //@NoEnglish
    //public static final Item CADON_CHEST_BOAT = new ABoatItem(true, RegenerationModBoatTypes.CADON, new AItemSettings());
}
