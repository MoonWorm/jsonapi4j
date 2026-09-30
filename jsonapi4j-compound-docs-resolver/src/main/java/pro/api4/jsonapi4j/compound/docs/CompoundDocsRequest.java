package pro.api4.jsonapi4j.compound.docs;

import lombok.Data;
import org.apache.commons.lang3.Validate;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static pro.api4.jsonapi4j.http.HttpHeaders.X_DISABLE_COMPOUND_DOCS;

/**
 * Narrowed, immutable view of the incoming request carrying the information needed for compound docs resolution.
 *
 * <p>All components except {@link #getIncludes()} are required (validated at construction). {@code includes} is
 * intentionally nullable — it is {@code null} when the request carries no {@code include} query param, in which case
 * the document is not processable ({@link #isProcessable()}).
 *
 */
@Data
public final class CompoundDocsRequest {

    private static final Pattern RELATIONSHIP_OPERATION_URL_PATTERN = Pattern.compile("/[^/]+/[^/]+/relationships/([^/]+)");

    private final List<String> includes;
    private final List<String> rejectedIncludes;
    private final Map<String, List<String>> fieldSets;
    private final Map<String, String> headers;
    private final Map<String, List<String>> customQueryParams;
    private final String relationshipNameFromRequestUri;

    private String relativePath;
    private boolean processable;

    public CompoundDocsRequest(String method,
                               List<String> includes,
                               Map<String, List<String>> fieldSets,
                               Map<String, String> headers,
                               String relativePath,
                               Map<String, List<String>> customQueryParams) {
        this(method, includes, List.of(), fieldSets, headers, relativePath, customQueryParams);
    }

    /**
     * @param rejectedIncludes include paths the server of the primary data already rejected as unsupported, so the
     *                         request was served without them - reported as gaps of the compound document
     */
    public CompoundDocsRequest(String method,
                               List<String> includes,
                               List<String> rejectedIncludes,
                               Map<String, List<String>> fieldSets,
                               Map<String, String> headers,
                               String relativePath,
                               Map<String, List<String>> customQueryParams) {
        Validate.notNull(rejectedIncludes, "rejectedIncludes must not be null");
        Validate.notBlank(method, "method must not be blank");
        Validate.notNull(fieldSets, "fieldSets must not be null");
        Validate.notNull(headers, "headers must not be null");
        Validate.notBlank(relativePath, "relativePath must not be blank");
        Validate.notNull(customQueryParams, "customQueryParams must not be null");
        this.includes = includes;
        this.rejectedIncludes = List.copyOf(rejectedIncludes);
        this.fieldSets = fieldSets;
        this.headers = headers;
        this.customQueryParams = customQueryParams;
        this.relationshipNameFromRequestUri = getRelationshipNameFromRequestUri(relativePath);
        this.processable = calculateProcessable(method, headers, includes, rejectedIncludes);
    }

    private boolean calculateProcessable(String method,
                                         Map<String, String> headers,
                                         List<String> includes,
                                         List<String> rejectedIncludes) {
        return "GET".equals(method)
                && !Boolean.parseBoolean(headers.get(X_DISABLE_COMPOUND_DOCS.getName()))
                && (includes != null && !includes.isEmpty() || !rejectedIncludes.isEmpty());
    }

    private String getRelationshipNameFromRequestUri(String relativePath) {
        Matcher matcher = RELATIONSHIP_OPERATION_URL_PATTERN.matcher(relativePath);
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
