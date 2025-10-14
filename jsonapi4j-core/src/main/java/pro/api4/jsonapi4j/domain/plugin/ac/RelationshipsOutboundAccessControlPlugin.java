package pro.api4.jsonapi4j.domain.plugin.ac;

import pro.api4.jsonapi4j.plugin.RelationshipPlugin;

import pro.api4.jsonapi4j.plugin.ac.model.AccessControlRequirementsForObject;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class RelationshipsOutboundAccessControlPlugin implements RelationshipPlugin<RelationshipsOutboundAccessControlPlugin> {

    @Builder.Default
    private AccessControlRequirementsForObject resourceIdentifierAccessControl = null;

    @Override
    public Class<RelationshipsOutboundAccessControlPlugin> getPluginClass() {
        return RelationshipsOutboundAccessControlPlugin.class;
    }

}
