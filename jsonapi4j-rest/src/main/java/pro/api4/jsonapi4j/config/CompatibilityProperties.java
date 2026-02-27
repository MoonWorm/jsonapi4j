package pro.api4.jsonapi4j.config;

import lombok.Data;
import pro.api4.jsonapi4j.compatibility.JsonApi4jCompatibilityMode;

@Data
public class CompatibilityProperties {

    private boolean legacyMode = false;

    public JsonApi4jCompatibilityMode resolveMode() {
        return JsonApi4jCompatibilityMode.fromLegacyMode(legacyMode);
    }
}
