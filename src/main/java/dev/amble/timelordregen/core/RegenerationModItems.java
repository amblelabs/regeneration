package dev.amble.timelordregen.core;

import com.terraformersmc.terraform.boat.api.item.TerraformBoatItemHelper;
import com.terraformersmc.terraform.boat.impl.item.TerraformBoatItem;
import dev.amble.lib.container.impl.ItemContainer;
import dev.amble.lib.datagen.util.AutomaticModel;
import dev.amble.lib.datagen.util.NoEnglish;
import dev.amble.lib.item.AItemSettings;
import dev.amble.timelordregen.core.item.ElixirOfLifeItem;
import dev.amble.timelordregen.core.item.PocketWatchItem;
import dev.amble.timelordregen.entity.RegenerationBoats;
import net.minecraft.item.Item;

public class RegenerationModItems extends ItemContainer {

    @NoEnglish
    public static final Item ELIXIR_OF_LIFE = new ElixirOfLifeItem(new AItemSettings().group(RegenerationModItemGroups.REGEN).maxCount(16));


	@NoEnglish
	public static final Item POCKET_WATCH = new PocketWatchItem(new AItemSettings().group(RegenerationModItemGroups.REGEN));



    @NoEnglish
    @AutomaticModel
    public static final Item CADON_BOAT = TerraformBoatItemHelper.registerBoatItem(RegenerationBoats.CADON_BOAT_ID, RegenerationBoats.CADON_BOAT_KEY, false);

    @NoEnglish
    @AutomaticModel
    public static final Item CADON_CHEST_BOAT = TerraformBoatItemHelper.registerBoatItem(RegenerationBoats.CADON_CHEST_BOAT_ID, RegenerationBoats.CADON_BOAT_KEY, true);



}
