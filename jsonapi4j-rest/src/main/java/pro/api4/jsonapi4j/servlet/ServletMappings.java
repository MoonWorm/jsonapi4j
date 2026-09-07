package pro.api4.jsonapi4j.servlet;

import org.apache.commons.lang3.StringUtils;

/**
 * Turns a configured root path into the servlet mapping the container is registered under.
 * <p>
 * Every integration mounts its servlets from a {@code jsonapi4j.*} root path - the dispatcher from
 * {@code jsonapi4j.rootPath}, the OpenAPI servlet from {@code jsonapi4j.oas.oasRootPath} - and they all have to
 * normalize it the same way, or the same configuration serves the same endpoint at different URLs depending on the
 * host. This is that one normalization.
 */
public final class ServletMappings {

    private static final String MATCH_ALL = "/*";

    private ServletMappings() {
    }

    /**
     * @param rootPath the configured root path, e.g. {@code /jsonapi}; blank, {@code null} and {@code "/"} all mean
     *                 the application root
     * @return the servlet mapping for that path, e.g. {@code /jsonapi/*}
     */
    public static String toMapping(String rootPath) {
        if (StringUtils.isBlank(rootPath) || "/".equals(rootPath.trim())) {
            return MATCH_ALL;
        }
        String normalized = rootPath.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        return normalized.endsWith(MATCH_ALL) ? normalized : normalized + MATCH_ALL;
    }

}
