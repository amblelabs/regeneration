package util;

import dev.amble.lib.util.LazyObject;
import net.minecraft.entity.vehicle.BoatEntity;

public interface ExBoatType {

    LazyObject<BoatEntity.Type> CADON = new LazyObject<>(() -> BoatEntity.Type.getType("cadon"));
}
