package io.jsonapi4j.plugin.ac;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public final class ReflectionUtils {

    private ReflectionUtils() {

    }

    public static Object getFieldValue(Object object, String fieldPath) {
        String[] fieldPathParts = fieldPath.split("\\.");
        if (fieldPathParts.length > 1) {
            Object nestedFieldValue = getFieldValue(object, fieldPathParts[0]);
            String nestedFieldPath = Arrays.stream(fieldPathParts).skip(1L).collect(Collectors.joining("."));
            return getFieldValue(nestedFieldValue, nestedFieldPath);
        }
        try {
            Field field = findField(object, fieldPath);
            field.setAccessible(true);
            return field.get(object);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Could not access field '" + fieldPath + "'", e);
        }
    }

    public static void setFieldValue(Object object,
                                     String fieldName,
                                     Object value) {
        try {
            Field field = findField(object, fieldName);
            field.setAccessible(true);
            field.set(object, value);
        } catch (IllegalAccessException e) {
            String valueType = value == null ? "null" : value.getClass().getSimpleName();
            throw new RuntimeException("Could not set field '" + fieldName + "' value with an object of type: " + valueType, e);
        }
    }

    public static <A extends Annotation> Map<String, A> fetchAnnotationForFields(
            Class<?> object,
            Class<A> annotationType
    ) {
        Map<String, A> result = new HashMap<>();
        Map<String, Field> allFields = ReflectionUtils.getAllFields(object);
        for (Map.Entry<String, Field> e : allFields.entrySet()) {
            A fieldAc = e.getValue().getAnnotation(annotationType);
            if (fieldAc != null) {
                result.put(e.getKey(), fieldAc);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    private static Field findField(Object object, String fieldName) {
        Class<?> type = object.getClass();
        Map<String, Field> allFields = getAllFields(type);
        Field field = allFields.get(fieldName);
        if (field == null) {
            throw new RuntimeException("Field '" + fieldName + "' not found on an object of type: " + object.getClass().getSimpleName());
        }
        return field;
    }

    public static Map<String, Field> getAllFields(Class<?> type) {
        return Collections.unmodifiableMap(getAllFieldsRecursively(new HashMap<>(), type));
    }

    private static Map<String, Field> getAllFieldsRecursively(Map<String, Field> fields,
                                                              Class<?> type) {
        for (Field f : type.getDeclaredFields()) {
            fields.put(f.getName(), f);
        }
        if (type.getSuperclass() != null) {
            getAllFieldsRecursively(fields, type.getSuperclass());
        }
        return fields;
    }

}
