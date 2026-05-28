package pro.api4.jsonapi4j.request;

import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Request mixin that exposes JSON:API sparse fieldsets query parameters.
 *
 * <p>Parses {@code fields[<type>]=<field>[,<field>]} parameters into a map of resource type
 * to requested field names. When specified, only the listed attributes are included in the response.
 *
 * @see <a href="https://jsonapi.org/format/#fetching-sparse-fieldsets">JSON:API Sparse Fieldsets</a>
 */
public interface SparseFieldsetsAwareRequest {

    Pattern PATTERN = Pattern.compile("^fields\\[(.+)]$");

    static String getFieldsParam(String resourceType) {
        return String.format("fields[%s]", resourceType);
    }

    static String getFieldsParamWithValue(String resourceType,
                                          List<String> fields) {
        if (resourceType == null || resourceType.isEmpty() || CollectionUtils.isEmpty(fields)) {
            return null;
        }
        return String.format("%s=%s", getFieldsParam(resourceType), String.join(",", fields));
    }

    static boolean isJsonApiFieldsParam(String paramName) {
        if (paramName == null) {
            return false;
        }
        String regex = "^fields\\[.+]$";
        return paramName.matches(regex);
    }

    static String extractResourceType(String paramName) {
        if (paramName == null) {
            return null;
        }
        Matcher matcher = PATTERN.matcher(paramName);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Relevant for all JSON:API operations. Optional. If not specified - full resource info is returned.
     * <p>
     * Refer JSON:API specification for more details: <a href="https://jsonapi.org/format/#fetching-sparse-fieldsets">sparse fieldsets</a>
     * <p>
     *
     * @return the map where keys are resource types e.g. 'users', 'countries' and values are list of fields that are
     * supposed to be returned.
     */
    Map<String, List<String>> getFieldSets();

}
