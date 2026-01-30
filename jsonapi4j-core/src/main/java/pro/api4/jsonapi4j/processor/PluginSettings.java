package pro.api4.jsonapi4j.processor;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import pro.api4.jsonapi4j.plugin.JsonApi4jPlugin;
import pro.api4.jsonapi4j.plugin.JsonApiPluginInfo;

@Builder
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PluginSettings {

    private final JsonApi4jPlugin plugin;
    private final JsonApiPluginInfo info;

}
