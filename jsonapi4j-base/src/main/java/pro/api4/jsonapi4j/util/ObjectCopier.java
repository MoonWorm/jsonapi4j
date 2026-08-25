package pro.api4.jsonapi4j.util;

import org.apache.commons.lang3.Validate;
import org.objenesis.Objenesis;
import org.objenesis.ObjenesisStd;
import org.objenesis.instantiator.ObjectInstantiator;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Copies a DTO while blanking out selected fields, without touching the original.
 *
 * <p>The source object is never modified, so it is safe to pass one that is cached, shared, or otherwise
 * outlives the request.
 *
 * <p>Copies are <b>shallow</b>: fields that are not blanked are carried over by reference. Callers that
 * redact deep inside a graph copy each object along the path and rewire the copies as they unwind.
 *
 */
public final class ObjectCopier {

    private static final Objenesis OBJENESIS = new ObjenesisStd();

    private static final Copier NOT_COPYABLE = (source, replacements) -> {
        throw new IllegalStateException("Not copyable");
    };

    private static final ClassValue<Copier> COPIERS = new ClassValue<>() {
        @Override
        protected Copier computeValue(Class<?> type) {
            return resolveCopier(type);
        }
    };

    private ObjectCopier() {

    }

    /**
     * Tells whether {@link #copyWith(Object, Map)} can copy the given type.
     *
     * @param type the type to check
     * @return {@code true} when the type can be copied
     */
    public static boolean isCopyable(Class<?> type) {
        Validate.notNull(type, "type must not be null");
        return COPIERS.get(type) != NOT_COPYABLE;
    }

    /**
     * Returns a copy of the given object with the named fields left null, leaving the original untouched.
     *
     * @param source       the object to copy
     * @param fieldsToNull names of the fields to blank out; may be empty to copy verbatim
     * @param <T>          the object's type
     * @return the copy, or {@code null} when {@code source} is {@code null}
     */
    public static <T> T copyWithout(T source, Set<String> fieldsToNull) {
        Validate.notNull(fieldsToNull, "fieldsToNull must not be null");
        Map<String, Object> replacements = new HashMap<>();
        fieldsToNull.forEach(f -> replacements.put(f, null));
        return copyWith(source, replacements);
    }

    /**
     * Returns a copy of the given object with the named fields replaced, leaving the original untouched.
     *
     * @param source       the object to copy
     * @param replacements new values by field name; a {@code null} value blanks the field out
     * @param <T>          the object's type
     * @return the copy, or {@code null} when {@code source} is {@code null}
     * @throws IllegalArgumentException if the type cannot be copied, or a primitive field is blanked
     */
    @SuppressWarnings("unchecked")
    public static <T> T copyWith(T source, Map<String, Object> replacements) {
        if (source == null) {
            return null;
        }
        Validate.notNull(replacements, "replacements must not be null");
        Class<?> type = source.getClass();
        Copier copier = COPIERS.get(type);
        if (copier == NOT_COPYABLE) {
            throw new IllegalArgumentException(String.format(
                    "Type %s cannot be copied, so its fields cannot be hidden.", type.getName()));
        }
        return (T) copier.copy(source, replacements);
    }

    private static Copier resolveCopier(Class<?> type) {
        return type.isRecord() ? recordCopier(type) : allocatingCopier(type);
    }

    /**
     * Rebuilds a record through its canonical constructor.
     *
     * <p>Record components are the one thing reflection will not write, by design — the runtime is entitled
     * to treat them as genuinely final — so a record can only be copied by constructing a new one.
     */
    private static Copier recordCopier(Class<?> type) {
        RecordComponent[] components = type.getRecordComponents();
        Class<?>[] parameterTypes = Arrays.stream(components)
                .map(RecordComponent::getType)
                .toArray(Class<?>[]::new);
        Constructor<?> canonical;
        try {
            canonical = type.getDeclaredConstructor(parameterTypes);
            canonical.setAccessible(true);
        } catch (NoSuchMethodException | RuntimeException e) {
            return NOT_COPYABLE;
        }
        Map<String, Field> fields = instanceFieldsByName(type);
        return (source, replacements) -> {
            Object[] args = new Object[components.length];
            for (int i = 0; i < components.length; i++) {
                String name = components[i].getName();
                args[i] = replacements.containsKey(name)
                        ? replacement(replacements, name, components[i].getType())
                        : readField(fields.get(name), source);
            }
            try {
                return canonical.newInstance(args);
            } catch (ReflectiveOperationException e) {
                throw new IllegalArgumentException(String.format(
                        "Could not copy the record %s.", type.getName()), e);
            }
        };
    }

    /**
     * Allocates an instance without running a constructor and copies every field across.
     */
    private static Copier allocatingCopier(Class<?> type) {
        if (type.isInterface() || type.isPrimitive() || type.isArray() || Modifier.isAbstract(type.getModifiers())) {
            return NOT_COPYABLE;
        }
        ObjectInstantiator<?> instantiator;
        List<Field> fields;
        try {
            instantiator = OBJENESIS.getInstantiatorOf(type);
            fields = allInstanceFields(type);
        } catch (RuntimeException e) {
            return NOT_COPYABLE;
        }
        if (instantiator == null) {
            return NOT_COPYABLE;
        }
        return (source, replacements) -> {
            Object copy = instantiator.newInstance();
            for (Field field : fields) {
                writeField(field, copy, replacements.containsKey(field.getName())
                        ? replacement(replacements, field.getName(), field.getType())
                        : readField(field, source));
            }
            return copy;
        };
    }

    private static Object replacement(Map<String, Object> replacements, String fieldName, Class<?> fieldType) {
        Object value = replacements.get(fieldName);
        if (value == null && fieldType.isPrimitive()) {
            throw new IllegalArgumentException(String.format(
                    "Field '%s' is a primitive (%s) and cannot be blanked out. Use a boxed type to make it "
                            + "hideable.",
                    fieldName, fieldType.getName()));
        }
        return value;
    }

    private static Object readField(Field field, Object source) {
        try {
            return field.get(source);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(String.format(
                    "Could not read field '%s' while copying.", field.getName()), e);
        }
    }

    private static void writeField(Field field, Object target, Object value) {
        try {
            field.set(target, value);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(String.format(
                    "Could not write field '%s' while copying.", field.getName()), e);
        }
    }

    /**
     * Returns every instance field of the type and its superclasses.
     *
     * <p>Includes synthetic instance fields, such as the reference a non-static inner class holds to its
     * enclosing instance.
     */
    private static List<Field> allInstanceFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (!Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    fields.add(field);
                }
            }
        }
        return Collections.unmodifiableList(fields);
    }

    private static Map<String, Field> instanceFieldsByName(Class<?> type) {
        Map<String, Field> byName = new HashMap<>();
        allInstanceFields(type).forEach(f -> byName.put(f.getName(), f));
        return Collections.unmodifiableMap(byName);
    }

    @FunctionalInterface
    private interface Copier {
        Object copy(Object source, Map<String, Object> replacements);
    }

}
