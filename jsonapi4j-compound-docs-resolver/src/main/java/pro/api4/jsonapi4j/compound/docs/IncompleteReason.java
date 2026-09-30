package pro.api4.jsonapi4j.compound.docs;

/**
 * Why resources of an included type are missing from a compound document resolved under
 * {@link pro.api4.jsonapi4j.compound.docs.config.ErrorStrategy#IGNORE}.
 */
public enum IncompleteReason {

    /**
     * Fetching them failed - likely transient, so a retry may return them.
     */
    FETCH_FAILED,

    /**
     * There is no route to fetch their type from - a server configuration issue, so a retry returns the same.
     */
    NO_ROUTE

}
