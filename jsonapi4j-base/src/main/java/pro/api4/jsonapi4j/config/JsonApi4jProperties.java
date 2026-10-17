package pro.api4.jsonapi4j.config;

import pro.api4.jsonapi4j.config.PropertiesValidationResult.PropertiesValidationResultBuilder;
import pro.api4.jsonapi4j.operation.validation.ValidationProperties;

/**
 * The root jsonapi4j configuration - everything bound from the {@code jsonapi4j} prefix that is not owned by a
 * plugin: the JSON:API root path, the built-in request validation limits, the meta API switch and the
 * developer-facing diagnostics.
 */
public interface JsonApi4jProperties extends ValidatableProperties {

    String CONFIG_PREFIX = "jsonapi4j";

    String ROOT_PATH_PROPERTY = "rootPath";
    String VALIDATION_PROPERTY = "validation";
    String META_PROPERTY = "meta";
    String DIAGNOSTICS_PROPERTY = "diagnostics";

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

    DiagnosticsProperties diagnostics();

    @Override
    default PropertiesValidationResult validate() {
        PropertiesValidationResultBuilder builder = PropertiesValidationResult.builder()
                .requireServletPath(propertyPath(ROOT_PATH_PROPERTY), rootPath())
                .requireNotNull(propertyPath(VALIDATION_PROPERTY), validation())
                .requireNotNull(propertyPath(META_PROPERTY), meta())
                .requireNotNull(propertyPath(DIAGNOSTICS_PROPERTY), diagnostics());
        validateLimits(builder);
        validateDiagnostics(builder);
        return builder.build();
    }

    /**
     * A key left empty in a config file binds its section to {@code null} rather than to its defaults.
     */
    private void validateDiagnostics(PropertiesValidationResultBuilder builder) {
        DiagnosticsProperties diagnostics = diagnostics();
        if (diagnostics == null) {
            return;
        }
        builder.requireNotNull(
                        propertyPath(DIAGNOSTICS_PROPERTY, DiagnosticsProperties.SERVER_TIMING_PROPERTY),
                        diagnostics.serverTiming()
                )
                .requireNotNull(
                        propertyPath(DIAGNOSTICS_PROPERTY, DiagnosticsProperties.STARTUP_REPORT_PROPERTY),
                        diagnostics.startupReport()
                );
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
