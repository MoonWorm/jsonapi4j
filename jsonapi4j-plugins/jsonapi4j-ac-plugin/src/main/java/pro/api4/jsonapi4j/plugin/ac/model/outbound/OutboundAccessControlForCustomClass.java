package pro.api4.jsonapi4j.plugin.ac.model.outbound;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import pro.api4.jsonapi4j.model.document.data.ResourceObject;
import pro.api4.jsonapi4j.plugin.ac.diagnostics.AccessControlDiagnostics;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlModel;
import pro.api4.jsonapi4j.util.CustomCollectors;
import pro.api4.jsonapi4j.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

@EqualsAndHashCode
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter(AccessLevel.PUBLIC)
@Builder(access = AccessLevel.PACKAGE)
public class OutboundAccessControlForCustomClass {

    private final AccessControlModel classLevel;
    private final Map<String, AccessControlModel> fieldLevel;
    private final Map<String, OutboundAccessControlForCustomClass> nested;

    /**
     * Outbound requirements per class, computed once and reused.
     * <p>
     * The scan reads no instance values — only the object's class and, for a {@link ResourceObject}, its
     * attributes class — so the result is a pure function of that pair. Nested {@link ClassValue}s hold
     * both keys weakly, so entries are collected along with the classes they describe.
     * <p>
     * Only this entry point is cached; {@code extractNestedRecursively} is not, so no lookup can re-enter
     * the cache it is populating.
     */
    private static final ClassValue<ClassValue<OutboundAccessControlForCustomClass>> BY_CLASS =
            new ClassValue<>() {
                @Override
                protected ClassValue<OutboundAccessControlForCustomClass> computeValue(Class<?> objectClass) {
                    return new ClassValue<>() {
                        @Override
                        protected OutboundAccessControlForCustomClass computeValue(Class<?> attributesClass) {
                            return build(objectClass, attributesClass == NoAttributes.class ? null : attributesClass);
                        }
                    };
                }
            };

    /**
     * Stands in for "this object carries no attributes", so the cache key is never null.
     */
    private static final class NoAttributes {
    }

    /**
     * Reads the outbound requirements declared on the given object's class.
     * <p>
     * Results are cached per class: a page of resources of one type performs a single class-graph scan. An
     * {@code AccessPolicy} declared on an attributes class is therefore instantiated once per process, so
     * policies must be stateless and thread-safe.
     *
     * @param object the object whose class declares the requirements
     * @return the requirements, or {@code null} when {@code object} is {@code null}
     */
    public static OutboundAccessControlForCustomClass fromClassAnnotationsOf(Object object) {
        if (object == null) {
            return null;
        }
        Class<?> attributesClass = NoAttributes.class;
        if (object instanceof ResourceObject<?, ?> resourceObject && resourceObject.getAttributes() != null) {
            attributesClass = resourceObject.getAttributes().getClass();
        }
        return BY_CLASS.get(object.getClass()).get(attributesClass);
    }

    private static OutboundAccessControlForCustomClass build(Class<?> clazz, Class<?> attributesClass) {
        AccessControlModel classLevelAccessControl
                = AccessControlModel.fromClassAnnotation(clazz);
        Map<String, AccessControlModel> fieldLevelAccessControl
                = AccessControlModel.fromFieldsAnnotations(clazz);
        Map<String, OutboundAccessControlForCustomClass> nested
                = new HashMap<>(extractNestedRecursively(clazz));

        // Resolve attributes real type at runtime against constructed object.
        // Otherwise, always resolved as Class<Object> when use reflection API for Type.
        if (attributesClass != null) {
            AccessControlModel attClassLevelAccessControl
                    = AccessControlModel.fromClassAnnotation(attributesClass);
            Map<String, AccessControlModel> attFieldLevelAccessControl
                    = AccessControlModel.fromFieldsAnnotations(attributesClass);
            Map<String, OutboundAccessControlForCustomClass> attNested
                    = extractNestedRecursively(attributesClass);
            AccessControlDiagnostics.reportUnhideableFields(attributesClass, attFieldLevelAccessControl);
            AccessControlDiagnostics.reportShadowedFields(attributesClass);
            if (attClassLevelAccessControl != null
                    || MapUtils.isNotEmpty(attFieldLevelAccessControl)
                    || MapUtils.isNotEmpty(attNested)) {
                nested.put(
                        ResourceObject.ATTRIBUTES_FIELD,
                        OutboundAccessControlForCustomClass.builder()
                                .classLevel(attClassLevelAccessControl)
                                .fieldLevel(attFieldLevelAccessControl)
                                .nested(Collections.unmodifiableMap(attNested))
                                .build()
                );
            }
        }

        AccessControlDiagnostics.reportUnhideableFields(clazz, fieldLevelAccessControl);
        AccessControlDiagnostics.reportShadowedFields(clazz);
        return OutboundAccessControlForCustomClass.builder()
                .classLevel(classLevelAccessControl)
                .fieldLevel(fieldLevelAccessControl)
                .nested(Collections.unmodifiableMap(nested))
                .build();
    }



    private static Map<String, OutboundAccessControlForCustomClass> extractNestedRecursively(Class<?> clazz) {
        return extractNestedRecursively(clazz, new HashSet<>());
    }

    private static Map<String, OutboundAccessControlForCustomClass> extractNestedRecursively(Class<?> clazz,
                                                                                             Set<Class<?>> visited) {
        Map<String, OutboundAccessControlForCustomClass> result = new HashMap<>();

        // Guard against self-referential / cyclic class graphs (e.g. a tree node referencing its own
        // type) that would otherwise recurse until the stack overflows. Scoped to the current branch
        // so diamond-shaped graphs are still fully explored.
        if (!visited.add(clazz)) {
            return result;
        }
        try {
            Map<String, Class<?>> fields = ReflectionUtils.fetchFieldTypes(clazz);
            Map<String, Field> declaredFields = ReflectionUtils.fetchFields(clazz);

            fields.forEach((fieldName, fieldClass) -> {
                AccessControlDiagnostics.reportUnenforceableElementRequirements(
                        clazz, fieldName, fieldClass, declaredFields.get(fieldName).getGenericType());
                if (!ReflectionUtils.isJdkType(fieldClass)) {
                    AccessControlModel classLevelAccessControl
                            = AccessControlModel.fromClassAnnotation(fieldClass);
                    Map<String, AccessControlModel> fieldLevelAccessControl
                            = AccessControlModel.fromFieldsAnnotations(fieldClass);
                    // Enums are value types: their compiler-generated constant fields are static and
                    // self-referential (each constant is a field typed as the enum itself), so recursing
                    // into them never terminates. Capture the enum's own class/field-level access control
                    // but do not descend into its constants.
                    Map<String, OutboundAccessControlForCustomClass> nested = fieldClass.isEnum()
                            ? Collections.emptyMap()
                            : extractNestedRecursively(fieldClass, visited);
                    AccessControlDiagnostics.reportUnhideableFields(fieldClass, fieldLevelAccessControl);
                    AccessControlDiagnostics.reportShadowedFields(fieldClass);
                    if (classLevelAccessControl != null
                            || MapUtils.isNotEmpty(fieldLevelAccessControl)
                            || MapUtils.isNotEmpty(nested)) {
                        result.put(
                                fieldName,
                                OutboundAccessControlForCustomClass.builder()
                                        .classLevel(classLevelAccessControl)
                                        .fieldLevel(fieldLevelAccessControl)
                                        .nested(Collections.unmodifiableMap(nested)).build()
                        );
                    }
                }
            });
        } finally {
            visited.remove(clazz);
        }

        return result;
    }

    public static OutboundAccessControlForCustomClass merge(OutboundAccessControlForCustomClass lowerPrecedence,
                                                            OutboundAccessControlForCustomClass higherPrecedence) {
        return OutboundAccessControlForCustomClass.builder()
                .classLevel(mergeClassLevelAccessControl(lowerPrecedence, higherPrecedence))
                .fieldLevel(mergeFieldLevelAccessControl(lowerPrecedence, higherPrecedence))
                .nested(mergeNestedAccessControl(lowerPrecedence, higherPrecedence))
                .build();
    }

    private static AccessControlModel mergeClassLevelAccessControl(OutboundAccessControlForCustomClass lowerPrecedence,
                                                                   OutboundAccessControlForCustomClass higherPrecedence) {
        return AccessControlModel.merge(
                lowerPrecedence != null ? lowerPrecedence.getClassLevel() : null,
                higherPrecedence != null ? higherPrecedence.getClassLevel() : null
        );
    }

    private static Map<String, AccessControlModel> mergeFieldLevelAccessControl(OutboundAccessControlForCustomClass lowerPrecedence,
                                                                                OutboundAccessControlForCustomClass higherPrecedence) {
        return Stream.concat(
                        isFieldLevelAcIsNotNull(lowerPrecedence) ? lowerPrecedence.getFieldLevel().keySet().stream() : Stream.empty(),
                        isFieldLevelAcIsNotNull(higherPrecedence) ? higherPrecedence.getFieldLevel().keySet().stream() : Stream.empty()
                )
                .distinct()
                .map(fieldName -> new ImmutablePair<>(
                                fieldName,
                                new ImmutablePair<>(
                                        getFieldLevelAccessControlNullable(lowerPrecedence, fieldName),
                                        getFieldLevelAccessControlNullable(higherPrecedence, fieldName)
                                )
                        )
                )
                .filter(p -> p.getRight().getLeft() != null || p.getRight().getRight() != null)
                .collect(
                        CustomCollectors.toMapThatSupportsNullValues(
                                ImmutablePair::getLeft,
                                pair -> AccessControlModel.merge(
                                        pair.getRight().getLeft(),
                                        pair.getRight().getRight()
                                )
                        )
                );
    }

    private static Map<String, OutboundAccessControlForCustomClass> mergeNestedAccessControl(OutboundAccessControlForCustomClass lowerPrecedence,
                                                                                             OutboundAccessControlForCustomClass higherPrecedence) {
        return Stream.concat(
                        isNestedAcIsNotNull(lowerPrecedence) ? lowerPrecedence.getNested().keySet().stream() : Stream.empty(),
                        isNestedAcIsNotNull(higherPrecedence) ? higherPrecedence.getNested().keySet().stream() : Stream.empty()
                )
                .distinct()
                .map(fieldName -> new ImmutablePair<>(
                                fieldName,
                                new ImmutablePair<>(
                                        getNestedAccessControlNullable(lowerPrecedence, fieldName),
                                        getNestedAccessControlNullable(higherPrecedence, fieldName)
                                )
                        )
                )
                .filter(p -> p.getRight().getLeft() != null || p.getRight().getRight() != null)
                .collect(
                        CustomCollectors.toMapThatSupportsNullValues(
                                ImmutablePair::getLeft,
                                pair -> OutboundAccessControlForCustomClass.merge(
                                        pair.getRight().getLeft(),
                                        pair.getRight().getRight()
                                )
                        )
                );
    }

    private static AccessControlModel getFieldLevelAccessControlNullable(
            OutboundAccessControlForCustomClass ac,
            String fieldName
    ) {
        return isFieldLevelAcIsNotNull(ac) ? ac.getFieldLevel().get(fieldName) : null;
    }

    private static OutboundAccessControlForCustomClass getNestedAccessControlNullable(
            OutboundAccessControlForCustomClass ac,
            String fieldName
    ) {
        return isNestedAcIsNotNull(ac) ? ac.getNested().get(fieldName) : null;
    }

    private static boolean isFieldLevelAcIsNotNull(OutboundAccessControlForCustomClass ac) {
        return ac != null && ac.getFieldLevel() != null;
    }

    private static boolean isNestedAcIsNotNull(OutboundAccessControlForCustomClass ac) {
        return ac != null && ac.getNested() != null;
    }

}

