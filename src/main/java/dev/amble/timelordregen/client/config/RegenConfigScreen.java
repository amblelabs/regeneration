package dev.amble.timelordregen.client.config;

import dev.amble.timelordregen.config.RegenerationServerConfig;
import dev.amble.timelordregen.core.energy.RegenEnergy;
import dev.isxander.yacl3.api.ButtonOption;
import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.LabelOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.DoubleFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.FloatFieldControllerBuilder;
import dev.isxander.yacl3.api.controller.FloatSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerFieldControllerBuilder;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.text.Text;

import java.lang.reflect.Field;
import java.util.Locale;

@Environment(EnvType.CLIENT)
public final class RegenConfigScreen {
    private static final String KEY = "config.timelordregen.";

    public static Screen create(Screen parent) {
        RegenerationServerConfig s = RegenerationServerConfig.get();
        RegenerationServerConfig sd = RegenerationServerConfig.INSTANCE.defaults();
        RegenerationClientConfig c = RegenerationClientConfig.get();
        RegenerationClientConfig cd = RegenerationClientConfig.INSTANCE.defaults();
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean local = mc.world == null || mc.isIntegratedServerRunning();

        ConfigCategory.Builder energy = ConfigCategory.createBuilder().name(text("category.energy"));
        if (!local) energy.option(LabelOption.create(text("remote")));
        energy.group(OptionGroup.createBuilder().name(text("group.presets")).description(desc("group.presets"))
                .option(preset(parent, "balanced", new RegenerationServerConfig(), local))
                .option(preset(parent, "trenzalore", RegenerationServerConfig.trenzalore(), local))
                .build());
        energy.group(group("reserve", s, sd, local, new Object[][]{
                {"energyPerRegen", 1},
                {"energyCap", 1}}));
        energy.group(group("tardis", s, sd, local, new Object[][]{
                {"tardisSafe"},
                {"tardisRestore"},
                {"tardisRestoreTicks", 1},
                {"tardisRestoreAmount", 1},
                {"tardisFuelPerEnergy", 0.0}}));

        ConfigCategory.Builder blast = ConfigCategory.createBuilder().name(text("category.blast"));
        if (!local) blast.option(LabelOption.create(text("remote")));
        blast.group(group("beam", s, sd, local, new Object[][]{
                {"chargeTicks", 0},
                {"beamDrain", 0},
                {"beamRange", 1.0},
                {"beamRadius", 0.0},
                {"beamSpread", 0.0},
                {"beamDamage", 0.0f},
                {"knockback", 0.0f},
                {"ignoresArmor"},
                {"playerDamage", 0.0f},
                {"burnLoot"}}));
        blast.group(group("impact", s, sd, local, new Object[][]{
                {"impactDamage", 0.0f},
                {"impactRadius", 0.0},
                {"impactTicks", 1},
                {"releaseDamage", 0.0f},
                {"releaseRadius", 0.0}}));
        blast.group(group("blocks", s, sd, local, new Object[][]{
                {"maxBreaks", 0},
                {"maxHardness", 0.0f},
                {"beamBreakRadius", 0.0},
                {"beamBreakPower", 0.0f},
                {"impactBreakRadius", 0.0},
                {"impactBreakPower", 0.0f},
                {"releaseBreakRadius", 0.0},
                {"releaseBreakHardness", 0.0f},
                {"fires", 0}}));

        ConfigCategory.Builder chain = ConfigCategory.createBuilder().name(text("category.chain"));
        if (!local) chain.option(LabelOption.create(text("remote")));
        chain.group(group("chain", s, sd, local, new Object[][]{
                {"chainEnabled"},
                {"chainRange", 1.0},
                {"chainAimAssist", 0.0},
                {"chainRadius", 0.0},
                {"chainHop", 0.0},
                {"chainTargets", 1},
                {"chainCost", 0},
                {"chainHopTicks", 1},
                {"chainCooldown", 0}}));
        chain.group(group("chain_hit", s, sd, local, new Object[][]{
                {"chainDamage", 0.0f},
                {"chainSplashDamage", 0.0f},
                {"chainSplashRadius", 0.0},
                {"chainCraterRadius", 0.0},
                {"chainCraterHardness", 0.0f}}));

        ConfigCategory.Builder support = ConfigCategory.createBuilder().name(text("category.support"));
        if (!local) support.option(LabelOption.create(text("remote")));
        support.group(group("heal", s, sd, local, new Object[][]{
                {"healTicks", 1},
                {"healCost", 0},
                {"healAmount", 0.0f},
                {"maxAbsorption", 0.0f}}));
        support.group(group("transfer", s, sd, local, new Object[][]{
                {"transferTicks", 1},
                {"transferAmount", 1},
                {"transferEfficiency", 0.0f},
                {"transferRange", 1.0},
                {"transferHeal", 0.0f},
                {"transferGrantsRegens"}}));

        ConfigCategory.Builder visuals = ConfigCategory.createBuilder().name(text("category.visuals"));
        visuals.group(group("visuals", c, cd, true, new Object[][]{
                {"particles", 0.0f, 5.0f, 0.05f},
                {"beamScale", 0.1f, 5.0f, 0.05f},
                {"shake", 0.0f, 5.0f, 0.05f},
                {"vignette"},
                {"glare"},
                {"handGlow"},
                {"hud"}}));

        return YetAnotherConfigLib.createBuilder()
                .title(text("title"))
                .category(visuals.build())
                .category(energy.build())
                .category(blast.build())
                .category(chain.build())
                .category(support.build())
                .save(() -> {
                    RegenerationClientConfig.INSTANCE.save();
                    if (local) {
                        RegenerationServerConfig.INSTANCE.save();
                        resync();
                    }
                })
                .build()
                .generateScreen(parent);
    }

    private static ButtonOption preset(Screen parent, String name, RegenerationServerConfig values, boolean on) {
        return ButtonOption.createBuilder()
                .name(text("preset." + name))
                .description(desc("preset." + name))
                .text(text("preset.apply"))
                .available(on)
                .action(screen -> {
                    RegenerationServerConfig.get().copy(values);
                    RegenerationServerConfig.INSTANCE.save();
                    resync();
                    MinecraftClient.getInstance().setScreen(create(parent));
                })
                .build();
    }

    private static void resync() {
        IntegratedServer server = MinecraftClient.getInstance().getServer();
        if (server != null) server.execute(() -> RegenEnergy.syncRules(server));
    }

    private static OptionGroup group(String name, Object cfg, Object def, boolean on, Object[][] rows) {
        OptionGroup.Builder g = OptionGroup.createBuilder().name(text("group." + name));
        for (Object[] row : rows) g.option(option(cfg, def, (String) row[0], row, on));
        return g.build();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Option<?> option(Object cfg, Object def, String name, Object[] row, boolean on) {
        Field f;
        try {
            f = cfg.getClass().getField(name);
        } catch (NoSuchFieldException e) {
            throw new IllegalArgumentException(name, e);
        }
        Class<?> t = f.getType();
        Text label = text(name);
        OptionDescription d = desc(name);
        boolean slider = row.length == 4;
        if (t == int.class) {
            return Option.<Integer>createBuilder().name(label).description(d).available(on)
                    .binding((Integer) read(f, def), () -> (Integer) read(f, cfg), v -> write(f, cfg, v))
                    .controller(o -> IntegerFieldControllerBuilder.create(o).min((Integer) row[1]))
                    .build();
        }
        if (t == float.class) {
            return Option.<Float>createBuilder().name(label).description(d).available(on)
                    .binding((Float) read(f, def), () -> (Float) read(f, cfg), v -> write(f, cfg, v))
                    .controller(o -> slider ? FloatSliderControllerBuilder.create(o).range((Float) row[1], (Float) row[2]).step((Float) row[3])
                            .formatValue(v -> Text.literal(String.format(Locale.ROOT, "%.2f", v)))
                            : FloatFieldControllerBuilder.create(o).min((Float) row[1]))
                    .build();
        }
        if (t == double.class) {
            return Option.<Double>createBuilder().name(label).description(d).available(on)
                    .binding((Double) read(f, def), () -> (Double) read(f, cfg), v -> write(f, cfg, v))
                    .controller(o -> DoubleFieldControllerBuilder.create(o).min((Double) row[1]))
                    .build();
        }
        if (t == boolean.class) {
            return Option.<Boolean>createBuilder().name(label).description(d).available(on)
                    .binding((Boolean) read(f, def), () -> (Boolean) read(f, cfg), v -> write(f, cfg, v))
                    .controller(o -> BooleanControllerBuilder.create(o).coloured(true))
                    .build();
        }
        return Option.<Enum>createBuilder().name(label).description(d).available(on)
                .binding((Enum) read(f, def), () -> (Enum) read(f, cfg), v -> write(f, cfg, v))
                .controller(o -> EnumControllerBuilder.create(o).enumClass((Class) t)
                        .formatValue(v -> text(name + "." + ((Enum<?>) v).name().toLowerCase(Locale.ROOT))))
                .build();
    }

    private static Object read(Field f, Object o) {
        try {
            return f.get(o);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void write(Field f, Object o, Object v) {
        try {
            f.set(o, v);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Text text(String key) {
        return Text.translatable(KEY + key);
    }

    private static OptionDescription desc(String key) {
        return OptionDescription.of(Text.translatable(KEY + key + ".desc"));
    }
}
