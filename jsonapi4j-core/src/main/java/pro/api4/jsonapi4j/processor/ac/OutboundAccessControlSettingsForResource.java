package pro.api4.jsonapi4j.processor.ac;

import pro.api4.jsonapi4j.plugin.ac.model.AccessControlRequirementsForObject;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OutboundAccessControlSettingsForResource {

    @Builder.Default
    private final AccessControlRequirementsForObject forResource = AccessControlRequirementsForObject.DEFAULT;
    @Builder.Default
    private final AccessControlRequirementsForObject forAttributes = AccessControlRequirementsForObject.DEFAULT;

    public static OutboundAccessControlSettingsForResource DEFAULT = OutboundAccessControlSettingsForResource.builder()
            .forResource(AccessControlRequirementsForObject.DEFAULT)
            .forAttributes(AccessControlRequirementsForObject.DEFAULT)
            .build();

    public static OutboundAccessControlSettingsForResource fromAnnotations(Class<?> resourceClazz,
                                                                           Class<?> attributesClazz) {
        return new OutboundAccessControlSettingsForResource(
                resourceClazz != null ? AccessControlRequirementsForObject.fromAnnotationsForClass(resourceClazz) : null,
                attributesClazz != null ? AccessControlRequirementsForObject.fromAnnotationsForClass(attributesClazz) : null
        );
    }

    public static OutboundAccessControlSettingsForResource merge(OutboundAccessControlSettingsForResource master,
                                                                 OutboundAccessControlSettingsForResource other) {
        return new OutboundAccessControlSettingsForResource(
                AccessControlRequirementsForObject.merge(
                        master != null ? master.getForResource() : null,
                        other != null ? other.getForResource() : null
                ),
                AccessControlRequirementsForObject.merge(
                        master != null ? master.getForAttributes() : null,
                        other != null ? other.getForAttributes() : null
                )
        );
    }

}
