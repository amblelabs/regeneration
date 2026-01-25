package dev.amble.timelordregen.core;

import dev.amble.timelordregen.api.boat.ABoatType;
import dev.amble.timelordregen.api.boat.BoatTypeContainer;

public class RegenerationModBoatTypes extends BoatTypeContainer {

    public static final ABoatType CADON = register(RegenerationModItems.CADON_BOAT, RegenerationModItems.CADON_CHEST_BOAT, RegenerationModBlocks.CADON_PLANKS);
}
