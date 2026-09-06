package pro.api4.jsonapi4j.plugin.exception;

/**
 * Thrown at startup when the {@code JsonApi4jBuilder} detects a misconfiguration in the
 * registered plugins, such as invalid config file.
 * <p>
 * This is a programmer error and should never occur at runtime in a correctly configured
 * application.
 */
public class PluginMisconfigurationException extends RuntimeException {

    public PluginMisconfigurationException(String message) {
        super(message);
    }

    public PluginMisconfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

}
