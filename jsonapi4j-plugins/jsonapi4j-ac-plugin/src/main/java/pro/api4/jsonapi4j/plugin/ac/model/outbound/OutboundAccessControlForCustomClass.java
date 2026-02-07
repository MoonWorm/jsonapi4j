package pro.api4.jsonapi4j.plugin.ac.model.outbound;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import pro.api4.jsonapi4j.plugin.utils.ReflectionUtils;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlModel;
import pro.api4.jsonapi4j.model.document.data.ResourceObject;
import pro.api4.jsonapi4j.util.CustomCollectors;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
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

    public static OutboundAccessControlForCustomClass fromClassAnnotationsOf(Object object) {
        if (object == null) {
            return null;
        }
        Class<?> clazz = object.getClass();
        AccessControlModel classLevelAccessControl
                = AccessControlModel.fromClassAnnotation(clazz);
        Map<String, AccessControlModel> fieldLevelAccessControl
                = AccessControlModel.fromFieldsAnnotations(clazz);
        Map<String, OutboundAccessControlForCustomClass> nested
                = extractNestedRecursively(clazz);

        // Resolve attributes real type at runtime against constructed object.
        // Otherwise, always resolved as Class<Object> when use reflection API for Type.
        if (object instanceof ResourceObject<?, ?> resourceObject) {
            Class<?> attClazz = resourceObject.getAttributes().getClass();
            AccessControlModel attClassLevelAccessControl
                    = AccessControlModel.fromClassAnnotation(attClazz);
            Map<String, AccessControlModel> attFieldLevelAccessControl
                    = AccessControlModel.fromFieldsAnnotations(attClazz);
            Map<String, OutboundAccessControlForCustomClass> attNested
                    = extractNestedRecursively(attClazz);
            if (attClassLevelAccessControl != null
                || MapUtils.isNotEmpty(attFieldLevelAccessControl)
                || MapUtils.isNotEmpty(attNested)) {
                nested.put(ResourceObject.ATTRIBUTES_FIELD, OutboundAccessControlForCustomClass.builder()
                        .classLevel(attClassLevelAccessControl)
                        .fieldLevel(attFieldLevelAccessControl)
                        .nested(attNested).build());
            }
        }

        return OutboundAccessControlForCustomClass.builder()
                .classLevel(classLevelAccessControl)
                .fieldLevel(fieldLevelAccessControl)
                .nested(Collections.unmodifiableMap(nested))
                .build();
    }

    private static Map<String, OutboundAccessControlForCustomClass> extractNestedRecursively(Class<?> clazz) {
        Map<String, OutboundAccessControlForCustomClass> result = new HashMap<>();

        Map<String, Class<?>> fields = ReflectionUtils.fetchFields(clazz);

        fields.forEach((fieldName, fieldClass) -> {
            if (!fieldClass.getPackageName().startsWith("java.")) {
                AccessControlModel classLevelAccessControl
                        = AccessControlModel.fromClassAnnotation(fieldClass);
                Map<String, AccessControlModel> fieldLevelAccessControl
                        = AccessControlModel.fromFieldsAnnotations(fieldClass);
                Map<String, OutboundAccessControlForCustomClass> nested
                        = extractNestedRecursively(fieldClass);
                if (classLevelAccessControl != null
                        || MapUtils.isNotEmpty(fieldLevelAccessControl)
                        || MapUtils.isNotEmpty(nested)) {
                    result.put(
                            fieldName,
                            OutboundAccessControlForCustomClass.builder()
                                    .classLevel(classLevelAccessControl)
                                    .fieldLevel(fieldLevelAccessControl)
                                    .nested(nested).build()
                    );
                }
            }
        });

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

