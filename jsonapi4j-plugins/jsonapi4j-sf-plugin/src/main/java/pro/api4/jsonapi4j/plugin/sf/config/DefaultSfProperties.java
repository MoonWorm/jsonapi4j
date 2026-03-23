package pro.api4.jsonapi4j.plugin.sf.config;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collections;
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
        Object sfPropertiesObject = jsonApi4jPropertiesRaw.get(SfProperties.SF_PROPERTY_NAME);
        Map<String, Object> sfPropertiesRaw = Collections.emptyMap();
        if (sfPropertiesObject instanceof Map sfPropertiesMap) {
            //noinspection unchecked
            sfPropertiesRaw = sfPropertiesMap;
        }

        DefaultSfProperties sfProperties = new DefaultSfProperties();
        Object enabledObject = sfPropertiesRaw.get("enabled");
        if (enabledObject instanceof String enabledString) {
            sfProperties.setEnabled(Boolean.parseBoolean(enabledString));
        }
        Object requestedFieldsDontExistModeObject = sfPropertiesRaw.get("requestedFieldsDontExistMode");
        if (requestedFieldsDontExistModeObject instanceof String requestedFieldsDontExistModeString) {
            sfProperties.setRequestedFieldsDontExistMode(RequestedFieldsDontExistMode.valueOf(requestedFieldsDontExistModeString));
        }
        return sfProperties;
    }

}
