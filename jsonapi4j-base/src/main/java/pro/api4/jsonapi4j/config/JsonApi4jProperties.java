package pro.api4.jsonapi4j.config;

import pro.api4.jsonapi4j.config.PropertiesValidationResult.PropertiesValidationResultBuilder;
import pro.api4.jsonapi4j.operation.validation.ValidationProperties;

/**
 * The root jsonapi4j configuration - everything bound from the {@code jsonapi4j} prefix that is not owned by a
 * plugin: the JSON:API root path, the built-in request validation limits and the meta API switch.
 */
public interface JsonApi4jProperties extends ValidatableProperties {

    String CONFIG_PREFIX = "jsonapi4j";

    String ROOT_PATH_PROPERTY = "rootPath";
    String VALIDATION_PROPERTY = "validation";
    String META_PROPERTY = "meta";

    String DEFAULT_ROOT_PATH = "/jsonapi";

    @Override
    default String propertyPathPrefix() {
        return CONFIG_PREFIX;
    }

    default String rootPath() {
        return DEFAULT_ROOT_PATH;
    }

    ValidationProperties validation();

    MetaProperties meta();

    @Override
    default PropertiesValidationResult validate() {
        PropertiesValidationResultBuilder builder = PropertiesValidationResult.builder()
                .requireServletPath(propertyPath(ROOT_PATH_PROPERTY), rootPath())
                .requireNotNull(propertyPath(VALIDATION_PROPERTY), validation())
                .requireNotNull(propertyPath(META_PROPERTY), meta());
        validateLimits(builder);
        return builder.build();
    }

    /**
     * The built-in request validator reads these on every request and a non-positive limit rejects every request
     * that carries the parameter it caps, so a zero here disables an entire query feature rather than widening it.
     */
    private void validateLimits(PropertiesValidationResultBuilder builder) {
        ValidationProperties validation = validation();
        if (validation == null) {
            return;
        }
        builder.requirePositive(
                        propertyPath(VALIDATION_PROPERTY, "maxNumberFilterParams"),
                        validation.maxNumberFilterParams()
                )
                .requirePositive(
                        propertyPath(VALIDATION_PROPERTY, "maxElementsInFilterParam"),
                        validation.maxElementsInFilterParam()
                )
                .requirePositive(
                        propertyPath(VALIDATION_PROPERTY, "resourceIdMaxLength"),
                        validation.resourceIdMaxLength()
                )
                .requirePositive(
                        propertyPath(VALIDATION_PROPERTY, "limitMaxValue"),
                        validation.limitMaxValue()
                )
                .requirePositive(
                        propertyPath(VALIDATION_PROPERTY, "maxElementsInIncludeParam"),
                        validation.maxElementsInIncludeParam()
                )
                .requirePositive(
                        propertyPath(VALIDATION_PROPERTY, "maxElementsInSortByParam"),
                        validation.maxElementsInSortByParam()
                );
    }

}
