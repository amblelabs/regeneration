package dev.amble.timelordregen.client.energy;

import dev.amble.timelordregen.client.config.RegenerationClientConfig;
import dev.amble.timelordregen.client.sound.LoopingSound;
import dev.amble.timelordregen.core.energy.EnergyAbility;
import dev.amble.timelordregen.core.energy.EnergyFxType;
import dev.amble.timelordregen.data.tree.RegenerationSounds;
import dev.amble.timelordregen.network.EnergyNetworking;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

@Environment(EnvType.CLIENT)
public final class EnergyClient {
    public static final Int2ObjectOpenHashMap<ChannelView> CHANNELS = new Int2ObjectOpenHashMap<>();
    private static final Int2ObjectOpenHashMap<Loop[]> LOOPS = new Int2ObjectOpenHashMap<>();
    private static final EnergyAbility[] ABILITIES = EnergyAbility.values();

    public static int energy, perRegen = 100, cap = 199;
    static int chargeTicks = 10;
    static float range = 24.0f;
    static boolean home;

    private static KeyBinding blast, heal, crack;
    private static boolean blastDown, healDown;
    private static long ticks, changed = -1000;

    public static void init() {
        RegenerationClientConfig.INSTANCE.load();
        blast = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.timelordregen.energy_blast", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_Z, "category.timelordregen"));
        heal = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.timelordregen.energy_heal", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, "category.timelordregen"));
        crack = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.timelordregen.energy_crack", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "category.timelordregen"));

        ClientTickEvents.END_CLIENT_TICK.register(EnergyClient::tick);

        ClientPlayNetworking.registerGlobalReceiver(EnergyNetworking.SYNC, (client, handler, buf, sender) -> {
            int v = buf.readVarInt();
            boolean s = buf.readBoolean();
            client.execute(() -> {
                if (v != energy) changed = ticks;
                energy = v;
                home = s;
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(EnergyNetworking.RULES, (client, handler, buf, sender) -> {
            int ct = buf.readVarInt();
            float r = buf.readFloat();
            int per = buf.readVarInt();
            int c = buf.readVarInt();
            client.execute(() -> {
                chargeTicks = Math.max(1, ct);
                range = r;
                perRegen = Math.max(1, per);
                cap = Math.max(1, c);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(EnergyNetworking.CHANNEL, (client, handler, buf, sender) -> {
            int id = buf.readVarInt();
            int a = buf.readByte();
            int phase = buf.readByte();
            int target = buf.readVarInt();
            client.execute(() -> channel(client, id, a, phase, target));
        });

        ClientPlayNetworking.registerGlobalReceiver(EnergyNetworking.FX, (client, handler, buf, sender) -> {
            int type = buf.readByte();
            int id = buf.readVarInt();
            Vec3d pos = new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
            float a = buf.readFloat();
            float b = buf.readFloat();
            client.execute(() -> {
                if (client.world != null) EnergyFx.play(client.world, type, id, pos, a, b);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(EnergyNetworking.CHAIN, (client, handler, buf, sender) -> {
            int caster = buf.readVarInt();
            int hop = buf.readVarInt();
            int n = Math.min(buf.readVarInt(), 256);
            int[] ids = new int[n];
            double[] pos = new double[n * 3];
            for (int i = 0; i < n; i++) {
                ids[i] = buf.readVarInt();
                pos[i * 3] = buf.readDouble();
                pos[i * 3 + 1] = buf.readDouble();
                pos[i * 3 + 2] = buf.readDouble();
            }
            client.execute(() -> {
                if (client.world != null && n > 0) EnergyBeamRenderer.chain(caster, Math.max(1, hop), ids, pos, client.world.getTime());
            });
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> reset(client)));

        HudRenderCallback.EVENT.register(EnergyHud::render);
        EnergyBeamRenderer.register();
    }

    public static ChannelView local() {
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        return p == null ? null : CHANNELS.get(p.getId());
    }

    static boolean held() {
        return blastDown || healDown;
    }

    static boolean recentlyChanged() {
        return ticks - changed < 60;
    }

    public static void input(EnergyAbility a, boolean pressed) {
        if (!ClientPlayNetworking.canSend(EnergyNetworking.INPUT)) return;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeByte(a.ordinal());
        buf.writeBoolean(pressed);
        ClientPlayNetworking.send(EnergyNetworking.INPUT, buf);
    }

    private static void tick(MinecraftClient client) {
        ticks++;
        EnergyFx.tickBudget();
        if (!client.isPaused()) EnergyMotes.tick();
        ClientPlayerEntity p = client.player;
        boolean free = p != null && client.currentScreen == null && p.isAlive();

        blastDown = edge(EnergyAbility.BLAST, free && blast.isPressed(), blastDown);
        healDown = edge(EnergyAbility.HEAL, free && heal.isPressed(), healDown);
        while (crack.wasPressed()) {
            if (free) input(EnergyAbility.CRACK, true);
        }

        ClientWorld world = client.world;
        if (world == null) return;

        ObjectIterator<Int2ObjectMap.Entry<ChannelView>> it = CHANNELS.int2ObjectEntrySet().fastIterator();
        while (it.hasNext()) {
            Int2ObjectMap.Entry<ChannelView> e = it.next();
            int id = e.getIntKey();
            ChannelView v = e.getValue();
            Entity ent = world.getEntityById(id);
            if (!(ent instanceof PlayerEntity caster) || ent.isRemoved()) {
                stopLoops(client, id);
                it.remove();
                continue;
            }
            if (!LOOPS.containsKey(id) && (v.ability != EnergyAbility.BLAST || v.phase == EnergyFxType.STREAM))
                LOOPS.put(id, startLoops(client, caster, v.ability));
            EnergyFx.tickChannel(world, caster, v);
        }
        if (!client.isPaused()) EnergyFx.tickChains(world);
    }

    private static boolean edge(EnergyAbility a, boolean now, boolean was) {
        if (now != was) {
            if (now && a == EnergyAbility.BLAST && MinecraftClient.getInstance().player != null) MinecraftClient.getInstance().player.setSprinting(false);
            input(a, now);
        }
        return now;
    }

    private static void channel(MinecraftClient client, int id, int a, int phase, int target) {
        ClientWorld world = client.world;
        if (world == null || a < 0 || a >= ABILITIES.length) return;

        if (phase == EnergyFxType.STOP) {
            stopLoops(client, id);
            CHANNELS.remove(id);
            return;
        }

        EnergyAbility ability = ABILITIES[a];
        long now = world.getTime();
        ChannelView v = CHANNELS.get(id);
        if (v != null && v.ability != ability) {
            stopLoops(client, id);
            v = null;
        }
        if (v == null) {
            v = new ChannelView(ability, now);
            CHANNELS.put(id, v);
        }
        v.targetId = target;
        if (v.phase == phase) return;
        v.phase = phase;
        v.phaseTick = now;

        Entity e = world.getEntityById(id);
        if (e != null && phase == EnergyFxType.CHARGE) {
            world.playSound(e.getX(), e.getBodyY(0.5), e.getZ(), SoundEvents.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 0.8f, 1.3f, false);
            world.playSound(e.getX(), e.getBodyY(0.5), e.getZ(), SoundEvents.BLOCK_BEACON_ACTIVATE, SoundCategory.PLAYERS, 0.7f, 1.2f, false);
        }
    }

    private static Loop[] startLoops(MinecraftClient client, Entity e, EnergyAbility a) {
        Loop[] l = a != EnergyAbility.BLAST
                ? new Loop[]{new Loop(e, SoundEvents.BLOCK_BEACON_AMBIENT, 0.35f, 1.4f)}
                : e == client.player
                ? new Loop[]{new Loop(e, RegenerationSounds.ELEVEN_REGEN_LOOP, 2.0f, 1.0f), new Loop(e, SoundEvents.BLOCK_BEACON_AMBIENT, 1.2f, 0.55f), new Loop(e, SoundEvents.BLOCK_FIRE_AMBIENT, 0.8f, 0.7f)}
                : new Loop[]{new Loop(e, SoundEvents.BLOCK_BEACON_AMBIENT, 1.2f, 0.55f), new Loop(e, SoundEvents.BLOCK_FIRE_AMBIENT, 0.8f, 0.7f)};
        for (Loop s : l) client.getSoundManager().play(s);
        return l;
    }

    private static void stopLoops(MinecraftClient client, int id) {
        Loop[] l = LOOPS.remove(id);
        if (l == null) return;
        for (Loop s : l) client.getSoundManager().stop(s);
    }

    private static void reset(MinecraftClient client) {
        for (Loop[] l : LOOPS.values())
            for (Loop s : l) client.getSoundManager().stop(s);
        LOOPS.clear();
        CHANNELS.clear();
        EnergyBeamRenderer.CHAINS.clear();
        EnergyMotes.clear();
        energy = 0;
        home = false;
        changed = -1000;
        blastDown = false;
        healDown = false;
    }

    public static final class ChannelView {
        public final EnergyAbility ability;
        public int phase;
        public int targetId = -1;
        public long startTick, phaseTick, spawned = Long.MIN_VALUE;

        ChannelView(EnergyAbility ability, long now) {
            this.ability = ability;
            this.startTick = now;
            this.phaseTick = now;
        }
    }

    private static final class Loop extends LoopingSound {
        private final Entity entity;

        Loop(Entity entity, SoundEvent event, float volume, float pitch) {
            super(event, SoundCategory.PLAYERS);
            this.entity = entity;
            this.setVolume(volume);
            this.setPitch(pitch);
            this.follow();
        }

        @Override
        public void tick() {
            if (this.entity.isRemoved()) {
                this.setDone();
                return;
            }
            this.follow();
        }

        private void follow() {
            this.x = this.entity.getX();
            this.y = this.entity.getBodyY(0.5);
            this.z = this.entity.getZ();
        }
    }
}
