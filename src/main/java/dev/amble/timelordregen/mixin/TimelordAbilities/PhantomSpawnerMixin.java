package dev.amble.timelordregen.mixin.TimelordAbilities;

import dev.amble.timelordregen.api.RegenerationCapable;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.spawner.PhantomSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.List;

@Mixin(PhantomSpawner.class)
public class PhantomSpawnerMixin {

    @Redirect(
            method = "spawn(Lnet/minecraft/server/world/ServerWorld;ZZ)I",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/server/world/ServerWorld;getPlayers()Ljava/util/List;"
            )
    )
    private List<ServerPlayerEntity> timelord$filterPhantomTargets(ServerWorld world) {
        List<ServerPlayerEntity> players = world.getPlayers();
        List<ServerPlayerEntity> filtered = new ArrayList<>(players.size());
        for (ServerPlayerEntity player : players) {
            if (!(player instanceof RegenerationCapable capable && capable.isTimelord())) {
                filtered.add(player);
            }
        }
        return filtered;
    }
}