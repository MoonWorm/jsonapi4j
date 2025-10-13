package io.jsonapi4j.request;

import java.util.Set;

public interface IncludeAwareRequest {

    int NUMBER_OF_INCLUDES_GLOBAL_CAP = 10;

    String INCLUDE_PARAM = "include";

    /**
     * Relevant for all operations that supports compound docs. Usually these are all GET operations. For example,
     * <code>GET /users/123?include=citizensips.currencies,properties</code>.
     * For this particular example the method will return a set of <code>citizensips</code> and
     * <code>properties</code> values.
     * <p>
     * Refer JSON:API specification for more details:
     * <a href="https://jsonapi.org/format/#fetching-includes">inclusion of related resources</a>
     * <p>
     * This method must always shrink the very first relationship in the chain of relationships. For example,
     * the currently effective relationship in 'citizensips.currencies' chain is 'citizensips'. This is needed
     * for a proper work of a Compound Documents resolver that tier by tier resolves all compound docs and adds then
     * into 'include' member of the top-level JSON:API document by removing the requested relationships one-by-one.
     * <p>
     * All the original requested includes must be still available in {@link #getOriginalIncludes()}.
     *
     * @return sot of requested relationships that are supposed to be added into 'included' member of the JSON:API
     * response.
     */
    Set<String> getEffectiveIncludes();

    default Set<String> getOriginalIncludes() {
        return getEffectiveIncludes();
    }

    default boolean requested(String relationshipName) {
        return getEffectiveIncludes() != null && getEffectiveIncludes().contains(relationshipName);
    }

}
