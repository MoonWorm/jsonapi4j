package pro.api4.jsonapi4j.compound.docs.config;

/**
 * What a request with {@code include} paths the server does not support gets. A path is unsupported when:
 * <ul>
 *     <li>it is deeper than {@link CompoundDocsResolverConfig#getMaxHops()} - checked up front, from the request
 *     alone;</li>
 *     <li>it names a relationship its resource type doesn't have - at any position in the path. Each server checks
 *     the relationships of its own resource types: the one serving the primary data checks the first one, and every
 *     server the resolver fetches from checks the next ones, answering {@code 400 UNSUPPORTED_INCLUDE}. A downstream
 *     service that rejects unknown relationships any other way - or ignores them - is not recognized.</li>
 * </ul>
 */
public enum UnsupportedIncludeStrategy {

    /**
     * The request fails with {@code 400 Bad Request} naming each unsupported path, as the JSON:API specification
     * requires. The default.
     */
    FAIL,

    /**
     * Each unsupported path is resolved only as far as supported - up to {@code maxHops}, or up to the unknown
     * relationship - and listed in the document's {@code meta.includedIncomplete}.
     */
    IGNORE

}
