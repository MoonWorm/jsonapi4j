package pro.api4.jsonapi4j.request;

import pro.api4.jsonapi4j.compatibility.JsonApi4jCompatibilityMode;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class JsonApiMediaType {

    public static final String TYPE = "application";
    public static final String SUBTYPE = "vnd.api+json";
    public static final String MEDIA_TYPE = TYPE + "/" + SUBTYPE;
    private static final String EXT_PARAM = "ext";
    private static final String PROFILE_PARAM = "profile";
    private static final String Q_PARAM = "q";
    private static final Set<String> JSONAPI_ALLOWED_PARAMS = Set.of(EXT_PARAM, PROFILE_PARAM);

    private JsonApiMediaType() {

    }

    public static boolean isMatches(String mediaType) {
        return isMatches(mediaType, JsonApi4jCompatibilityMode.STRICT);
    }

    public static boolean isMatches(String mediaType,
                                    JsonApi4jCompatibilityMode compatibilityMode) {
        if (compatibilityMode == JsonApi4jCompatibilityMode.LEGACY) {
            return isMatchesLegacy(mediaType);
        }
        ParsedMediaType parsedMediaType = ParsedMediaType.parse(mediaType);
        if (parsedMediaType == null || !parsedMediaType.isJsonApi()) {
            return false;
        }
        return parsedMediaType.hasOnlyParameters(JSONAPI_ALLOWED_PARAMS);
    }

    public static boolean isAccepted(String accepts) {
        return isAccepted(accepts, JsonApi4jCompatibilityMode.STRICT);
    }

    public static boolean isAccepted(String accepts,
                                     JsonApi4jCompatibilityMode compatibilityMode) {
        if (compatibilityMode == JsonApi4jCompatibilityMode.LEGACY) {
            return isAcceptedLegacy(accepts);
        }
        if (accepts == null) {
            return true;
        }
        for (String mediaRange : splitOutsideQuotes(accepts, ',')) {
            ParsedMediaType parsedMediaType = ParsedMediaType.parse(mediaRange);
            if (parsedMediaType == null || parsedMediaType.getQuality() <= 0d) {
                continue;
            }
            if (parsedMediaType.isAnyMediaType() || parsedMediaType.isTypeWildcard(TYPE)) {
                return true;
            }
            if (!parsedMediaType.isJsonApi()) {
                continue;
            }
            if (parsedMediaType.hasOnlyParametersForAccept(JSONAPI_ALLOWED_PARAMS)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isMatchesLegacy(String mediaType) {
        if (mediaType == null) {
            return false;
        }
        String[] parts = mediaType.split("/");
        String normalizedType = parts[0].toLowerCase(Locale.ROOT).trim();
        if (!TYPE.equals(normalizedType) || parts.length < 2) {
            return false;
        }
        String normalizedSubtype = parts[1].split(";")[0].toLowerCase(Locale.ROOT).trim();
        return SUBTYPE.equals(normalizedSubtype);
    }

    private static boolean isAcceptedLegacy(String accepts) {
        if (accepts == null) {
            return true;
        }
        String[] parts = accepts.split(",");
        for (String part : parts) {
            String mediaType = part.split(";")[0].toLowerCase(Locale.ROOT).trim();
            if (mediaType.equals("*/*")
                    || mediaType.equals(TYPE + "/*")
                    || JsonApiMediaType.MEDIA_TYPE.equalsIgnoreCase(mediaType)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> splitOutsideQuotes(String value,
                                                   char separator) {
        List<String> result = new ArrayList<>();
        if (value == null) {
            return result;
        }
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
                current.append(c);
                continue;
            }
            if (c == separator && !inQuotes) {
                result.add(current.toString().trim());
                current.setLength(0);
                continue;
            }
            current.append(c);
        }
        result.add(current.toString().trim());
        return result;
    }

    private record ParsedMediaType(String type,
                                   String subtype,
                                   Map<String, String> parameters) {

        static ParsedMediaType parse(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            List<String> parts = splitOutsideQuotes(value, ';');
            if (parts.isEmpty() || parts.getFirst().isBlank()) {
                return null;
            }

            String[] mediaTypeParts = parts.getFirst().trim().split("/");
            if (mediaTypeParts.length != 2) {
                return null;
            }
            String type = mediaTypeParts[0].trim().toLowerCase(Locale.ROOT);
            String subtype = mediaTypeParts[1].trim().toLowerCase(Locale.ROOT);
            if (type.isEmpty() || subtype.isEmpty()) {
                return null;
            }

            Map<String, String> params = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            for (int i = 1; i < parts.size(); i++) {
                String part = parts.get(i);
                if (part == null || part.isBlank()) {
                    continue;
                }
                int eq = part.indexOf('=');
                if (eq <= 0 || eq == part.length() - 1) {
                    return null;
                }
                String key = part.substring(0, eq).trim();
                String val = part.substring(eq + 1).trim();
                if (key.isEmpty() || val.isEmpty()) {
                    return null;
                }
                params.put(key.toLowerCase(Locale.ROOT), val);
            }
            return new ParsedMediaType(type, subtype, params);
        }

        boolean isJsonApi() {
            return TYPE.equals(type) && SUBTYPE.equals(subtype);
        }

        boolean isAnyMediaType() {
            return "*".equals(type) && "*".equals(subtype);
        }

        boolean isTypeWildcard(String value) {
            return value.equals(type) && "*".equals(subtype);
        }

        boolean hasOnlyParameters(Set<String> allowed) {
            return parameters.keySet().stream().allMatch(allowed::contains)
                    && hasValidJsonApiParameterValues();
        }

        boolean hasOnlyParametersForAccept(Set<String> allowed) {
            return parameters.keySet().stream()
                    .allMatch(paramName -> Q_PARAM.equals(paramName) || allowed.contains(paramName))
                    && hasValidJsonApiParameterValues();
        }

        double getQuality() {
            if (!parameters.containsKey(Q_PARAM)) {
                return 1d;
            }
            try {
                return Double.parseDouble(parameters.get(Q_PARAM));
            } catch (NumberFormatException e) {
                return 0d;
            }
        }

        private boolean hasValidJsonApiParameterValues() {
            return isValidUriListParameter(parameters.get(EXT_PARAM))
                    && isValidUriListParameter(parameters.get(PROFILE_PARAM));
        }

        private boolean isValidUriListParameter(String value) {
            if (value == null) {
                return true;
            }
            if (!isQuoted(value) || value.length() <= 2) {
                return false;
            }
            String uriList = value.substring(1, value.length() - 1);
            if (uriList.isBlank()) {
                return false;
            }
            if (uriList.chars().anyMatch(c -> Character.isWhitespace(c) && c != ' ')) {
                return false;
            }
            String[] uris = uriList.split(" ");
            for (String uriValue : uris) {
                if (uriValue.isBlank() || !isValidAbsoluteUri(uriValue)) {
                    return false;
                }
            }
            return true;
        }

        private boolean isQuoted(String value) {
            return value.length() >= 2
                    && value.charAt(0) == '"'
                    && value.charAt(value.length() - 1) == '"';
        }

        private boolean isValidAbsoluteUri(String value) {
            try {
                URI uri = URI.create(value);
                return uri.isAbsolute();
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
    }

}
