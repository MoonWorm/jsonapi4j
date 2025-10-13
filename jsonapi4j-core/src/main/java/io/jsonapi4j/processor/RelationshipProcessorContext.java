package io.jsonapi4j.processor;

import io.jsonapi4j.plugin.ac.AccessControlEvaluator;
import io.jsonapi4j.processor.ac.InboundAccessControlSettings;
import io.jsonapi4j.processor.ac.OutboundAccessControlSettingsForRelationship;
import lombok.Getter;
import lombok.With;
import org.apache.commons.lang3.Validate;

@Getter
@With
public class RelationshipProcessorContext {

    public static final RelationshipProcessorContext DEFAULT = new RelationshipProcessorContext(
            ResourceProcessorContext.DEFAULT_ACCESS_CONTROL_EVALUATOR,
            InboundAccessControlSettings.DEFAULT,
            OutboundAccessControlSettingsForRelationship.DEFAULT
    );

    private final AccessControlEvaluator accessControlEvaluator;
    private final InboundAccessControlSettings inboundAccessControlSettings;
    private final OutboundAccessControlSettingsForRelationship outboundAccessControlSettings;

    public RelationshipProcessorContext(AccessControlEvaluator accessControlEvaluator,
                                        InboundAccessControlSettings inboundAccessControlSettings,
                                        OutboundAccessControlSettingsForRelationship outboundAccessControlSettings) {
        Validate.notNull(accessControlEvaluator);
        Validate.notNull(inboundAccessControlSettings);
        Validate.notNull(outboundAccessControlSettings);
        this.accessControlEvaluator = accessControlEvaluator;
        this.inboundAccessControlSettings = inboundAccessControlSettings;
        this.outboundAccessControlSettings = outboundAccessControlSettings;
    }

}
