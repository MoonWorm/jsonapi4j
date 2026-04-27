package pro.api4.jsonapi4j.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pro.api4.jsonapi4j.operation.validation.ValidationProperties;

@NoArgsConstructor
@Getter
@Setter
public class DefaultJsonApi4jProperties implements JsonApi4jProperties {

    private String rootPath;
    private DefaultValidationProperties validation;

    @Override
    public String rootPath() {
        return rootPath;
    }

    @Override
    public ValidationProperties validation() {
        return validation;
    }

    @Getter
    @Setter
    public static class DefaultValidationProperties implements ValidationProperties {

        private int maxNumberFilterParams = Integer.parseInt(DEFAULT_MAX_NUMBER_FILTER_PARAMS);
        private int maxElementsInFilterParam = Integer.parseInt(DEFAULT_MAX_ELEMENTS_IN_FILTER_PARAM);
        private int resourceIdMaxLength = Integer.parseInt(DEFAULT_RESOURCE_ID_MAX_LENGTH);
        private long limitMaxValue = Long.parseLong(DEFAULT_LIMIT_MAX_VALUE);
        private int maxElementsInIncludeParam = Integer.parseInt(DEFAULT_MAX_ELEMENTS_IN_INCLUDE_PARAM);
        private int maxElementsInSortByParam = Integer.parseInt(DEFAULT_MAX_ELEMENTS_IN_SORT_BY_PARAM);

        @Override
        public int maxNumberFilterParams() {
            return this.maxNumberFilterParams;
        }

        @Override
        public int maxElementsInFilterParam() {
            return this.maxElementsInFilterParam;
        }

        @Override
        public int resourceIdMaxLength() {
            return this.resourceIdMaxLength;
        }

        @Override
        public long limitMaxValue() {
            return this.limitMaxValue;
        }

        @Override
        public int maxElementsInIncludeParam() {
            return this.maxElementsInIncludeParam;
        }

        @Override
        public int maxElementsInSortByParam() {
            return this.maxElementsInSortByParam;
        }

    }

}
