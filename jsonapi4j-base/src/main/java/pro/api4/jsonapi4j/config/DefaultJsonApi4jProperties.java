package pro.api4.jsonapi4j.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import pro.api4.jsonapi4j.operation.validation.ValidationProperties;

@NoArgsConstructor
@Getter
@Setter
@ToString
public class DefaultJsonApi4jProperties implements JsonApi4jProperties {

    private String rootPath = DEFAULT_ROOT_PATH;
    private DefaultValidationProperties validation = new DefaultValidationProperties();
    private DefaultMetaProperties meta = new DefaultMetaProperties();
    private DefaultDiagnosticsProperties diagnostics = new DefaultDiagnosticsProperties();

    @Override
    public String rootPath() {
        return rootPath;
    }

    @Override
    public ValidationProperties validation() {
        return validation;
    }

    @Override
    public MetaProperties meta() {
        return meta;
    }

    @Override
    public DiagnosticsProperties diagnostics() {
        return diagnostics;
    }

    @Getter
    @Setter
    public static class DefaultMetaProperties implements MetaProperties {

        private boolean enabled = Boolean.parseBoolean(DEFAULT_ENABLED);

        @Override
        public boolean enabled() {
            return this.enabled;
        }

    }

    @Getter
    @Setter
    @ToString
    public static class DefaultDiagnosticsProperties implements DiagnosticsProperties {

        private DefaultServerTimingProperties serverTiming = new DefaultServerTimingProperties();
        private DefaultStartupReportProperties startupReport = new DefaultStartupReportProperties();

        @Override
        public ServerTimingProperties serverTiming() {
            return this.serverTiming;
        }

        @Override
        public StartupReportProperties startupReport() {
            return this.startupReport;
        }

    }

    @Getter
    @Setter
    @ToString
    public static class DefaultServerTimingProperties implements DiagnosticsProperties.ServerTimingProperties {

        private boolean enabled = Boolean.parseBoolean(DEFAULT_ENABLED);

        @Override
        public boolean enabled() {
            return this.enabled;
        }

    }

    @Getter
    @Setter
    @ToString
    public static class DefaultStartupReportProperties implements DiagnosticsProperties.StartupReportProperties {

        private boolean enabled = Boolean.parseBoolean(DEFAULT_ENABLED);

        @Override
        public boolean enabled() {
            return this.enabled;
        }

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
