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

    private boolean failOnMisconfiguration
            = Boolean.parseBoolean(AcProperties.DEFAULT_FAIL_ON_MISCONFIGURATION);

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public boolean failOnMisconfiguration() {
        return failOnMisconfiguration;
    }

    public static AcProperties toAcProperties(Map<String, Object> jsonApi4jPropertiesRaw) {
        DefaultAcProperties acProperties = new DefaultAcProperties();
        RawConfigAccessor rawConfig = new RawConfigAccessor(jsonApi4jPropertiesRaw);
        rawConfig.section(AcProperties.AC_PROPERTY)
                .flatMap(ac -> ac.boolValue(AcProperties.ENABLED_PROPERTY))
                .ifPresent(acProperties::setEnabled);
        rawConfig.section(AcProperties.AC_PROPERTY)
                .flatMap(ac -> ac.boolValue(AcProperties.FAIL_ON_MISCONFIGURATION_PROPERTY))
                .ifPresent(acProperties::setFailOnMisconfiguration);
        return acProperties;
    }

}
