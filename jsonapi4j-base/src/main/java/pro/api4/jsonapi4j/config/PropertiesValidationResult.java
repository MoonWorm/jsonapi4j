package pro.api4.jsonapi4j.config;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * Outcome of a {@link ValidatableProperties#validate()} call: everything that is wrong with one configuration
 * section, collected in one pass rather than reported one failure at a time.
 * <p>
 * Errors come in two flavours: <em>property errors</em>, each bound to a fully-qualified config key (see
 * {@link ValidatableProperties#propertyPath(String...)}), and <em>cross-properties errors</em>, which describe an
 * inconsistency between several properties that are individually valid.
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PropertiesValidationResult {

    private static final Set<String> URL_SCHEMES = Set.of("http", "https");

    private final Map<String, List<String>> propertyErrors;
    private final List<String> crossPropertiesErrors;

    public static PropertiesValidationResultBuilder builder() {
        return new PropertiesValidationResultBuilder();
    }

    public static PropertiesValidationResult empty() {
        return new PropertiesValidationResult(Collections.emptyMap(), Collections.emptyList());
    }

    public boolean hasErrors() {
        return !propertyErrors.isEmpty() || !crossPropertiesErrors.isEmpty();
    }

    public Map<String, List<String>> getPropertyErrors() {
        return Collections.unmodifiableMap(propertyErrors);
    }

    public List<String> getCrossPropertiesErrors() {
        return Collections.unmodifiableList(crossPropertiesErrors);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (MapUtils.isNotEmpty(propertyErrors)) {
            sb.append("Property errors:\n");
            propertyErrors.forEach((path, errors) ->
                    errors.forEach(error -> sb.append(String.format("  - '%s': %s\n", path, error)))
            );
        }
        if (CollectionUtils.isNotEmpty(crossPropertiesErrors)) {
            sb.append("Cross-properties errors:\n");
            crossPropertiesErrors.forEach(error -> sb.append(String.format("  - %s\n", error)));
        }
        return sb.toString();
    }

    /**
     * Collects errors for a single {@link ValidatableProperties#validate()} run. The {@code require*} methods are the
     * vocabulary shared by all plugins - each records an error when the value is wrong and does nothing otherwise,
     * so a whole config section reads as one chain.
     */
    public static class PropertiesValidationResultBuilder {

        private final Map<String, List<String>> propertyErrors = new LinkedHashMap<>();
        private final List<String> crossPropertiesErrors = new ArrayList<>();

        public PropertiesValidationResultBuilder addPropertyError(String path,
                                                                       String error) {
            Validate.notBlank(path, "error path shouldn't be blank");
            Validate.notBlank(error, "error message shouldn't be blank");
            propertyErrors.computeIfAbsent(path, p -> new ArrayList<>()).add(error);
            return this;
        }

        public PropertiesValidationResultBuilder addPropertyError(String path,
                                                                       Exception exception) {
            return addPropertyError(path, exception.getMessage());
        }

        public PropertiesValidationResultBuilder addAll(PropertiesValidationResult other) {
            other.propertyErrors.forEach((path, errors) ->
                    errors.forEach(error -> addPropertyError(path, error))
            );
            other.crossPropertiesErrors.forEach(this::addCrossPropertiesError);
            return this;
        }

        public PropertiesValidationResultBuilder addCrossPropertiesError(String error) {
            Validate.notBlank(error, "error message shouldn't be blank");
            crossPropertiesErrors.add(error);
            return this;
        }

        public PropertiesValidationResultBuilder requireNotNull(String path,
                                                                     Object value) {
            if (value == null) {
                addPropertyError(path, "must be set");
            }
            return this;
        }

        public PropertiesValidationResultBuilder requireNotBlank(String path,
                                                                      String value) {
            if (StringUtils.isBlank(value)) {
                addPropertyError(path, "must be set to a non-blank value");
            }
            return this;
        }

        public PropertiesValidationResultBuilder requirePositive(String path,
                                                                      Number value) {
            if (value == null) {
                return requireNotNull(path, null);
            }
            if (value.longValue() < 1) {
                addPropertyError(path, String.format("must be greater than 0, but was %s", value));
            }
            return this;
        }

        /**
         * Requires an absolute {@code http(s)} URL, e.g. {@code https://users.foo.bar/jsonapi}.
         */
        public PropertiesValidationResultBuilder requireHttpUrl(String path,
                                                                     String value) {
            if (StringUtils.isBlank(value)) {
                return requireNotBlank(path, value);
            }
            try {
                URI uri = new URI(value.trim());
                String scheme = StringUtils.lowerCase(uri.getScheme());
                if (scheme == null || !URL_SCHEMES.contains(scheme) || StringUtils.isBlank(uri.getHost())) {
                    addPropertyError(path, String.format(
                            "must be an absolute http(s) URL with a host, but was '%s'", value
                    ));
                }
            } catch (URISyntaxException e) {
                addPropertyError(path, String.format("must be a valid URL, but was '%s': %s", value, e.getMessage()));
            }
            return this;
        }

        /**
         * Requires a path that can be mounted as a servlet mapping, e.g. {@code /jsonapi}: absolute, without a
         * trailing slash (unless it is the root {@code "/"} itself) and without wildcards, since the framework
         * appends {@code "/*"} to it.
         */
        public PropertiesValidationResultBuilder requireServletPath(String path,
                                                                    String value) {
            if (StringUtils.isBlank(value)) {
                return requireNotBlank(path, value);
            }
            String servletPath = value.trim();
            if (!servletPath.startsWith("/")) {
                addPropertyError(path, String.format("must start with '/', but was '%s'", value));
            }
            if (servletPath.length() > 1 && servletPath.endsWith("/")) {
                addPropertyError(path, String.format("must not end with '/', but was '%s'", value));
            }
            if (StringUtils.containsAny(servletPath, '*', '?')) {
                addPropertyError(path, String.format(
                        "must not contain '*' or '?' - it is used as a servlet mapping prefix, but was '%s'", value
                ));
            }
            return this;
        }

        public PropertiesValidationResultBuilder requireOneOfIgnoringCase(String path,
                                                                               String value,
                                                                               Collection<String> supportedValues) {
            if (StringUtils.isBlank(value)) {
                return requireNotBlank(path, value);
            }
            if (supportedValues.stream().noneMatch(v -> v.equalsIgnoreCase(value.trim()))) {
                addPropertyError(path, String.format(
                        "must be one of %s, but was '%s'", supportedValues, value
                ));
            }
            return this;
        }

        public PropertiesValidationResult build() {
            return new PropertiesValidationResult(propertyErrors, crossPropertiesErrors);
        }

    }

}
