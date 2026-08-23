package pro.api4.jsonapi4j.plugin.ac.context;

/**
 * The point in the request lifecycle at which an access control requirement is being evaluated.
 */
public enum Stage {

    /**
     * Before any data has been fetched. Only the request is known — there is no resource to inspect yet.
     */
    INBOUND,

    /**
     * After data has been fetched and the JSON:API document has been composed, right before it is sent.
     * The resource being emitted is available.
     */
    OUTBOUND

}
