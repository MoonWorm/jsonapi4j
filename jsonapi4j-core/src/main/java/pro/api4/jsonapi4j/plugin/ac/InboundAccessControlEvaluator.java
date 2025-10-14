package pro.api4.jsonapi4j.plugin.ac;

import pro.api4.jsonapi4j.plugin.ac.model.AccessControlRequirements;

public interface InboundAccessControlEvaluator {

    <REQUEST> boolean evaluateInboundRequirements(
            REQUEST request,
            AccessControlRequirements accessControlRequirements
    );

}
