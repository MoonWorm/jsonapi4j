package pro.api4.jsonapi4j.processor.ac;

import lombok.Builder;
import lombok.Data;
import pro.api4.jsonapi4j.plugin.ac.JsonApiAnnotationExtractorUtils;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlRequirements;

@Data
@Builder
public class InboundAccessControlSettings {

    public static InboundAccessControlSettings DEFAULT = InboundAccessControlSettings.builder()
            .forRequest(AccessControlRequirements.DEFAULT)
            .build();

    @Builder.Default
    private final AccessControlRequirements forRequest = AccessControlRequirements.DEFAULT;

    public static InboundAccessControlSettings fromAnnotation(Class<?> clazz) {
        return new InboundAccessControlSettings(
                clazz != null ? JsonApiAnnotationExtractorUtils.extractAccessControlInfo(clazz) : null
        );
    }

    public static InboundAccessControlSettings merge(InboundAccessControlSettings master,
                                                     InboundAccessControlSettings other) {
        return new InboundAccessControlSettings(
                AccessControlRequirements.merge(
                        master != null ? master.getForRequest() : null,
                        other != null ? other.getForRequest() : null
                )
        );
    }

}
