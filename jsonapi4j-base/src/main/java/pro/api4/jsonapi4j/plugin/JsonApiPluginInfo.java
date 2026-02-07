package pro.api4.jsonapi4j.plugin;

import lombok.Data;

@Data
public class JsonApiPluginInfo {

    private final Object operationPluginInfo;
    private final Object resourcePluginInfo;
    private final Object relationshipPluginInfo;

}
