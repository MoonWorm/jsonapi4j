package pro.api4.jsonapi4j.operation.plugin;

import pro.api4.jsonapi4j.plugin.OperationPlugin;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlRequirements;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class OperationInboundAccessControlPlugin implements OperationPlugin<OperationInboundAccessControlPlugin> {

    @Builder.Default
    private AccessControlRequirements requestAccessControl = null;

    @Override
    public Class<OperationInboundAccessControlPlugin> getPluginClass() {
        return OperationInboundAccessControlPlugin.class;
    }

}
