package pro.api4.jsonapi4j.plugin.ac.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pro.api4.jsonapi4j.config.RawConfigAccessor;

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
        DefaultAcProperties acProperties = new DefaultAcProperties();
        new RawConfigAccessor(jsonApi4jPropertiesRaw)
                .section(AcProperties.AC_PROPERTY)
                .flatMap(ac -> ac.boolValue(AcProperties.ENABLED_PROPERTY))
                .ifPresent(acProperties::setEnabled);
        return acProperties;
    }

}
