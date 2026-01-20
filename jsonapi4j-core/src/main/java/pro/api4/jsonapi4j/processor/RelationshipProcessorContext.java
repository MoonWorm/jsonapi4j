package pro.api4.jsonapi4j.processor;

import lombok.Getter;
import lombok.With;
import pro.api4.jsonapi4j.plugin.ac.AccessControlEvaluator;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlModel;
import pro.api4.jsonapi4j.plugin.ac.model.outbound.OutboundAccessControlForJsonApiResourceIdentifier;

@Getter
@With
public class RelationshipProcessorContext {

    private AccessControlEvaluator accessControlEvaluator;
    private AccessControlModel inboundAccessControlSettings;
    private OutboundAccessControlForJsonApiResourceIdentifier outboundAccessControlSettings;

    public RelationshipProcessorContext(AccessControlEvaluator accessControlEvaluator,
                                        AccessControlModel inboundAccessControlSettings,
                                        OutboundAccessControlForJsonApiResourceIdentifier outboundAccessControlSettings) {
        this.accessControlEvaluator = accessControlEvaluator;
        this.inboundAccessControlSettings = inboundAccessControlSettings;
        this.outboundAccessControlSettings = outboundAccessControlSettings;
    }

    public RelationshipProcessorContext() {
    }

}
