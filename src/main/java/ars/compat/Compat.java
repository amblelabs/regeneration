package ars.compat;

import ars.compat.ait.AITCompat;
import ars.compat.origin.OriginCompat;

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
