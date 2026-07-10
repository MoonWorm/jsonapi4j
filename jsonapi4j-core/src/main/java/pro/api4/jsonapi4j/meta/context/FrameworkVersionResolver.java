package pro.api4.jsonapi4j.meta.context;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class FrameworkVersionResolver {

    private static final String VERSION_RESOURCE = "jsonapi4j-version.properties";
    private static final String UNKNOWN_VERSION = "unknown";

    private FrameworkVersionResolver() {

    }

    /**
     * Reads the framework version baked into the core jar at build time (Maven resource filtering of
     * {@code <revision>}). Falls back to {@code "unknown"} if unavailable.
     *
     * @return the framework version string
     */
    public static String resolveFrameworkVersion() {
        try (InputStream in = MetaContext.class.getClassLoader().getResourceAsStream(VERSION_RESOURCE)) {
            if (in == null) {
                return UNKNOWN_VERSION;
            }
            Properties properties = new Properties();
            properties.load(in);
            String version = properties.getProperty("version");
            return StringUtils.isBlank(version) || version.startsWith("${") ? UNKNOWN_VERSION : version;
        } catch (IOException e) {
            return UNKNOWN_VERSION;
        }
    }

}
