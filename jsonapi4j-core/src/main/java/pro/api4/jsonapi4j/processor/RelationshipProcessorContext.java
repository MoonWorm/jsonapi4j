package pro.api4.jsonapi4j.processor;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import pro.api4.jsonapi4j.plugin.PluginSettings;

import java.util.Collections;
import java.util.List;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class RelationshipProcessorContext {

    @Builder.Default
    private List<PluginSettings> plugins = Collections.emptyList();

}
