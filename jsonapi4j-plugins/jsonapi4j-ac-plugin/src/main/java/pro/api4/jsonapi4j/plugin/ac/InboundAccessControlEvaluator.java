package pro.api4.jsonapi4j.plugin.ac;

import pro.api4.jsonapi4j.plugin.ac.context.AccessControlContext;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlModel;

public interface InboundAccessControlEvaluator {

    /**
     * Evaluates Access Control requirements for inbound stage. At this stage requirements are
     * evaluated against incoming JSON:API request before retrieving data from downstream sources.
     * Evaluations include (if requested):
     * <ul>
     *     <li>Check whether user is authenticated</li>
     *     <li>Check the client holds the required entitlements</li>
     *     <li>Check if client got permission to access user data via OAuth2 scopes mechanism</li>
     *     <li>Check if data owned by a user initiated the request (request is used as a source of the current owner id)</li>
     *     <li>Check the declared {@code AccessPolicy} allows it</li>
     * </ul>
     *
     * <p>A denied result carries the {@link pro.api4.jsonapi4j.model.document.error.ErrorCode} reported to
     * the caller.
     *
     * @param context            everything the requirements are decided against — the caller, the operation,
     *                           the request, and the stage
     * @param accessControlModel Access Control requirements that should be evaluated
     * @return the decision, and the refusing requirement when denied
     */
    EvaluationResult evaluateInboundRequirements(
            AccessControlContext context,
            AccessControlModel accessControlModel
    );

}
