package dev.amble.timelordregen.config;

import dev.amble.timelordregen.RegenerationMod;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public class RegenerationServerConfig {
    public static final ConfigClassHandler<RegenerationServerConfig> INSTANCE = ConfigClassHandler.createBuilder(RegenerationServerConfig.class)
            .id(RegenerationMod.id("server"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(FabricLoader.getInstance().getConfigDir().resolve("timelordregen-server.json5"))
                    .setJson5(true)
                    .build())
            .build();

    @SerialEntry public int energyPerRegen = 100;
    @SerialEntry public int energyCap = 199;
    @SerialEntry public boolean tardisSafe = true;
    @SerialEntry public boolean tardisRestore = true;
    @SerialEntry public int tardisRestoreTicks = 10;
    @SerialEntry public int tardisRestoreAmount = 1;
    @SerialEntry public double tardisFuelPerEnergy = 25.0;

    @SerialEntry public int chargeTicks = 10;
    @SerialEntry public int beamDrain = 1;
    @SerialEntry public double beamRange = 24.0;
    @SerialEntry public double beamRadius = 1.5;
    @SerialEntry public double beamSpread = 0.08;
    @SerialEntry public float beamDamage = 3.0f;
    @SerialEntry public float impactDamage = 8.0f;
    @SerialEntry public double impactRadius = 3.5;
    @SerialEntry public int impactTicks = 5;
    @SerialEntry public float releaseDamage = 10.0f;
    @SerialEntry public double releaseRadius = 6.0;
    @SerialEntry public float knockback = 1.0f;
    @SerialEntry public boolean ignoresArmor = false;
    @SerialEntry public float playerDamage = 0.5f;
    @SerialEntry public boolean burnLoot = true;

    @SerialEntry public int maxBreaks = 16;
    @SerialEntry public float maxHardness = 30.0f;
    @SerialEntry public double beamBreakRadius = 2.0;
    @SerialEntry public float beamBreakPower = 1.0f;
    @SerialEntry public double impactBreakRadius = 3.0;
    @SerialEntry public float impactBreakPower = 3.0f;
    @SerialEntry public double releaseBreakRadius = 4.5;
    @SerialEntry public float releaseBreakHardness = 1.0f;
    @SerialEntry public int fires = 4;

    @SerialEntry public boolean chainEnabled = true;
    @SerialEntry public double chainRange = 24.0;
    @SerialEntry public double chainRadius = 20.0;
    @SerialEntry public double chainHop = 8.0;
    @SerialEntry public int chainTargets = 8;
    @SerialEntry public int chainCost = 10;
    @SerialEntry public int chainHopTicks = 2;
    @SerialEntry public float chainDamage = 12.0f;
    @SerialEntry public float chainSplashDamage = 4.0f;
    @SerialEntry public double chainSplashRadius = 2.0;
    @SerialEntry public double chainCraterRadius = 1.5;
    @SerialEntry public float chainCraterHardness = 1.5f;
    @SerialEntry public int chainCooldown = 40;
    @SerialEntry public double chainAimAssist = 1.0;

    @SerialEntry public int healTicks = 10;
    @SerialEntry public int healCost = 5;
    @SerialEntry public float healAmount = 1.0f;
    @SerialEntry public float maxAbsorption = 4.0f;

    @SerialEntry public int transferTicks = 5;
    @SerialEntry public int transferAmount = 5;
    @SerialEntry public float transferEfficiency = 0.8f;
    @SerialEntry public double transferRange = 6.0;
    @SerialEntry public float transferHeal = 1.0f;
    @SerialEntry public boolean transferGrantsRegens = true;

    public static RegenerationServerConfig get() {
        return INSTANCE.instance();
    }

    public static RegenerationServerConfig trenzalore() {
        RegenerationServerConfig c = new RegenerationServerConfig();
        c.tardisRestoreTicks = 4;
        c.tardisFuelPerEnergy = 0.0;
        c.beamRange = 32.0;
        c.beamRadius = 2.2;
        c.beamSpread = 0.12;
        c.beamDamage = 30.0f;
        c.impactDamage = 60.0f;
        c.impactRadius = 6.0;
        c.releaseDamage = 40.0f;
        c.releaseRadius = 10.0;
        c.ignoresArmor = true;
        c.playerDamage = 1.0f;
        c.maxBreaks = 96;
        c.maxHardness = 100.0f;
        c.beamBreakRadius = 4.0;
        c.beamBreakPower = 2.0f;
        c.impactBreakRadius = 6.0;
        c.impactBreakPower = 8.0f;
        c.releaseBreakRadius = 8.5;
        c.fires = 16;
        c.chainRange = 30.0;
        c.chainHop = 20.0;
        c.chainTargets = 40;
        c.chainCost = 5;
        c.chainDamage = 100.0f;
        c.chainSplashDamage = 40.0f;
        c.chainSplashRadius = 3.5;
        c.chainCraterRadius = 2.6;
        c.chainCraterHardness = 6.0f;
        c.chainCooldown = 0;
        c.healTicks = 4;
        c.healAmount = 2.0f;
        c.maxAbsorption = 8.0f;
        c.transferEfficiency = 1.0f;
        return c;
    }

    public void copy(RegenerationServerConfig from) {
        try {
            for (Field f : RegenerationServerConfig.class.getFields()) {
                if (!Modifier.isStatic(f.getModifiers())) f.set(this, f.get(from));
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
