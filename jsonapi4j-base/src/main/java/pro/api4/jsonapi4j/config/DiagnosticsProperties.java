package pro.api4.jsonapi4j.config;

/**
 * Developer-facing diagnostics, bound from {@code jsonapi4j.diagnostics.*}.
 * <p>
 * Everything here is off by default: it tells clients how the app works inside, which is worth seeing while
 * developing, but not worth publishing in production.
 */
public interface DiagnosticsProperties {

    String SERVER_TIMING_PROPERTY = "serverTiming";
    String STARTUP_REPORT_PROPERTY = "startupReport";

    ServerTimingProperties serverTiming();

    StartupReportProperties startupReport();

    /**
     * The {@code Server-Timing} response header, bound from {@code jsonapi4j.diagnostics.serverTiming.*}: how long the
     * framework, and the plugins taking part in a request, took to serve it.
     */
    interface ServerTimingProperties {

        String ENABLED_PROPERTY = "enabled";

        String DEFAULT_ENABLED = "false";

        default boolean enabled() {
            return Boolean.parseBoolean(DEFAULT_ENABLED);
        }

    }

    /**
     * The report logged once the framework starts, bound from {@code jsonapi4j.diagnostics.startupReport.*}: the
     * registered resources, relationships, operations and plugins.
     */
    interface StartupReportProperties {

        String ENABLED_PROPERTY = "enabled";

        String DEFAULT_ENABLED = "false";

        default boolean enabled() {
            return Boolean.parseBoolean(DEFAULT_ENABLED);
        }

    }

}
