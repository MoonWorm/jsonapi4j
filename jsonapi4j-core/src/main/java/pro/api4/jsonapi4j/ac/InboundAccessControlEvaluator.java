package pro.api4.jsonapi4j.ac;

import pro.api4.jsonapi4j.ac.model.AccessControlModel;

public interface InboundAccessControlEvaluator {

    /**
     * Evaluates Access Control requirements for inbound stage. At this stage requirements are
     * evaluated against incoming JSON:API request before retrieving data from downstream sources.
     * Evaluations include (if requested):
     * <ul>
     *     <li>Check whether user is authenticated</li>
     *     <li>Check clients access tier matching the required one</li>
     *     <li>Check if client got permission to access user data via OAuth2 scopes mechanism</li>
     *     <li>Check if data owned by a user initiated the request (request is used as a source of the current owner id)</li>
     * </ul>
     *
     * @param request            JSON:API request is used as a source for looking for the owner ID.
     *                           This ID will be compared with Principal ID.
     * @param accessControlModel Access Control requirements that should be evaluated
     * @return <code>true</code> if passed, <code>false</code> - otherwise
     */
    <REQUEST> boolean evaluateInboundRequirements(
            REQUEST request,
            AccessControlModel accessControlModel
    );

}
