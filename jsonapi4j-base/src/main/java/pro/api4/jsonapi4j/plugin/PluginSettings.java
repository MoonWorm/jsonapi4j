package pro.api4.jsonapi4j.plugin;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PluginSettings {

    private final JsonApi4jPlugin plugin;
    private final JsonApiPluginInfo info;

}
