package pro.api4.jsonapi4j.sampleapp.domain.user;

import pro.api4.jsonapi4j.domain.ToManyRelationship;
import pro.api4.jsonapi4j.domain.annotation.JsonApiRelationship;
import pro.api4.jsonapi4j.plugin.oas.domain.annotation.OasRelationshipInfo;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.UserRelationshipInfo;

import java.util.Map;

@JsonApiRelationship(relationshipName = "relatives", parentResource = UserResource.class)
@OasRelationshipInfo(
        relationshipTypes = {UserResource.class}
)
public class UserRelativesRelationship implements ToManyRelationship<UserRelationshipInfo> {

    @Override
    public String resolveResourceIdentifierType(UserRelationshipInfo userRelationshipInfo) {
        return "users";
    }

    @Override
    public String resolveResourceIdentifierId(UserRelationshipInfo userRelationshipInfo) {
        return userRelationshipInfo.getRelativeUserId();
    }

    @Override
    public Object resolveResourceIdentifierMeta(JsonApiRequest relationshipRequest, UserRelationshipInfo userRelationshipInfo) {
        return Map.of("relationshipType", userRelationshipInfo.getRelationshipType());
    }

}
