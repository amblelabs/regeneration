package ars.core.particle_effects;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import ars.RegenerationMod;
import net.minecraft.entity.Entity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;
import net.minecraft.world.World;

public class RegenParticleEffect implements ParticleEffect {
    private final int entityId;
    private final float yawOffset;
    private final float pitchOffset;
    private final boolean shouldPitch;
    private final boolean shouldFollowPlayer;
    private final float speed;
    private final boolean shortLife;

    public RegenParticleEffect(int entityId, float yawOffset, float pitchOffset, boolean shouldPitch, boolean shouldFollowPlayer, float speed, boolean shortLife) {
        this.entityId = entityId;
        this.yawOffset = yawOffset;
        this.pitchOffset = pitchOffset;
        this.shouldPitch = shouldPitch;
        this.shouldFollowPlayer = shouldFollowPlayer;
        this.speed = speed;
        this.shortLife = shortLife;
    }

    // 旧构造函数兼容
    public RegenParticleEffect(int entityId, float yawOffset, float pitchOffset, boolean shouldPitch, boolean shouldFollowPlayer, float speed) {
        this(entityId, yawOffset, pitchOffset, shouldPitch, shouldFollowPlayer, speed, false);
    }

    public RegenParticleEffect() {
        this.entityId = -1;
        this.yawOffset = 0;
        this.pitchOffset = 0;
        this.shouldPitch = true;
        this.shouldFollowPlayer = true;
        this.speed = 0.4f;
        this.shortLife = false;
    }

    // ★ 显式手写所有 getter，不依赖 Lombok
    public int getEntityId() { return entityId; }
    public float getYawOffset() { return yawOffset; }
    public float getPitchOffset() { return pitchOffset; }
    public boolean getShouldPitch() { return shouldPitch; }
    public boolean getShouldFollowPlayer() { return shouldFollowPlayer; }
    public float getSpeed() { return speed; }
    public boolean isShortLife() { return shortLife; }

    public static final Factory<RegenParticleEffect> PARAMETERS_FACTORY = new Factory<>() {
        @Override
        public RegenParticleEffect read(ParticleType<RegenParticleEffect> particleType, StringReader stringReader) throws CommandSyntaxException {
            int entityId = stringReader.readInt();
            float yawOffset = stringReader.readFloat();
            float pitchOffset = stringReader.readFloat();
            boolean shouldPitch = stringReader.readBoolean();
            boolean shouldFollowPlayer = stringReader.readBoolean();
            float speed = stringReader.readFloat();
            boolean shortLife = stringReader.readBoolean();
            return new RegenParticleEffect(entityId, yawOffset, pitchOffset, shouldPitch, shouldFollowPlayer, speed, shortLife);
        }

        @Override
        public RegenParticleEffect read(ParticleType<RegenParticleEffect> particleType, PacketByteBuf packetByteBuf) {
            int entityId = packetByteBuf.readInt();
            float yawOffset = packetByteBuf.readFloat();
            float pitchOffset = packetByteBuf.readFloat();
            boolean shouldPitch = packetByteBuf.readBoolean();
            boolean shouldFollowPlayer = packetByteBuf.readBoolean();
            float speed = packetByteBuf.readFloat();
            boolean shortLife = packetByteBuf.readBoolean();
            return new RegenParticleEffect(entityId, yawOffset, pitchOffset, shouldPitch, shouldFollowPlayer, speed, shortLife);
        }
    };

    @Override
    public void write(PacketByteBuf buf) {
        buf.writeInt(entityId);
        buf.writeFloat(yawOffset);
        buf.writeFloat(pitchOffset);
        buf.writeBoolean(shouldPitch);
        buf.writeBoolean(shouldFollowPlayer);
        buf.writeFloat(speed);
        buf.writeBoolean(shortLife);
    }

    @Override
    public String asString() {
        return entityId + " " + yawOffset + " " + pitchOffset + " " + shouldPitch + " " + shouldFollowPlayer + " " + speed + " " + shortLife;
    }

    public Entity getEntity(World world) {
        return world.getEntityById(this.entityId);
    }

    public ParticleType<RegenParticleEffect> getType() {
        return RegenerationMod.RIGHT_REGEN_PARTICLE;
    }
}