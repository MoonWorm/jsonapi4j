package pro.api4.jsonapi4j.processor;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.List;

// TODO remove class? extract just plugins
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class RelationshipProcessorContext {

    @Builder.Default
    private List<PluginSettings> plugins = Collections.emptyList();

}
