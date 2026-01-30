package pro.api4.jsonapi4j.plugin;

import pro.api4.jsonapi4j.domain.Relationship;
import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.operation.Operation;
import pro.api4.jsonapi4j.processor.multi.relationship.ToManyRelationshipsJsonApiContext;

public interface JsonApi4jPlugin {

    int HIGHEST_PRECEDENCE = 0;
    int HIGH_PRECEDENCE = 10;
    int LOW_PRECEDENCE = 100;
    int LOWEST_PRECEDENCE = Integer.MAX_VALUE;

    String pluginName();

    default int precedence() {
        return LOW_PRECEDENCE;
    }

    default Object extractPluginInfoFromOperation(Operation operation, Class<?> operationClass) {
        return null;
    }

    default Object extractPluginInfoFromResource(Resource<?> resource) {
        return null;
    }

    default Object extractPluginInfoFromRelationship(Relationship<?> relationship) {
        return null;
    }

    default SingleResourceVisitors singleResourceVisitors() {
        return new SingleResourceVisitors() {
        };
    }

    default ToOneRelationshipVisitors toOneRelationshipVisitors() {
        return new ToOneRelationshipVisitors() {
        };
    }

    default ToManyRelationshipVisitors toManyRelationshipVisitors() {
        return new ToManyRelationshipVisitors() {
        };
    }


}
