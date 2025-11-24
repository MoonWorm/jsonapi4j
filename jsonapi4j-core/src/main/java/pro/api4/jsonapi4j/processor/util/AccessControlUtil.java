package pro.api4.jsonapi4j.processor.util;

import pro.api4.jsonapi4j.ac.model.outbound.OutboundAccessControlForCustomClass;
import pro.api4.jsonapi4j.ac.model.outbound.OutboundAccessControlForJsonApiResource;
import pro.api4.jsonapi4j.processor.ProcessingItem;

import java.util.List;
import java.util.Optional;

public final class AccessControlUtil {

    private AccessControlUtil() {

    }

    public static <DATA_SOURCE_DTO, RESOURCE> OutboundAccessControlForCustomClass getEffectiveOutboundAccessControlSettings(
            List<ProcessingItem<DATA_SOURCE_DTO, RESOURCE>> processingItems,
            OutboundAccessControlForJsonApiResource outboundAccessControlSettingsFromConfig
    ) {
        return getEffectiveOutboundAccessControlSettings(
                processingItems.stream().findFirst().map(ProcessingItem::getResource).orElse(null),
                outboundAccessControlSettingsFromConfig
        );
    }

    public static <RESOURCE> OutboundAccessControlForCustomClass getEffectiveOutboundAccessControlSettings(
            RESOURCE resource,
            OutboundAccessControlForJsonApiResource outboundAccessControlSettingsFromConfig
    ) {
        Optional<Object> resourceNullable = Optional.ofNullable(resource);
        OutboundAccessControlForCustomClass fromConfig = Optional.ofNullable(outboundAccessControlSettingsFromConfig)
                .map(OutboundAccessControlForJsonApiResource::toOutboundRequirementsForCustomClass)
                .orElse(null);
        OutboundAccessControlForCustomClass fromAnnotations = resourceNullable
                .map(OutboundAccessControlForCustomClass::fromClassAnnotationsOf)
                .orElse(null);
        return OutboundAccessControlForCustomClass.merge(fromAnnotations, fromConfig);
    }

}
