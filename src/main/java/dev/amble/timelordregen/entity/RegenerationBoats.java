package dev.amble.timelordregen.entity;

import com.terraformersmc.terraform.boat.api.TerraformBoatType;
import com.terraformersmc.terraform.boat.api.TerraformBoatTypeRegistry;
import dev.amble.timelordregen.RegenerationMod;
import dev.amble.timelordregen.core.RegenerationModBlocks;
import dev.amble.timelordregen.core.RegenerationModItems;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.util.Identifier;

//public class RegenerationBoats {
//
//    public static final Identifier CADON_BOAT_ID = new Identifier(RegenerationMod.MOD_ID,"cadon_boat");
//    public static final Identifier CADON_CHEST_BOAT_ID = new Identifier(RegenerationMod.MOD_ID,"cadon_chest_boat");
//
//    public static final RegistryKey<TerraformBoatType> CADON_BOAT_KEY = TerraformBoatTypeRegistry.createKey(CADON_BOAT_ID);
//    public static final RegistryKey<TerraformBoatType> CADON_CHEST_BOAT_KEY = TerraformBoatTypeRegistry.createKey(CADON_CHEST_BOAT_ID);
//
//    public static void registerBoats() {
//        TerraformBoatType cadonBoat = new TerraformBoatType.Builder()
//                .item(RegenerationModItems.CADON_BOAT)
//                .chestItem(RegenerationModItems.CADON_CHEST_BOAT)
//                .planks(RegenerationModBlocks.CADON_PLANKS.asItem())
//                .build();
//
//        Registry.register(TerraformBoatTypeRegistry.INSTANCE, CADON_BOAT_KEY, cadonBoat);
//    }
//}
