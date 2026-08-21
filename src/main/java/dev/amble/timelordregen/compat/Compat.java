package dev.amble.timelordregen.compat;

import dev.amble.timelordregen.compat.ait.AITCompat;
import dev.amble.timelordregen.compat.origin.OriginCompat;

public class Compat {
	public static void init() {
		if (DependencyChecker.HAS_AIT) {
			AITCompat.init();
		}

		if (DependencyChecker.HAS_ORIGIN) {
			 OriginCompat.init();
		}
	}
}
