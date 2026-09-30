package pro.api4.jsonapi4j.compound.docs;

/**
 * Why resources are missing from the {@code included} member of a compound document.
 */
public enum IncompleteReason {

    /**
     * Fetching resources of a type failed under {@link pro.api4.jsonapi4j.compound.docs.config.ErrorStrategy#IGNORE}
     * - likely transient, so a retry may return them.
     */
    FETCH_FAILED,

    /**
     * There is no route to fetch a type from, ignored under
     * {@link pro.api4.jsonapi4j.compound.docs.config.ErrorStrategy#IGNORE} - a server configuration issue, so a retry
     * returns the same.
     */
    NO_ROUTE,

    /**
     * The document reached {@code maxIncludedResources}, so an include path was resolved no further - a retry returns
     * the same, asking for less does not.
     */
    MAX_INCLUDED_RESOURCES,

    /**
     * An include path is not supported - deeper than {@code maxHops}, or naming a relationship its resource type
     * doesn't have - and was resolved only as far as supported, under
     * {@link pro.api4.jsonapi4j.compound.docs.config.UnsupportedIncludeStrategy#IGNORE}.
     */
    UNSUPPORTED_INCLUDE

}
