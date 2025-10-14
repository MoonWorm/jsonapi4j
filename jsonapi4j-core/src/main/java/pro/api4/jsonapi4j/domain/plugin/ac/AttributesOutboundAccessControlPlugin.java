package pro.api4.jsonapi4j.domain.plugin.ac;

import pro.api4.jsonapi4j.plugin.ResourcePlugin;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlRequirementsForObject;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class AttributesOutboundAccessControlPlugin implements ResourcePlugin<AttributesOutboundAccessControlPlugin> {

    @Builder.Default
    private AccessControlRequirementsForObject attributesAccessControl = null;

    @Override
    public Class<AttributesOutboundAccessControlPlugin> getPluginClass() {
        return AttributesOutboundAccessControlPlugin.class;
    }

}
