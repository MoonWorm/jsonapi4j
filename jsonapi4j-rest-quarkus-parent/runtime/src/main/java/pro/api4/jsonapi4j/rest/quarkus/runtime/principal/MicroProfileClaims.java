package pro.api4.jsonapi4j.rest.quarkus.runtime.principal;

import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts the JSON-P values that MicroProfile JWT exposes for custom claims into plain Java types.
 * <p>
 * {@link org.eclipse.microprofile.jwt.JsonWebToken#getClaim(String)} returns Java types for the registered
 * claims but {@link JsonValue} instances for custom ones, whose {@code toString()} keeps the JSON quoting —
 * a claim read as {@code "user@api4.pro"} (quotes included) rather than {@code user@api4.pro}. Normalizing
 * here keeps principal attributes and scope resolution consistent with the other resolvers.
 */
final class MicroProfileClaims {

    private MicroProfileClaims() {
    }

    /**
     * Converts a claim value to its plain Java equivalent, recursively for arrays and objects.
     *
     * @param claimValue the raw claim value as exposed by MicroProfile JWT
     * @return a {@link String}, {@link Number}, {@link Boolean}, {@link List}, {@link Map} or {@code null};
     * values that are already plain Java types are returned unchanged
     */
    static Object toJavaType(Object claimValue) {
        if (!(claimValue instanceof JsonValue jsonValue)) {
            return claimValue;
        }
        return switch (jsonValue.getValueType()) {
            case STRING -> ((JsonString) jsonValue).getString();
            case NUMBER -> toNumber((JsonNumber) jsonValue);
            case TRUE -> Boolean.TRUE;
            case FALSE -> Boolean.FALSE;
            case NULL -> null;
            case ARRAY -> toList((JsonArray) jsonValue);
            case OBJECT -> toMap((JsonObject) jsonValue);
        };
    }

    private static Number toNumber(JsonNumber jsonNumber) {
        if (jsonNumber.isIntegral()) {
            return jsonNumber.longValue();
        }
        return jsonNumber.doubleValue();
    }

    private static List<Object> toList(JsonArray jsonArray) {
        List<Object> values = new ArrayList<>(jsonArray.size());
        for (JsonValue element : jsonArray) {
            values.add(toJavaType(element));
        }
        return values;
    }

    private static Map<String, Object> toMap(JsonObject jsonObject) {
        Map<String, Object> values = new LinkedHashMap<>();
        jsonObject.forEach((key, value) -> values.put(key, toJavaType(value)));
        return values;
    }

}
