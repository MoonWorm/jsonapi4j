package pro.api4.jsonapi4j.plugin.ac.model;

import pro.api4.jsonapi4j.plugin.ac.model.outbound.OutboundAccessControlForCustomClass;
import pro.api4.jsonapi4j.plugin.ac.model.outbound.OutboundAccessControlForJsonApiResource;

import java.util.Optional;

public final class AccessControlUtil {

    private AccessControlUtil() {

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
