package pro.api4.jsonapi4j.compound.docs.client;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A JSON:API-aware URL builder that provides a fluent API for constructing
 * spec-compliant request URLs with standard query parameters like
 * {@code filter}, {@code include}, {@code fields}, and custom parameters.
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * String url = JsonApiUrlBuilder.from(baseUri)
 *     .resourceType("users")
 *     .filterParam("id", List.of("1", "2", "3"))
 *     .includeParam(Set.of("placeOfBirth", "citizenships"))
 *     .fieldsParam("users", List.of("firstName", "lastName"))
 *     .build();
 * // → "https://api.example.com/users?filter[id]=1,2,3&include=citizenships,placeOfBirth&fields[users]=firstName,lastName"
 * }</pre>
 *
 * @see <a href="https://jsonapi.org/format/#fetching">JSON:API Fetching</a>
 */
class JsonApiUrlBuilder {

    private final String basePath;
    private String resourceType;
    private final List<String> queryParams = new ArrayList<>();

    private JsonApiUrlBuilder(URI baseUri) {
        String uri = baseUri.toString();
        this.basePath = uri.endsWith("/") ? uri.substring(0, uri.length() - 1) : uri;
    }

    /**
     * Creates a new builder from the given base URI.
     *
     * @param baseUri the base URI (e.g. {@code https://api.example.com/jsonapi})
     * @return a new builder instance
     */
    public static JsonApiUrlBuilder from(URI baseUri) {
        return new JsonApiUrlBuilder(baseUri);
    }

    /**
     * Sets the JSON:API resource type path segment (e.g. "users", "countries").
     *
     * @param resourceType the resource type
     * @return this builder
     */
    public JsonApiUrlBuilder resourceType(String resourceType) {
        this.resourceType = resourceType;
        return this;
    }

    /**
     * Adds a {@code filter[name]=value1,value2} query parameter.
     *
     * @param filterName the filter name (e.g. "id", "status")
     * @param values     the filter values
     * @return this builder
     * @see <a href="https://jsonapi.org/format/#fetching-filtering">JSON:API Filtering</a>
     */
    public JsonApiUrlBuilder filterParam(String filterName, Collection<String> values) {
        if (values != null && !values.isEmpty()) {
            queryParams.add(String.format("filter[%s]=%s", filterName, String.join(",", values)));
        }
        return this;
    }

    /**
     * Adds an {@code include=rel1,rel2} query parameter.
     *
     * @param includes the relationship names to include
     * @return this builder
     * @see <a href="https://jsonapi.org/format/#fetching-includes">JSON:API Inclusion of Related Resources</a>
     */
    public JsonApiUrlBuilder includeParam(Collection<String> includes) {
        if (includes != null && !includes.isEmpty()) {
            queryParams.add("include=" + String.join(",", includes));
        }
        return this;
    }

    /**
     * Adds a {@code fields[type]=attr1,attr2} sparse fieldset query parameter for a single resource type.
     *
     * @param resourceType the resource type
     * @param fields       the field names to request
     * @return this builder
     * @see <a href="https://jsonapi.org/format/#fetching-sparse-fieldsets">JSON:API Sparse Fieldsets</a>
     */
    public JsonApiUrlBuilder fieldsParam(String resourceType, List<String> fields) {
        if (fields != null && !fields.isEmpty()) {
            queryParams.add(String.format("fields[%s]=%s", resourceType, String.join(",", fields)));
        }
        return this;
    }

    /**
     * Adds {@code fields[type]=attr1,attr2} sparse fieldset query parameters for multiple resource types.
     *
     * @param fieldSets a map of resource type to field names
     * @return this builder
     * @see <a href="https://jsonapi.org/format/#fetching-sparse-fieldsets">JSON:API Sparse Fieldsets</a>
     */
    public JsonApiUrlBuilder fieldsParams(Map<String, List<String>> fieldSets) {
        if (fieldSets != null) {
            fieldSets.forEach(this::fieldsParam);
        }
        return this;
    }

    /**
     * Adds a custom query parameter as {@code key=value1,value2}.
     *
     * @param key    the parameter name
     * @param values the parameter values
     * @return this builder
     */
    public JsonApiUrlBuilder queryParam(String key, List<String> values) {
        if (values != null && !values.isEmpty()) {
            queryParams.add(String.format("%s=%s", key, String.join(",", values)));
        }
        return this;
    }

    /**
     * Adds multiple custom query parameters.
     *
     * @param params a map of parameter names to values
     * @return this builder
     */
    public JsonApiUrlBuilder queryParams(Map<String, List<String>> params) {
        if (params != null) {
            params.forEach(this::queryParam);
        }
        return this;
    }

    /**
     * Builds the final URL string.
     *
     * @return the constructed URL
     */
    public String build() {
        StringBuilder sb = new StringBuilder(basePath);
        if (resourceType != null) {
            sb.append("/").append(resourceType);
        }
        if (!queryParams.isEmpty()) {
            sb.append("?").append(queryParams.stream().collect(Collectors.joining("&")));
        }
        return sb.toString();
    }

}
