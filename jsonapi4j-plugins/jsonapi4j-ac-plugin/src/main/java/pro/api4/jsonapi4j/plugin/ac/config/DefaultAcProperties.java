package pro.api4.jsonapi4j.plugin.ac.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collections;
import java.util.Map;

@NoArgsConstructor
@Getter
@Setter
public class DefaultAcProperties implements AcProperties {

    private boolean enabled = Boolean.parseBoolean(AcProperties.DEFAULT_ENABLED);

    @Override
    public boolean enabled() {
        return enabled;
    }

    public static AcProperties toAcProperties(Map<String, Object> jsonApi4jPropertiesRaw) {
        Object acPropertiesObject = jsonApi4jPropertiesRaw.get(AcProperties.AC_PROPERTY_NAME);
        Map<String, Object> acPropertiesRaw = Collections.emptyMap();
        if (acPropertiesObject instanceof Map acPropertiesMap) {
            //noinspection unchecked
            acPropertiesRaw = acPropertiesMap;
        }

        DefaultAcProperties acProperties = new DefaultAcProperties();
        Object enabledObject = acPropertiesRaw.get("enabled");
        if (enabledObject instanceof Boolean enabledBoolean) {
            acProperties.setEnabled(enabledBoolean);
        }
        return acProperties;
    }

}
