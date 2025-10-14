package pro.api4.jsonapi4j.processor.ac;

import pro.api4.jsonapi4j.plugin.ac.model.AccessControlRequirements;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class InboundAccessControlSettings {

    public static InboundAccessControlSettings DEFAULT = InboundAccessControlSettings.builder()
            .forRequest(AccessControlRequirements.DEFAULT)
            .build();

    @Builder.Default
    private final AccessControlRequirements forRequest = AccessControlRequirements.DEFAULT;

}
