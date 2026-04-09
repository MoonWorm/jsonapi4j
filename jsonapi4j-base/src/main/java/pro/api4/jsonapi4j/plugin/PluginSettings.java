package pro.api4.jsonapi4j.plugin;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import pro.api4.jsonapi4j.operation.OperationMeta;

@Builder
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PluginSettings {

    private final OperationMeta operationMeta;
    private final JsonApi4jPlugin plugin;
    private final JsonApiPluginInfo info;

}
