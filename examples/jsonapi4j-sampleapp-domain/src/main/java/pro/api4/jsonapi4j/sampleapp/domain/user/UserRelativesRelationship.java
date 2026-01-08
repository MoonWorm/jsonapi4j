package pro.api4.jsonapi4j.sampleapp.domain.user;

import pro.api4.jsonapi4j.domain.ToManyRelationship;
import pro.api4.jsonapi4j.domain.annotation.JsonApiRelationship;
import pro.api4.jsonapi4j.domain.plugin.oas.model.OasRelationshipInfo;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.UserDbEntity;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.UserRelationshipInfo;

@JsonApiRelationship(relationshipName = "relatives", parentResource = UserResource.class)
@OasRelationshipInfo(
        relationshipTypes = {UserResource.class}
)
public class UserRelativesRelationship implements ToManyRelationship<UserDbEntity, UserRelationshipInfo> {

    @Override
    public String resolveResourceIdentifierType(UserRelationshipInfo userDbEntity) {
        return "users";
    }

    @Override
    public String resolveResourceIdentifierId(UserRelationshipInfo userDbEntity) {
        return userDbEntity.getRelativeUserId();
    }

}
