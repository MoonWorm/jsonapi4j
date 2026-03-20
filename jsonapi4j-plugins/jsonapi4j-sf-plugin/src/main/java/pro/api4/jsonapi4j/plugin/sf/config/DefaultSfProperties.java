package pro.api4.jsonapi4j.plugin.sf.config;

import lombok.Getter;
import lombok.Setter;

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

}
