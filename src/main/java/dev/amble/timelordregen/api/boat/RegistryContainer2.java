package dev.amble.timelordregen.api.boat;

import dev.amble.lib.AmbleKit;
import dev.amble.lib.container.AssignedName;
import net.minecraft.util.Identifier;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Locale;

public interface RegistryContainer2<T> {

    Class<T> getTargetClass();

    default boolean start(Field[] fields) {
        return true;
    }

    default boolean preProcessField(Field field) {
        return true;
    }

    default void postProcessField(Identifier identifier, T value, Field field) {
    }

    default void finish() {
    }

    static <T> void register(Class<? extends RegistryContainer2<T>> clazz, String namespace) {
        try {
            RegistryContainer2<T> container = clazz.getDeclaredConstructor().newInstance();
            Field[] fields = clazz.getDeclaredFields();
            if (!container.start(fields)) return;

            for(Field field : fields) {
                if (Modifier.isStatic(field.getModifiers()) && container.getTargetClass().isAssignableFrom(field.getType())
                        && container.preProcessField(field)) {
                    T v = (T) field.get(null);

                    if (v != null) {
                        String name = field.getName().toLowerCase(Locale.ROOT);
                        if (field.isAnnotationPresent(AssignedName.class)) {
                            name = field.getAnnotation(AssignedName.class).value();
                        }

                        Identifier id = new Identifier(namespace, name);
                        container.postProcessField(id, v, field);
                    }
                }
            }

            container.finish();
        } catch (ReflectiveOperationException e) {
            AmbleKit.LOGGER.error("Failed to process a registry container", e);
        }

    }

    static <T> Class<T> conform(Class<?> input) {
        return (Class<T>) input;
    }
}
