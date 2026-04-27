package pro.api4.jsonapi4j.operation.validation;

public interface ValidationProperties {

    String DEFAULT_MAX_NUMBER_FILTER_PARAMS = "5";
    String DEFAULT_MAX_ELEMENTS_IN_FILTER_PARAM = "20";
    String DEFAULT_RESOURCE_ID_MAX_LENGTH = "64";
    String DEFAULT_LIMIT_MAX_VALUE = "100";
    String DEFAULT_MAX_ELEMENTS_IN_INCLUDE_PARAM = "10";
    String DEFAULT_MAX_ELEMENTS_IN_SORT_BY_PARAM = "5";

    int maxNumberFilterParams();

    int maxElementsInFilterParam();

    int resourceIdMaxLength();

    long limitMaxValue();

    int maxElementsInIncludeParam();

    int maxElementsInSortByParam();

}
