package pro.api4.jsonapi4j.request;

public final class JsonApiMediaType {

    private JsonApiMediaType() {

    }

    public static final String TYPE = "application";
    public static final String SUBTYPE = "vnd.api+json";
    public static final String MEDIA_TYPE = TYPE + "/" + SUBTYPE;

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

    public static boolean isAccepted(String accept) {
        if (accept == null) {
            return true;
        } else if (accept.equals("*/*")) {
            return true;
        } else if (accept.equals(TYPE + "/*")) {
            return true;
        } else return JsonApiMediaType.MEDIA_TYPE.equalsIgnoreCase(accept);
    }

}
