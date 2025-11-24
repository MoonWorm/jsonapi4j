package pro.api4.jsonapi4j.ac;

import pro.api4.jsonapi4j.ac.model.AccessControlModel;

public interface OutboundAccessControlEvaluator {

    /**
     * Evaluates Access Control requirements for outbound stage. At this stage requirements are
     * evaluated against prepared JSON:API models right before sending them back to the client.
     * Evaluations include (if requested):
     * <ul>
     *     <li>Check whether user is authenticated</li>
     *     <li>Check clients access tier matching the required one</li>
     *     <li>Check if client got permission to access user data via OAuth2 scopes mechanism</li>
     *     <li>Check if data owned by a user initiated the request (resourceObject is used as a source of the current owner id)</li>
     * </ul>
     *
     * @param resourceObject     JSON:API Resource or Resource Identifier object reference. This model is used as
     *                           a root reference for looking for the owner ID. This ID will be compared with Principal ID.
     * @param accessControlModel Access Control requirements that should be evaluated
     * @return <code>true</code> if passed, <code>false</code> - otherwise
     */
    boolean evaluateOutboundRequirements(
            Object resourceObject,
            AccessControlModel accessControlModel
    );

}
