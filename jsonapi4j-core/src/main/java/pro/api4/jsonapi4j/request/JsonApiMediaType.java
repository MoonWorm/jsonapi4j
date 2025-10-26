package pro.api4.jsonapi4j.request;

public final class JsonApiMediaType {

    public static final String TYPE = "application";
    public static final String SUBTYPE = "vnd.api+json";
    public static final String MEDIA_TYPE = TYPE + "/" + SUBTYPE;
    private JsonApiMediaType() {

    }

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

    public static boolean isAccepted(String accepts) {
        if (accepts == null) {
            return true;
        } else {
            String[] parts = accepts.split(",");
            for (String part : parts) {
                String mediaType = part.split(";")[0].toLowerCase().trim();
                if (mediaType.equals("*/*")
                        || mediaType.equals(TYPE + "/*")
                        || JsonApiMediaType.MEDIA_TYPE.equalsIgnoreCase(mediaType)) {
                    return true;
                }
            }
            return false;
        }
    }

}
