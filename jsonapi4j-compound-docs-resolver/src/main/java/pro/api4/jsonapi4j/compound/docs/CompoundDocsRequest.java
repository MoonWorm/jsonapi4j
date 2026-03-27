package pro.api4.jsonapi4j.compound.docs;

import pro.api4.jsonapi4j.compound.docs.client.JsonCompoundDocsApiHttpClient;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Narrowed version of the request that has information needed for compound docs resolution logic.
 */
public interface CompoundDocsRequest {

    Pattern RELATIONSHIP_OPERATION_URL_PATTERN = Pattern.compile("/[^/]+/[^/]+/relationships/([^/]+)");

    /**
     * @return HTTP method, e.g. <code>GET</code>, <code>POST</code>
     */
    String method();

    /**
     * JSON:API-specific 'include' query param that tells which nested related resources is supposed to be included.
     * This query param is in fact controlling the Compound Docs resolution logic.
     * <p>
     * For example: <code>include=placeOfBirth.currencies,relatives</code>
     * <p>
     * There is no need to specify lower level includes if they are already included in any higher-level, for
     * example: there is no need to explicitly request for <code>placeOfBirth</code> if
     * <code>placeOfBirth.currencies</code> is already requested.
     *
     * @return list of 'includes' query param values
     */
    List<String> includes();

    /**
     * JSON:API-specific 'Sparse Fieldsets' query params are propagated during the Compound Docs resolution process to guarantee that only requested
     * fields are returned across all included resources.
     * <p>
     * For example: <code>fields[users]=name,age&fields[countries]=region</code>
     *
     * @return dictionary; resource type is used as a key, list of fields - as a value
     */
    Map<String, List<String>> fieldSets();

    /**
     * @return map of all headers of the original HTTP request.
     */
    Map<String, String> headers();

    /**
     * Should return the relative path of the incoming HTTP request - excluding protocol part and domain name.
     * <p>
     * For example:
     * <p>
     * <code>POST /some/path.html HTTP/1.1</code> -> <code>/some/path.html</code>
     * <p>
     * <code>GET http://foo.bar/a.html HTTP/1.0</code> -> <code>/a.html</code>
     * <p>
     * <code>HEAD /xyz?a=b HTTP/1.1</code> -> <code>/xyz</code>
     *
     * @return a <code>String</code> representing a relative path of the URI
     */
    String relativePath();

    /**
     * @return all non JSON:API specific query params (user custom query params)
     */
    Map<String, List<String>> customQueryParams();

    default boolean isProcessable() {
        return "GET".equals(method())
                && !Boolean.parseBoolean(headers().get(JsonCompoundDocsApiHttpClient.X_DISABLE_COMPOUND_DOCS))
                && includes() != null && !includes().isEmpty();
    }

    default String getRelationshipNameFromRequestUri() {
        Matcher matcher = RELATIONSHIP_OPERATION_URL_PATTERN.matcher(relativePath());
        if (matcher.find()) {
            try {
                return matcher.group(1);
            } catch (Exception e) {
                // do nothing
            }
        }
        return null;
    }

}
