package pro.api4.jsonapi4j.plugin.ac.model.outbound;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlModel;
import pro.api4.jsonapi4j.model.document.data.ResourceObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static pro.api4.jsonapi4j.model.document.data.ResourceObject.ATTRIBUTES_FIELD;
import static pro.api4.jsonapi4j.model.document.data.ResourceObject.LINKS_FIELD;
import static pro.api4.jsonapi4j.model.document.data.ResourceObject.META_FIELD;

@EqualsAndHashCode
@ToString
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter(AccessLevel.PUBLIC)
@Builder(access = AccessLevel.PUBLIC)
public class OutboundAccessControlForJsonApiResource {

    // resource top level
    private final AccessControlModel resourceClassLevel;
    // resource 'attributes' field level
    private final AccessControlModel resourceAttributesFieldLevel;
    // resource 'links' field level
    private final AccessControlModel resourceLinksFieldLevel;
    // resource 'meta' field level
    private final AccessControlModel resourceMetaFieldLevel;
    // 'attributes'-specific nested requirements
    private final OutboundAccessControlForCustomClass attributesNested;

    public static OutboundAccessControlForJsonApiResource fromClassAnnotationsOf(ResourceObject<?, ?> resourceObject) {
        if (resourceObject == null) {
            return null;
        }
        Class<?> clazz = resourceObject.getClass();
        AccessControlModel classLevelAccessControl
                = AccessControlModel.fromClassAnnotation(clazz);
        Map<String, AccessControlModel> fieldLevelAccessControl
                = AccessControlModel.fromFieldsAnnotations(clazz);
        OutboundAccessControlForCustomClass attributesNested
                = OutboundAccessControlForCustomClass.fromClassAnnotationsOf(resourceObject.getAttributes());
        return OutboundAccessControlForJsonApiResource.builder()
                .resourceClassLevel(classLevelAccessControl)
                .resourceAttributesFieldLevel(fieldLevelAccessControl.getOrDefault(ATTRIBUTES_FIELD, null))
                .resourceLinksFieldLevel(fieldLevelAccessControl.getOrDefault(LINKS_FIELD, null))
                .resourceMetaFieldLevel(fieldLevelAccessControl.getOrDefault(META_FIELD, null))
                .attributesNested(attributesNested)
                .build();
    }

    public static OutboundAccessControlForJsonApiResource merge(OutboundAccessControlForJsonApiResource lowerPrecedence,
                                                                OutboundAccessControlForJsonApiResource higherPrecedence) {
        AccessControlModel resourceClassLevelEffective = AccessControlModel.merge(
                lowerPrecedence != null ? lowerPrecedence.getResourceClassLevel() : null,
                higherPrecedence != null ? higherPrecedence.getResourceClassLevel() : null
        );

        AccessControlModel resourceAttributesFieldLevelEffective = AccessControlModel.merge(
                lowerPrecedence != null ? lowerPrecedence.getResourceAttributesFieldLevel() : null,
                higherPrecedence != null ? higherPrecedence.getResourceAttributesFieldLevel() : null
        );

        AccessControlModel resourceLinksFieldLevelEffective = AccessControlModel.merge(
                lowerPrecedence != null ? lowerPrecedence.getResourceLinksFieldLevel() : null,
                higherPrecedence != null ? higherPrecedence.getResourceLinksFieldLevel() : null
        );

        AccessControlModel resourceMetaFieldLevelEffective = AccessControlModel.merge(
                lowerPrecedence != null ? lowerPrecedence.getResourceMetaFieldLevel() : null,
                higherPrecedence != null ? higherPrecedence.getResourceMetaFieldLevel() : null
        );

        OutboundAccessControlForCustomClass attributesNestedEffective = OutboundAccessControlForCustomClass.merge(
                lowerPrecedence != null ? lowerPrecedence.getAttributesNested() : null,
                higherPrecedence != null ? higherPrecedence.getAttributesNested() : null
        );

        return OutboundAccessControlForJsonApiResource.builder()
                .resourceClassLevel(resourceClassLevelEffective)
                .resourceAttributesFieldLevel(resourceAttributesFieldLevelEffective)
                .resourceLinksFieldLevel(resourceLinksFieldLevelEffective)
                .resourceMetaFieldLevel(resourceMetaFieldLevelEffective)
                .attributesNested(attributesNestedEffective)
                .build();
    }

    public OutboundAccessControlForCustomClass toOutboundRequirementsForCustomClass() {
        final Map<String, AccessControlModel> fieldLevelAcSettings = new HashMap<>();
        Optional.ofNullable(this.resourceAttributesFieldLevel)
                .ifPresent(att -> fieldLevelAcSettings.put(ATTRIBUTES_FIELD, att));
        Optional.ofNullable(this.resourceLinksFieldLevel)
                .ifPresent(links -> fieldLevelAcSettings.put(LINKS_FIELD, links));
        Optional.ofNullable(this.resourceMetaFieldLevel)
                .ifPresent(meta -> fieldLevelAcSettings.put(META_FIELD, meta));
        return OutboundAccessControlForCustomClass.builder()
                .classLevel(this.resourceClassLevel)
                .fieldLevel(Collections.unmodifiableMap(fieldLevelAcSettings))
                .nested(this.attributesNested == null ? null : Map.of(ATTRIBUTES_FIELD, this.attributesNested))
                .build();
    }

}

