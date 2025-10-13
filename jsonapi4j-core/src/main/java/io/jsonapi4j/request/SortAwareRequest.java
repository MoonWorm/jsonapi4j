package io.jsonapi4j.request;

import java.util.Map;

public interface SortAwareRequest {

    int NUMBER_OF_SORT_BY_GLOBAL_CAP = 5;
    String SORT_PARAM = "sort";

    static String extractSortBy(String sortByParam) {
        if (sortByParam.startsWith("+") || sortByParam.startsWith("-")) {
            return sortByParam.substring(1);
        }
        return sortByParam;
    }

    static SortOrder extractSortOrder(String sortByParam) {
        if (sortByParam.startsWith("-")) {
            return SortOrder.DESC;
        }
        return SortOrder.ASC;
    }

    static String wrapWithSortOrder(String sortBy, SortOrder sortOrder) {
        if (sortOrder == SortOrder.ASC) {
            return sortBy;
        } else {
            return "-" + sortBy;
        }
    }

    /**
     * Relevant for all multi resource operations, e.g.<code>GET /users&sort=age,-income,+lastName</code>.
     * For the example above this method will return a map for three keys: <code>age</code>, <code>income</code>, and
     * <code>lastName</code>. With {@link SortOrder#ASC}, {@link SortOrder#DESC}, and {@link SortOrder#ASC}
     * correspondingly. "+" and "" (empty string) both treated as {@link SortOrder#ASC}.
     * <p>
     * Refer JSON:API specification for more details: <a href="https://jsonapi.org/format/#fetching-sorting">sorting</a>
     *
     * @return map of 'sort param name' - {@link SortOrder} pairs
     */
    Map<String, SortOrder> getSortBy();

    enum SortOrder {
        ASC, DESC
    }

}
