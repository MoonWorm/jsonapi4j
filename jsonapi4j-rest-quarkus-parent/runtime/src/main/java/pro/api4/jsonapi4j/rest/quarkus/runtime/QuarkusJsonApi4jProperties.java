package pro.api4.jsonapi4j.rest.quarkus.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import jakarta.inject.Singleton;
import pro.api4.jsonapi4j.config.DefaultJsonApi4jProperties;
import pro.api4.jsonapi4j.config.DefaultJsonApi4jProperties.DefaultValidationProperties;
import pro.api4.jsonapi4j.config.JsonApi4jProperties;
import pro.api4.jsonapi4j.operation.validation.ValidationProperties;

import java.util.Optional;

import static io.smallrye.config.ConfigMapping.NamingStrategy.VERBATIM;

@Singleton
@ConfigMapping(prefix = "jsonapi4j", namingStrategy = VERBATIM)
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface QuarkusJsonApi4jProperties {

    /**
     * Root path where JsonApi4j servlet is mounted.
     * Example: `/jsonapi`, `/api`.
     */
    @WithDefault(JsonApi4jProperties.DEFAULT_ROOT_PATH)
    String rootPath();

    /**
     * Default JsonApi4j validator settings. Optional.
     */
    Optional<QuarkusValidationProperties> validation();

    interface QuarkusValidationProperties {

        /**
         * Max number of 'filter[*]' query params. Optional.
         */
        @WithDefault(ValidationProperties.DEFAULT_MAX_NUMBER_FILTER_PARAMS)
        Optional<Integer> maxNumberFilterParams();

        /**
         * Max number of value elements for each 'filter[*]' query param. Optional.
         */
        @WithDefault(ValidationProperties.DEFAULT_MAX_ELEMENTS_IN_FILTER_PARAM)
        Optional<Integer> maxElementsInFilterParam();

        /**
         * JSON:API Resource Id max length. Optional.
         */
        @WithDefault(ValidationProperties.DEFAULT_RESOURCE_ID_MAX_LENGTH)
        Optional<Integer> resourceIdMaxLength();

        /**
         * Pagination limit param (page[limit]) max value. Optional.
         */
        @WithDefault(ValidationProperties.DEFAULT_LIMIT_MAX_VALUE)
        Optional<Long> limitMaxValue();

        /**
         * Max number of value elements for each 'include' query param. Optional.
         */
        @WithDefault(ValidationProperties.DEFAULT_MAX_ELEMENTS_IN_INCLUDE_PARAM)
        Optional<Integer> maxElementsInIncludeParam();

        /**
         * Max number of value elements for each 'sort' query param. Optional.
         */
        @WithDefault(ValidationProperties.DEFAULT_MAX_ELEMENTS_IN_SORT_BY_PARAM)
        Optional<Integer> maxElementsInSortByParam();

    }

    default JsonApi4jProperties toJsonApi4jProperties() {
        DefaultJsonApi4jProperties properties = new DefaultJsonApi4jProperties();
        properties.setRootPath(rootPath());
        properties.setValidation(validation().map(qv -> {
            DefaultValidationProperties dvp = new DefaultValidationProperties();
            qv.maxNumberFilterParams().ifPresent(dvp::setMaxNumberFilterParams);
            qv.maxElementsInFilterParam().ifPresent(dvp::setMaxElementsInFilterParam);
            qv.resourceIdMaxLength().ifPresent(dvp::setResourceIdMaxLength);
            qv.limitMaxValue().ifPresent(dvp::setLimitMaxValue);
            qv.maxElementsInIncludeParam().ifPresent(dvp::setMaxElementsInIncludeParam);
            qv.maxElementsInSortByParam().ifPresent(dvp::setMaxElementsInSortByParam);
            return dvp;
        }).orElse(null));
        return properties;
    }
}
