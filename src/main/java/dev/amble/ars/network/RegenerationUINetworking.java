package dev.amble.ars.network;

import dev.amble.ars.api.RegenerationCapable;
import dev.amble.ars.core.RegenerationCore;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class RegenerationUINetworking {
    public static final Identifier OPEN_GUI_PACKET = new Identifier("timelordregen", "open_gui");
    public static final Identifier REQUEST_OPEN_GUI = new Identifier("timelordregen", "request_open_gui");
    public static final Identifier FORCE_REGEN = new Identifier("timelordregen", "force_regen");

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(REQUEST_OPEN_GUI, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> {
                if (!(player instanceof RegenerationCapable capable) || !capable.isTimelord()) {
                    return;
                }

                RegenerationCore info = capable.getRegenerationInfo();
                if (info != null) {
                    syncRegenInfoToClient(player, info);
                }

                sendOpenGuiPacket(player);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(FORCE_REGEN, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> handleForceRegen(player));
        });
    }

    private static void handleForceRegen(ServerPlayerEntity player) {
        RegenerationCore info = RegenerationCore.get(player);
        if (info == null || info.getUsesLeft() <= 0) return;

        if (info.isInvulnerable()) {
            player.sendMessage(Text.translatable("message.timelordregen.cannot_force_regen"), true);
            return;
        }

        if (!info.isActive()) {
            info.tryStart(player);
            syncRegenInfoToClient(player, info);
            return;
        }

        if (info.getDelay().isRunning()) {
            info.getDelay().stop();
            info.setRegenQueued(true);
            syncRegenInfoToClient(player, info);
        }
    }

    private static void syncRegenInfoToClient(ServerPlayerEntity player, RegenerationCore info) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(player.getUuid());
        buf.encodeAsJson(RegenerationCore.CODEC, info);
        ServerPlayNetworking.send(player, RegenerationCore.SYNC_PACKET, buf);
    }

    public static void sendOpenGuiPacket(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, OPEN_GUI_PACKET, PacketByteBufs.create());
    }
}