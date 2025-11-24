package pro.api4.jsonapi4j.ac.model.outbound;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.ac.model.AccessControlModel;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject.META_FIELD;

@EqualsAndHashCode
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter(AccessLevel.PUBLIC)
@Builder(access = AccessLevel.PUBLIC)
public class OutboundAccessControlForJsonApiResourceIdentifier {

    // resource top level
    private final AccessControlModel resourceIdentifierClassLevel;
    // resource 'meta' field level
    private final AccessControlModel resourceIdentifierMetaFieldLevel;

    public static OutboundAccessControlForJsonApiResourceIdentifier fromClassAnnotationsOf(ResourceIdentifierObject resourceIdentifierObject) {
        Validate.notNull(resourceIdentifierObject);
        Class<?> clazz = resourceIdentifierObject.getClass();
        AccessControlModel classLevelAccessControl
                = AccessControlModel.fromClassAnnotation(clazz);
        Map<String, AccessControlModel> fieldLevelAccessControl
                = AccessControlModel.fromFieldsAnnotations(clazz);
        return OutboundAccessControlForJsonApiResourceIdentifier.builder()
                .resourceIdentifierClassLevel(classLevelAccessControl)
                .resourceIdentifierMetaFieldLevel(fieldLevelAccessControl.getOrDefault(META_FIELD, null))
                .build();
    }

    public static OutboundAccessControlForJsonApiResourceIdentifier merge(OutboundAccessControlForJsonApiResourceIdentifier lowerPrecedence,
                                                                          OutboundAccessControlForJsonApiResourceIdentifier higherPrecedence) {
        AccessControlModel resourceClassLevelEffective = AccessControlModel.merge(
                lowerPrecedence != null ? lowerPrecedence.getResourceIdentifierClassLevel() : null,
                higherPrecedence != null ? higherPrecedence.getResourceIdentifierClassLevel() : null
        );

        AccessControlModel resourceMetaFieldLevelEffective = AccessControlModel.merge(
                lowerPrecedence != null ? lowerPrecedence.getResourceIdentifierMetaFieldLevel() : null,
                higherPrecedence != null ? higherPrecedence.getResourceIdentifierMetaFieldLevel() : null
        );

        return OutboundAccessControlForJsonApiResourceIdentifier.builder()
                .resourceIdentifierClassLevel(resourceClassLevelEffective)
                .resourceIdentifierMetaFieldLevel(resourceMetaFieldLevelEffective)
                .build();
    }

    public OutboundAccessControlForCustomClass toOutboundRequirementsForCustomClass() {
        final Map<String, AccessControlModel> fieldLevelAcSettings = new HashMap<>();
        Optional.ofNullable(this.resourceIdentifierMetaFieldLevel)
                .ifPresent(att -> fieldLevelAcSettings.put(META_FIELD, att));
        return OutboundAccessControlForCustomClass.builder()
                .classLevel(this.resourceIdentifierClassLevel)
                .fieldLevel(Collections.unmodifiableMap(fieldLevelAcSettings))
                .build();
    }

}

