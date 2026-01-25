package dev.amble.timelordregen.api.boat;

import dev.amble.lib.util.LazyObject;
import net.minecraft.block.Block;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class BoatTypeRegistry {

    private static Item[] TYPE2ITEM;
    private static Item[] CTYPE2ITEM;

    private static final BoatItemConsumer CONSUMER = new BoatItemConsumer() {

        @Override
        public void init(int size) {
            TYPE2ITEM = new Item[size];
            CTYPE2ITEM = new Item[size];
        }

        @Override
        public void accept(BoatEntity.Type type, Item normal, Item chest) {
            TYPE2ITEM[type.ordinal()] = normal;
            CTYPE2ITEM[type.ordinal()] = chest;
        }
    };

    private static final List<Entry> entries = new ArrayList<>();

    public static LazyBoatType register(Identifier id, Item item, Item chest, Block block) {
        Entry entry = new Entry(id, item, chest, block);
        entries.add(entry);

        return entry.lazyGetter();
    }

    public static Item getItem(BoatEntity.Type type, boolean chest) {
        return (chest ? CTYPE2ITEM : TYPE2ITEM)[type.ordinal()];
    }

    public static void apply() {
        ((CustomBoatTypes) (Object) BoatEntity.Type.OAK).amble$recalc(entries, CONSUMER);
    }

    public interface BoatItemConsumer {
        void init(int size);
        void accept(BoatEntity.Type type, Item normal, Item chest);
    }

    public interface CustomBoatTypes {
        void amble$recalc(List<Entry> entries, BoatItemConsumer consumer);
    }

    public record Entry(Identifier id, Item item, Item chest, Block block) {

        public String getPath() {
            return id.getNamespace() + "/" + id.getPath();
        }

        public String getEnumName() {
            return id.getNamespace().toUpperCase() + "_" + id.getPath().replace('/', '_').toUpperCase();
        }

        public LazyBoatType lazyGetter() {
            return new LazyBoatType(this);
        }
    }

    public static class LazyBoatType extends LazyObject<BoatEntity.Type> implements ABoatType {

        public LazyBoatType(Entry entry) {
            super(() -> BoatEntity.Type.getType(entry.getPath()), BoatEntity.Type.OAK);
        }
    }
}
