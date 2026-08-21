package dev.amble.timelordregen.compat;

import net.fabricmc.loader.api.FabricLoader;

public class DependencyChecker {
	public static final boolean HAS_AIT = doesModExist("ait");
	public static final boolean HAS_ORIGIN = doesModExist("origins");

	public static boolean doesModExist(String modid) {
		return FabricLoader.getInstance().isModLoaded(modid);
	}
}
