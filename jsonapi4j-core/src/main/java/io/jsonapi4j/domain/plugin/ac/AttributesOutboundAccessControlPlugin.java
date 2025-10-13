package io.jsonapi4j.domain.plugin.ac;

import io.jsonapi4j.plugin.ResourcePlugin;
import io.jsonapi4j.plugin.ac.model.AccessControlRequirementsForObject;
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
