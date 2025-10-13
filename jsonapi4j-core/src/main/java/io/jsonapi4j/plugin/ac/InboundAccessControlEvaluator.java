package io.jsonapi4j.plugin.ac;

import io.jsonapi4j.plugin.ac.model.AccessControlRequirements;

public interface InboundAccessControlEvaluator {

    <REQUEST> boolean evaluateInboundRequirements(
            REQUEST request,
            AccessControlRequirements accessControlRequirements
    );

}
