package dev.amble.ars.compat;

import dev.amble.ars.compat.ait.AITCompat;
import dev.amble.ars.compat.origin.OriginCompat;

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
