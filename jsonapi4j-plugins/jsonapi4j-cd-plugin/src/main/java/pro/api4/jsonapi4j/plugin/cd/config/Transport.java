package pro.api4.jsonapi4j.plugin.cd.config;

/**
 * How resources of a type the app serves itself are fetched for {@code included}. A type mapped to a {@code url} is
 * served by another service and always fetched over HTTP.
 */
public enum Transport {

    /**
     * Read through the framework directly - no HTTP request, no servlet filters. The default.
     */
    IN_PROCESS,

    /**
     * Requested over HTTP from {@code mapping.default.url}, this app's own address - through every servlet filter, with
     * the client's credentials.
     */
    HTTP

}
