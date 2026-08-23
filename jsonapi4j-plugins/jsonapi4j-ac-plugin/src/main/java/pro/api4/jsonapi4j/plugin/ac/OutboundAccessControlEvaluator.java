package pro.api4.jsonapi4j.plugin.ac;

import pro.api4.jsonapi4j.plugin.ac.context.AccessControlContext;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlModel;

public interface OutboundAccessControlEvaluator {

    /**
     * Evaluates Access Control requirements for outbound stage. At this stage requirements are
     * evaluated against prepared JSON:API models right before sending them back to the client.
     * Evaluations include (if requested):
     * <ul>
     *     <li>Check whether user is authenticated</li>
     *     <li>Check the client holds the required entitlements</li>
     *     <li>Check if client got permission to access user data via OAuth2 scopes mechanism</li>
     *     <li>Check if data owned by a user initiated the request (the resource is used as a source of the current owner id)</li>
     *     <li>Check the declared {@code AccessPolicy} allows it</li>
     * </ul>
     *
     * @param context            everything the requirements are decided against — the caller, the operation,
     *                           the resource being emitted, and the stage
     * @param accessControlModel Access Control requirements that should be evaluated
     * @return <code>true</code> if passed, <code>false</code> - otherwise
     */
    boolean evaluateOutboundRequirements(
            AccessControlContext context,
            AccessControlModel accessControlModel
    );

}
