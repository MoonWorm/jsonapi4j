package pro.api4.jsonapi4j.domain.plugin.oas;

import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.plugin.ResourcePlugin;
import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

@Builder
@Getter
public class ResourceOasPlugin implements ResourcePlugin<ResourceOasPlugin> {

    @Builder.Default
    private Class<?> attributes = null;
    @Builder.Default
    private List<Class<? extends Resource<?>>> includes = Collections.emptyList();

    @Override
    public Class<ResourceOasPlugin> getPluginClass() {
        return ResourceOasPlugin.class;
    }

}
