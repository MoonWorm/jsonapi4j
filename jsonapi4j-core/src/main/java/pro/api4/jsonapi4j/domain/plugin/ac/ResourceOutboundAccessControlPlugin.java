package pro.api4.jsonapi4j.domain.plugin.ac;

import pro.api4.jsonapi4j.plugin.ResourcePlugin;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlRequirementsForObject;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ResourceOutboundAccessControlPlugin implements ResourcePlugin<ResourceOutboundAccessControlPlugin> {

    @Builder.Default
    private AccessControlRequirementsForObject resourceAccessControl = null;

    @Override
    public Class<ResourceOutboundAccessControlPlugin> getPluginClass() {
        return ResourceOutboundAccessControlPlugin.class;
    }

}
