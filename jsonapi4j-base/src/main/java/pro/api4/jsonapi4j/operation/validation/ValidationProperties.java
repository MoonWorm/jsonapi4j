package pro.api4.jsonapi4j.operation.validation;

/**
 * Configuration properties that define default limits for JSON:API request validation.
 *
 * <p>Implementations provide upper bounds for common request parameters such as the number of
 * filter parameters, element counts within filters, resource id length, pagination limits, and
 * sort/include element counts. Each property has a corresponding {@code DEFAULT_*} string constant
 * that serves as the default value.
 *
 * @see JsonApiRequestValidator
 */
public interface ValidationProperties {

    String DEFAULT_MAX_NUMBER_FILTER_PARAMS = "5";
    String DEFAULT_MAX_ELEMENTS_IN_FILTER_PARAM = "20";
    String DEFAULT_RESOURCE_ID_MAX_LENGTH = "64";
    String DEFAULT_LIMIT_MAX_VALUE = "100";
    String DEFAULT_MAX_ELEMENTS_IN_INCLUDE_PARAM = "10";
    String DEFAULT_MAX_ELEMENTS_IN_SORT_BY_PARAM = "5";

    /** Returns the maximum number of distinct filter parameters allowed in a request. */
    int maxNumberFilterParams();

    /** Returns the maximum number of values allowed in a single filter parameter. */
    int maxElementsInFilterParam();

    /** Returns the maximum allowed length for a resource id. */
    int resourceIdMaxLength();

    /** Returns the maximum allowed value for the {@code page[limit]} parameter. */
    long limitMaxValue();

    /** Returns the maximum number of relationships allowed in the {@code include} parameter. */
    int maxElementsInIncludeParam();

    /** Returns the maximum number of sort fields allowed in the {@code sort} parameter. */
    int maxElementsInSortByParam();

}
