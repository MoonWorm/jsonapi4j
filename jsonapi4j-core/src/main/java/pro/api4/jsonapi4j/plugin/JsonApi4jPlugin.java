package pro.api4.jsonapi4j.plugin;

import pro.api4.jsonapi4j.domain.Relationship;
import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.operation.Operation;

public interface JsonApi4jPlugin {

    String pluginName();

    default Object extractPluginInfoFromOperation(Operation operation, Class<?> operationClass) {
        return null;
    }

    default Object extractPluginInfoFromResource(Resource<?> resource) {
        return null;
    }

    default Object extractPluginInfoFromRelationship(Relationship<?, ?> relationship) {
        return null;
    }

}
