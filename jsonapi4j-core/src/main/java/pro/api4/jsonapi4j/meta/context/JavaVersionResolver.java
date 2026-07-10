package pro.api4.jsonapi4j.meta.context;

public final class JavaVersionResolver {

    private JavaVersionResolver() {

    }

    /**
     * @return the current Java runtime version (e.g. {@code 23.0.1}).
     */
    public static String resolveJavaVersion() {
        return Runtime.version().toString();
    }

}
