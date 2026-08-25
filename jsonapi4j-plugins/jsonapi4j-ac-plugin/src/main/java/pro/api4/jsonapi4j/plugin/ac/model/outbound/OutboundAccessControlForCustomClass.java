package pro.api4.jsonapi4j.plugin.ac.model.outbound;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.tuple.ImmutablePair;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlModel;
import pro.api4.jsonapi4j.util.CustomCollectors;

import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * The outbound requirements a single class declares — on the class itself, and on its individual fields.
 *
 * <p>Covers that one class. Requirements on objects it holds are resolved separately, from the runtime type
 * of each value met while a response is anonymized, so a rule applies wherever its type actually appears.
 */
@EqualsAndHashCode
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter(AccessLevel.PUBLIC)
@Builder(access = AccessLevel.PACKAGE)
public class OutboundAccessControlForCustomClass {

    private final AccessControlModel classLevel;
    private final Map<String, AccessControlModel> fieldLevel;

    /**
     * Requirements per class, computed once and reused. Annotations cannot change while the JVM runs, and
     * {@link ClassValue} keys weakly, so an entry is collected together with the class it describes.
     */
    private static final ClassValue<Optional<OutboundAccessControlForCustomClass>> BY_CLASS =
            new ClassValue<>() {
                @Override
                protected Optional<OutboundAccessControlForCustomClass> computeValue(Class<?> clazz) {
                    return Optional.ofNullable(build(clazz));
                }
            };

    /**
     * Reads the requirements declared by the given object's class.
     *
     * @param object the object whose class declares the requirements
     * @return the requirements, or {@code null} when the object is {@code null} or its class declares none
     */
    public static OutboundAccessControlForCustomClass fromClassAnnotationsOf(Object object) {
        return object == null ? null : forClass(object.getClass());
    }

    /**
     * Reads the requirements declared by the given class.
     *
     * @param clazz the class to read
     * @return the requirements, or {@code null} when the class declares none
     */
    public static OutboundAccessControlForCustomClass forClass(Class<?> clazz) {
        return clazz == null ? null : BY_CLASS.get(clazz).orElse(null);
    }

    private static OutboundAccessControlForCustomClass build(Class<?> clazz) {
        AccessControlModel classLevelAccessControl = AccessControlModel.fromClassAnnotation(clazz);
        Map<String, AccessControlModel> fieldLevelAccessControl
                = AccessControlModel.fromFieldsAnnotations(clazz);
        if (classLevelAccessControl == null && fieldLevelAccessControl.isEmpty()) {
            return null;
        }
        return OutboundAccessControlForCustomClass.builder()
                .classLevel(classLevelAccessControl)
                .fieldLevel(fieldLevelAccessControl)
                .build();
    }

    public static OutboundAccessControlForCustomClass merge(OutboundAccessControlForCustomClass lowerPrecedence,
                                                            OutboundAccessControlForCustomClass higherPrecedence) {
        if (lowerPrecedence == null && higherPrecedence == null) {
            return null;
        }
        return OutboundAccessControlForCustomClass.builder()
                .classLevel(mergeClassLevelAccessControl(lowerPrecedence, higherPrecedence))
                .fieldLevel(mergeFieldLevelAccessControl(lowerPrecedence, higherPrecedence))
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

    private static AccessControlModel getFieldLevelAccessControlNullable(
            OutboundAccessControlForCustomClass ac,
            String fieldName
    ) {
        return isFieldLevelAcIsNotNull(ac) ? ac.getFieldLevel().get(fieldName) : null;
    }

    private static boolean isFieldLevelAcIsNotNull(OutboundAccessControlForCustomClass ac) {
        return ac != null && ac.getFieldLevel() != null;
    }

}
