package pro.api4.jsonapi4j.util;

import org.apache.commons.lang3.Validate;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

public final class ReflectionUtils {

    private ReflectionUtils() {

    }

    /**
     * Gets field value. Supports nesting (with dot ('.') as a path delimiter).
     * Throws an exception if path is wrong or due to any other reason.
     *
     * @param object    target object
     * @param fieldPath field path
     * @return value
     */
    public static Object getFieldValueThrowing(Object object, String fieldPath) {
        Validate.notNull(object, "object must not be null");
        Validate.notBlank(fieldPath, "fieldPath must not be blank");
        String[] fieldPathParts = fieldPath.split("\\.");
        if (fieldPathParts.length > 1) {
            Object nestedFieldValue = getFieldValueThrowing(object, fieldPathParts[0]);
            String nestedFieldPath = Arrays.stream(fieldPathParts)
                    .skip(1L)
                    .collect(Collectors.joining("."));
            return getFieldValueThrowing(nestedFieldValue, nestedFieldPath);
        }
        try {
            Field field = findField(object, fieldPath);
            field.setAccessible(true);
            return field.get(object);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Could not access field '" + fieldPath + "'", e);
        }
    }

    /**
     * Checks if field path exist for a given object. Using {@link #getFieldValueThrowing(Object, String)} as a backbone.
     *
     * @param object target object
     * @param fieldPath field path
     * @return true if exists, false - otherwise
     */
    public static boolean fieldPathExists(Object object, String fieldPath) {
        try {
            getFieldValueThrowing(object, fieldPath);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Sets value to a particular object field. Doesn't support nesting.
     * Throws an exception if path is wrong or due to any other reason.
     *
     * @param object    target object
     * @param fieldName field name
     * @param value     value to set
     */
    public static void setFieldValueThrowing(Object object,
                                             String fieldName,
                                             Object value) {
        Validate.notNull(object, "object must not be null");
        Validate.notBlank(fieldName, "fieldName must not be blank");
        try {
            Field field = findField(object, fieldName);
            field.setAccessible(true);
            field.set(object, value);
        } catch (IllegalAccessException e) {
            String valueType = value == null ? "null" : value.getClass().getSimpleName();
            throw new IllegalArgumentException("Could not set field '" + fieldName + "' value with an object of type: " + valueType, e);
        }
    }

    /**
     * Sets value to an object following its field path. Supports nesting (with dot ('.') as a path delimiter).
     * Throws an exception if path is wrong or due to any other reason.
     *
     * @param object    target object
     * @param fieldPath field path (use dot ('.') as delimiter)
     * @param value     value to set
     */
    public static void setFieldPathValueThrowing(Object object,
                                                 String fieldPath,
                                                 Object value) {
        Validate.notNull(object, "object must not be null");
        Validate.notBlank(fieldPath, "fieldPath must not be blank");
        String[] pathSegments = fieldPath.split("\\.");
        Object currentObject = object;
        for (int i = 0; i <= pathSegments.length - 1; i++) {
            if (currentObject == null) {
                break;
            }
            String fieldName = pathSegments[i];
            if (pathSegments.length - 1 == i) {
                setFieldValueThrowing(currentObject, fieldName, value);
                break;
            } else {
                currentObject = getFieldValueThrowing(currentObject, fieldName);
            }
        }
    }

    /**
     * Sets value to an object following its field path. Supports nesting (with dot ('.') as a path delimiter).
     * Doesn't throw an exception if path is wrong or due to any other reason.
     *
     * @param object    target object
     * @param fieldPath field path (use dot ('.') as delimiter)
     * @param value     value to set
     */
    public static void setFieldPathValueSilent(Object object,
                                               String fieldPath,
                                               Object value) {
        try {
            setFieldPathValueThrowing(object, fieldPath, value);
        } catch (Exception e) {
            // do nothing
        }

    }

    /**
     * Fetches annotation for a particular method. Each method can be uniquely identified by a name and params.
     *
     * @param type           target type
     * @param methodName     method name
     * @param parameterTypes set of param types
     * @param annotationType target annotation type
     * @param <A>            annotation type
     * @return annotation
     */
    public static <A extends Annotation> A fetchAnnotationForMethod(
            Class<?> type,
            String methodName,
            Class<?>[] parameterTypes,
            Class<A> annotationType) {
        Class<?> currentType = type;
        while (currentType != null && currentType != Object.class) {
            try {
                Method method = currentType.getDeclaredMethod(methodName, parameterTypes);
                A annotation = method.getAnnotation(annotationType);
                if (annotation != null) {
                    return annotation;
                }
            } catch (NoSuchMethodException ignored) {
            }
            currentType = currentType.getSuperclass();
        }
        return null;
    }

    /**
     * CDI proxy fault-tolerant way to get an annotation. Searches for annotation across all superclasses.
     * Some frameworks can use proxies - this method supports these cases.
     *
     * @param type           target object type
     * @param annotationType annotation type
     * @return annotation
     */
    public static <A extends Annotation> A findAnnotationForClass(Class<?> type,
                                                                  Class<A> annotationType) {
        Class<?> currentType = type;
        while (currentType != null && currentType != Object.class) {
            A annotation = currentType.getAnnotation(annotationType);
            if (annotation != null) {
                return annotation;
            }
            currentType = currentType.getSuperclass();
        }
        return null;
    }

    /**
     * CDI proxy fault-tolerant way to find the class that declares a given annotation.
     * Walks superclass hierarchy (handles proxies that subclass the real bean).
     *
     * @param type           target object type (possibly a proxy)
     * @param annotationType annotation type to look for
     * @return the class that declares the annotation, or {@code type} itself if not found
     */
    public static Class<?> findClassWithAnnotation(Class<?> type,
                                                   Class<? extends Annotation> annotationType) {
        Class<?> currentType = type;
        while (currentType != null && currentType != Object.class) {
            if (currentType.getAnnotation(annotationType) != null) {
                return currentType;
            }
            currentType = currentType.getSuperclass();
        }
        return type;
    }

    /**
     * Explores the full set of existing field paths. Recursively analyzes all fields up until reaching
     * {@link Object} superclass.
     *
     * @param clazz target type
     * @return set of all paths
     */
    public static Set<String> getAllFieldPaths(Class<?> clazz) {
        Set<String> result = new HashSet<>();
        traverse(clazz, "", result, new HashSet<>());
        return result;
    }

    private static void traverse(Class<?> clazz,
                                 String prefix,
                                 Set<String> result,
                                 Set<Class<?>> visited) {
        if (clazz == null || isLeafType(clazz)) {
            return;
        }
        // prevent cycles
        if (visited.contains(clazz)) {
            return;
        }
        visited.add(clazz);
        for (Field field : clazz.getDeclaredFields()) {
            field.setAccessible(true);
            String path = prefix.isEmpty()
                    ? field.getName()
                    : prefix + "." + field.getName();
            result.add(path);
            Class<?> fieldType = field.getType();
            // recurse
            traverse(fieldType, path, result, visited);
        }
        visited.remove(clazz); // important for different branches
    }

    private static boolean isLeafType(Class<?> clazz) {
        return clazz.isPrimitive()
                || clazz.equals(String.class)
                || Number.class.isAssignableFrom(clazz)
                || Boolean.class.equals(clazz)
                || Character.class.equals(clazz)
                || Date.class.isAssignableFrom(clazz)
                || clazz.isEnum();
    }

    /**
     * Fetches field types map of the target class and its superclasses.
     *
     * @param objectType type
     * @return map of the target class and its superclasses
     */
    public static Map<String, Class<?>> fetchFieldTypes(Class<?> objectType) {
        return ReflectionUtils.getAllFields(objectType).entrySet().stream().collect(toMap(
                Map.Entry::getKey,
                e -> e.getValue().getType()
        ));
    }

    /**
     * Fetches annotations per field for a given type. Doesn't resolve field types, but resolves fields of all
     * superclasses recursively.
     *
     * @param objectType     target object type
     * @param annotationType annotation type
     * @param <A>            annotation type
     * @return map of fieldName - annotation pairs
     */
    public static <A extends Annotation> Map<String, A> fetchAnnotationForFields(
            Class<?> objectType,
            Class<A> annotationType
    ) {
        Map<String, A> result = new HashMap<>();
        Map<String, Field> allFields = ReflectionUtils.getAllFields(objectType);
        for (Map.Entry<String, Field> e : allFields.entrySet()) {
            A annotation = e.getValue().getAnnotation(annotationType);
            if (annotation != null) {
                result.put(e.getKey(), annotation);
            }
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Checks whether a method declared in a parent class or interface has been overridden
     * in the given implementation class.
     *
     * @param implementationClass the concrete class to check
     * @param methodName          the method name
     * @param parameterTypes      the method parameter types
     * @return {@code true} if the implementation class overrides the method, {@code false} otherwise
     */
    public static boolean isMethodOverridden(Class<?> implementationClass,
                                             String methodName,
                                             Class<?>... parameterTypes) {
        Validate.notNull(implementationClass, "implementationClass must not be null");
        Validate.notBlank(methodName, "methodName must not be blank");
        try {
            Method method = implementationClass.getMethod(methodName, parameterTypes);
            return method.getDeclaringClass().equals(implementationClass);
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    private static Field findField(Object object, String fieldName) {
        Class<?> type = object.getClass();
        Map<String, Field> allFields = getAllFields(type);
        Field field = allFields.get(fieldName);
        if (field == null) {
            throw new IllegalArgumentException("Field '" + fieldName + "' not found on an object of type: " + object.getClass().getSimpleName());
        }
        return field;
    }

    private static Map<String, Field> getAllFields(Class<?> type) {
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
