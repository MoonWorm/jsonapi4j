package pro.api4.jsonapi4j.plugin.sf.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import pro.api4.jsonapi4j.config.RawConfigAccessor;

import java.util.Map;

@NoArgsConstructor
@Getter
@Setter
public class DefaultSfProperties implements SfProperties {

    private boolean enabled = Boolean.parseBoolean(SfProperties.DEFAULT_SF_ENABLED);
    private RequestedFieldsDontExistMode requestedFieldsDontExistMode
            = RequestedFieldsDontExistMode.valueOf(DEFAULT_REQUESTED_FIELDS_DONT_EXIST_MODE);

    @Override
    public boolean enabled() {
        return enabled;
    }

    @Override
    public RequestedFieldsDontExistMode requestedFieldsDontExistMode() {
        return requestedFieldsDontExistMode;
    }

    public static SfProperties toSfProperties(Map<String, Object> jsonApi4jPropertiesRaw) {
        DefaultSfProperties sfProperties = new DefaultSfProperties();
        new RawConfigAccessor(jsonApi4jPropertiesRaw)
                .section(SfProperties.SF_PROPERTY)
                .ifPresent(sf -> {
                    sf.boolValue(SfProperties.ENABLED_PROPERTY).ifPresent(sfProperties::setEnabled);
                    sf.strValue(SfProperties.REQUESTED_FIELDS_DONT_EXIST_MODE_PROPERTY)
                            .map(RequestedFieldsDontExistMode::valueOf)
                            .ifPresent(sfProperties::setRequestedFieldsDontExistMode);
                });
        return sfProperties;
    }

}
