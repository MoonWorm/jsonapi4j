package pro.api4.jsonapi4j.compound.docs.config;

/**
 * What a request whose {@code include} paths the resolver does not support gets - for now, a path deeper than
 * {@link CompoundDocsResolverConfig#getMaxHops()}.
 */
public enum UnsupportedIncludeStrategy {

    /**
     * The request fails with {@code 400 Bad Request}, as the JSON:API specification requires. The default.
     */
    FAIL,

    /**
     * Each unsupported path is resolved only as deep as supported, and listed in the document's
     * {@code meta.includedIncomplete}.
     */
    IGNORE

}
