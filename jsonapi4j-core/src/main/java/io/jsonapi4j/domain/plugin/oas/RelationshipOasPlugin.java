package io.jsonapi4j.domain.plugin.oas;

import io.jsonapi4j.domain.ResourceType;
import io.jsonapi4j.plugin.RelationshipPlugin;
import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.Set;

@Builder
@Getter
public class RelationshipOasPlugin implements RelationshipPlugin<RelationshipOasPlugin> {

    @Builder.Default
    private Class<?> resourceLinkageMetaType = null;
    @Builder.Default
    private Set<ResourceType> relationshipTypes = Collections.emptySet();

    @Override
    public Class<RelationshipOasPlugin> getPluginClass() {
        return RelationshipOasPlugin.class;
    }

}
