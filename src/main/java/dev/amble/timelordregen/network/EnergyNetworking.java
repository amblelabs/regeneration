package dev.amble.timelordregen.network;

import dev.amble.timelordregen.RegenerationMod;
import dev.amble.timelordregen.config.RegenerationServerConfig;
import dev.amble.timelordregen.core.energy.EnergyAbility;
import dev.amble.timelordregen.core.energy.RegenEnergy;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PlayerLookup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class EnergyNetworking {
    public static final Identifier INPUT = RegenerationMod.id("energy_input");
    public static final Identifier SYNC = RegenerationMod.id("energy_sync");
    public static final Identifier CHANNEL = RegenerationMod.id("energy_channel");
    public static final Identifier FX = RegenerationMod.id("energy_fx");
    public static final Identifier CHAIN = RegenerationMod.id("energy_chain");
    public static final Identifier RULES = RegenerationMod.id("energy_rules");

    private static final EnergyAbility[] ABILITIES = EnergyAbility.values();

    public static void registerServer() {
        ServerPlayNetworking.registerGlobalReceiver(INPUT, (server, player, handler, buf, responseSender) -> {
            int idx = buf.readByte();
            boolean pressed = buf.readBoolean();
            if (idx < 0 || idx >= ABILITIES.length) return;
            EnergyAbility ability = ABILITIES[idx];
            server.execute(() -> RegenEnergy.input(player, ability, pressed));
        });
    }

    public static void sendSync(ServerPlayerEntity player, int energy) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(energy);
        buf.writeBoolean(RegenEnergy.home(player.getWorld()));
        ServerPlayNetworking.send(player, SYNC, buf);
    }

    public static void sendRules(ServerPlayerEntity player) {
        RegenerationServerConfig c = RegenerationServerConfig.get();
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(c.chargeTicks);
        buf.writeFloat((float) c.beamRange);
        buf.writeVarInt(c.energyPerRegen);
        buf.writeVarInt(c.energyCap);
        ServerPlayNetworking.send(player, RULES, buf);
    }

    public static void sendChannel(ServerPlayerEntity caster, EnergyAbility ability, int phase, int targetId) {
        sendChannelTo(caster, caster, ability, phase, targetId);
        for (ServerPlayerEntity p : PlayerLookup.tracking(caster)) {
            sendChannelTo(p, caster, ability, phase, targetId);
        }
    }

    public static void sendChannelTo(ServerPlayerEntity viewer, ServerPlayerEntity caster, EnergyAbility ability, int phase, int targetId) {
        ServerPlayNetworking.send(viewer, CHANNEL, channelBuf(caster, ability, phase, targetId));
    }

    public static void sendFx(ServerWorld world, Vec3d pos, int type, int entityId, float a, float b) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeByte(type);
        buf.writeVarInt(entityId);
        buf.writeDouble(pos.x);
        buf.writeDouble(pos.y);
        buf.writeDouble(pos.z);
        buf.writeFloat(a);
        buf.writeFloat(b);
        for (ServerPlayerEntity p : PlayerLookup.around(world, pos, 96)) {
            ServerPlayNetworking.send(p, FX, PacketByteBufs.copy(buf));
        }
    }

    public static void sendChain(ServerWorld world, ServerPlayerEntity caster, List<? extends Entity> nodes, int hop) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(caster.getId());
        buf.writeVarInt(hop);
        buf.writeVarInt(nodes.size());
        for (Entity e : nodes) {
            Vec3d c = e.getBoundingBox().getCenter();
            buf.writeVarInt(e.getId());
            buf.writeDouble(c.x);
            buf.writeDouble(c.y);
            buf.writeDouble(c.z);
        }
        for (ServerPlayerEntity p : PlayerLookup.around(world, caster.getPos(), 128)) {
            ServerPlayNetworking.send(p, CHAIN, PacketByteBufs.copy(buf));
        }
    }

    private static PacketByteBuf channelBuf(ServerPlayerEntity caster, EnergyAbility ability, int phase, int targetId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(caster.getId());
        buf.writeByte(ability.ordinal());
        buf.writeByte(phase);
        buf.writeVarInt(targetId);
        return buf;
    }
}
