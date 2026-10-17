package pro.api4.jsonapi4j.servlet.response;

import jakarta.servlet.http.HttpServletResponse;

import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import static pro.api4.jsonapi4j.http.HttpHeaders.SERVER_TIMING;

/**
 * Reports how long parts of serving a request took in the {@code Server-Timing} response header, when enabled by
 * {@code jsonapi4j.diagnostics.serverTiming.enabled} - a disabled one adds nothing, so callers need no check of their
 * own.
 *
 * <p>Each metric is added as a header of its own rather than set, so the ones the framework, its plugins and the app
 * report all reach the client - multiple {@code Server-Timing} headers combine into one list. A metric has to be added
 * before the body is written, as that commits the response.
 */
public final class ServerTiming {

    /**
     * How long the framework took to handle the request itself, up to writing the response body.
     */
    public static final String PRIMARY_METRIC = "primary";

    private static final ServerTiming ENABLED = new ServerTiming(true);
    private static final ServerTiming DISABLED = new ServerTiming(false);

    private final boolean enabled;

    private ServerTiming(boolean enabled) {
        this.enabled = enabled;
    }

    public static ServerTiming of(boolean enabled) {
        return enabled ? ENABLED : DISABLED;
    }

    /**
     * @param startedAtNanos the {@link System#nanoTime()} the measured part started at; it ends now
     */
    public void add(HttpServletResponse response, String metric, long startedAtNanos) {
        if (enabled) {
            add(response, metric, Duration.ofNanos(System.nanoTime() - startedAtNanos));
        }
    }

    /**
     * Adds a metric for each of {@code durations}, named by its key under {@code metricPrefix}, in key order.
     */
    public void addAll(HttpServletResponse response, String metricPrefix, Map<String, Duration> durations) {
        if (enabled) {
            new TreeMap<>(durations).forEach((name, duration) -> add(response, metricPrefix + name, duration));
        }
    }

    /**
     * A metric name has to be an HTTP token, so characters a token can't hold - a JSON:API member name may have
     * some - are replaced with {@code _}.
     */
    private void add(HttpServletResponse response, String metric, Duration duration) {
        response.addHeader(SERVER_TIMING.getName(), String.format(
                Locale.ROOT,
                "%s;dur=%.1f",
                metric.replaceAll("[^A-Za-z0-9!#$%&'*+.^_`|~-]", "_"),
                duration.toNanos() / 1_000_000.0
        ));
    }

}
