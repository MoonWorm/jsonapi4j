package pro.api4.jsonapi4j.sampleapp.domain.country;

import java.util.Arrays;

public enum Region {
    europe,
    africa,
    asia;

    public static Region fromName(String name) {
        return Arrays.stream(values())
                .filter(r -> r.name().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }
}
