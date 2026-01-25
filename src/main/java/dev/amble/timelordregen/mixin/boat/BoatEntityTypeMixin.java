package dev.amble.timelordregen.mixin.boat;

import dev.amble.timelordregen.api.boat.BoatTypeRegistry;
import net.minecraft.block.Block;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.Items;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.function.ValueLists;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;
import java.util.function.IntFunction;

@Mixin(BoatEntity.Type.class)
public class BoatEntityTypeMixin implements BoatTypeRegistry.CustomBoatTypes {

    @Mutable @Shadow @Final private static BoatEntity.Type[] field_7724;
    @Mutable @Shadow @Final public static StringIdentifiable.Codec<BoatEntity.Type> CODEC;
    @Mutable @Shadow @Final private static IntFunction<BoatEntity.Type> BY_ID;

    @Invoker("<init>")
    public static BoatEntity.Type stargate$init(String internalName, int ordinal, Block baseBlock, String name) {
        throw new AssertionError();
    }

    @Unique
    private static BoatEntity.Type stargate$init(BoatTypeRegistry.Entry entry, int offset) {
        return stargate$init(entry.getEnumName(),
                field_7724.length + offset,
                entry.block(), entry.getPath()
        );
    }

    @Override
    public void amble$recalc(List<BoatTypeRegistry.Entry> entries, BoatTypeRegistry.BoatItemConsumer consumer) {
        int l = field_7724.length;

        BoatEntity.Type[] variants = new BoatEntity.Type[l + entries.size()];
        System.arraycopy(field_7724, 0, variants, 0, l);

        consumer.init(variants.length);
        consumer.accept(BoatEntity.Type.OAK, Items.OAK_BOAT, Items.OAK_CHEST_BOAT);
        consumer.accept(BoatEntity.Type.SPRUCE, Items.SPRUCE_BOAT, Items.SPRUCE_CHEST_BOAT);
        consumer.accept(BoatEntity.Type.BIRCH, Items.BIRCH_BOAT, Items.BIRCH_CHEST_BOAT);
        consumer.accept(BoatEntity.Type.JUNGLE, Items.JUNGLE_BOAT, Items.JUNGLE_CHEST_BOAT);
        consumer.accept(BoatEntity.Type.ACACIA, Items.ACACIA_BOAT, Items.ACACIA_CHEST_BOAT);
        consumer.accept(BoatEntity.Type.CHERRY, Items.CHERRY_BOAT, Items.CHERRY_CHEST_BOAT);
        consumer.accept(BoatEntity.Type.DARK_OAK, Items.DARK_OAK_BOAT, Items.DARK_OAK_CHEST_BOAT);
        consumer.accept(BoatEntity.Type.MANGROVE, Items.MANGROVE_BOAT, Items.MANGROVE_CHEST_BOAT);
        consumer.accept(BoatEntity.Type.BAMBOO, Items.BAMBOO_RAFT, Items.BAMBOO_CHEST_RAFT);

        for (int i = 0; i < entries.size(); i++) {
            BoatTypeRegistry.Entry entry = entries.get(i);
            BoatEntity.Type type = variants[l + i] = stargate$init(entry, i);

            consumer.accept(type, entry.item(), entry.chest());
        }

        field_7724 = variants;

        CODEC = StringIdentifiable.createCodec(BoatEntity.Type::values);
        BY_ID = ValueLists.<BoatEntity.Type>createIdToValueFunction(Enum::ordinal, variants, ValueLists.OutOfBoundsHandling.ZERO);
    }
}
