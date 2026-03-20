package pro.api4.jsonapi4j.plugin.sf.config;

public interface SfProperties {

    String DEFAULT_SF_ENABLED = "true";
    String DEFAULT_REQUESTED_FIELDS_DONT_EXIST_MODE = "SPARSE_ALL_FIELDS";

    default boolean enabled() {
        return Boolean.parseBoolean(DEFAULT_SF_ENABLED);
    }

    default RequestedFieldsDontExistMode requestedFieldsDontExistMode() {
        return RequestedFieldsDontExistMode.valueOf(DEFAULT_REQUESTED_FIELDS_DONT_EXIST_MODE);
    }

    /**
     * JSON:API explicitly tells that if empty 'fields[TYPE]=' is requested - no fields should be returned.
     * Also, the spec tells we should ignore non-existing fields.
     * But it's unclear how to behave if some fields were requested but none exists.
     * This enum defines strategies of how to behave in situations when some fields were requested but none exists.
     */
    enum RequestedFieldsDontExistMode {
        /**
         * Equal to the situation where 'fields[TYPE]' parameter is never requested
         */
        RETURN_ALL_FIELDS,
        /**
         * Equal to 'fields[TYPE]=' which explicitly tells to do not return any fields
         */
        SPARSE_ALL_FIELDS
    }

}
