package pro.api4.jsonapi4j.request;

/**
 * Utility class for the JSON:API media type {@code application/vnd.api+json}.
 * <p>
 * Provides constants for the type, subtype, and fully-qualified media type string, plus
 * helper methods for content-negotiation: checking whether an incoming {@code Content-Type}
 * header matches the JSON:API media type ({@link #isMatches}), whether a client's
 * {@code Accept} header accepts it ({@link #isAccepted}), and extracting media-type
 * parameters (e.g. {@code ext}, {@code profile}) from a header value ({@link #getParam}).
 *
 * @see <a href="https://jsonapi.org/format/#content-negotiation">JSON:API Content Negotiation</a>
 */
public final class JsonApiMediaType {

    /** The type component of the JSON:API media type: {@code application}. */
    public static final String TYPE = "application";
    /** The subtype component of the JSON:API media type: {@code vnd.api+json}. */
    public static final String SUBTYPE = "vnd.api+json";
    /** The fully-qualified JSON:API media type string: {@code application/vnd.api+json}. */
    public static final String MEDIA_TYPE = TYPE + "/" + SUBTYPE;

    private JsonApiMediaType() {

    }

    /**
     * Returns {@code true} if the given media type string represents the JSON:API media type,
     * ignoring parameters (e.g. {@code ext}, {@code profile}) and case.
     *
     * @param mediaType the {@code Content-Type} header value to test; {@code null} returns {@code false}
     * @return {@code true} if the media type is {@code application/vnd.api+json}
     */
    public static boolean isMatches(String mediaType) {
        if (mediaType == null) {
            return false;
        }
        String[] parts = mediaType.split("/");
        String normalizedType = parts[0].toLowerCase().trim();
        if (!TYPE.equals(normalizedType) || parts.length < 2) {
            return false;
        }
        String normalizedSubtype = parts[1].split(";")[0].toLowerCase().trim();
        return SUBTYPE.equals(normalizedSubtype);
    }

    /**
     * Returns {@code true} if the given {@code Accept} header value accepts the JSON:API media type.
     * <p>
     * Accepts {@code *}{@code /*}, {@code application/*}, or {@code application/vnd.api+json}.
     * A {@code null} accept string is treated as accepting everything.
     *
     * @param accepts the {@code Accept} header value; {@code null} returns {@code true}
     * @return {@code true} if the JSON:API media type is acceptable to the client
     */
    public static boolean isAccepted(String accepts) {
        if (accepts == null) {
            return true;
        }
        String[] acceptValues = accepts.split(",");
        for (String mediaTypeAndParams: acceptValues) {
            String mediaType = mediaTypeAndParams.split(";")[0].toLowerCase().trim();
            if (mediaType.equals("*/*")
                    || mediaType.equals(TYPE + "/*")
                    || JsonApiMediaType.MEDIA_TYPE.equalsIgnoreCase(mediaType)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Extracts a named parameter value from a media type string.
     * <p>
     * For example, given {@code "application/vnd.api+json;ext=atomic"} and param name {@code "ext"},
     * returns {@code "atomic"}.
     *
     * @param mediaType the media type string (may include parameters separated by {@code ;})
     * @param paramName the parameter name to look up (case-insensitive)
     * @return the parameter value, or {@code null} if not present or if either argument is {@code null}
     */
    public static String getParam(String mediaType, String paramName) {
        if (mediaType == null || paramName == null) {
            return null;
        }
        String[] parts = mediaType.split(";");
        if (parts.length > 1) {
            for (int i = 1; i < parts.length; i++) {
                String paramStr = parts[i].toLowerCase().trim();
                String[] keyValueParts = paramStr.split("=");
                if (keyValueParts.length == 2 && paramName.equalsIgnoreCase(keyValueParts[0])) {
                    return keyValueParts[1];
                }
            }
        }
        return null;
    }

}
