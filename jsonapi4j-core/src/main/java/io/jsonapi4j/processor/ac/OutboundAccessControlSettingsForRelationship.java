package io.jsonapi4j.processor.ac;

import io.jsonapi4j.plugin.ac.model.AccessControlRequirementsForObject;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OutboundAccessControlSettingsForRelationship {

    public static OutboundAccessControlSettingsForRelationship DEFAULT = OutboundAccessControlSettingsForRelationship
            .builder()
            .forResourceIdentifier(AccessControlRequirementsForObject.DEFAULT)
            .build();

    @Builder.Default
    private final AccessControlRequirementsForObject forResourceIdentifier = AccessControlRequirementsForObject.DEFAULT;

}
