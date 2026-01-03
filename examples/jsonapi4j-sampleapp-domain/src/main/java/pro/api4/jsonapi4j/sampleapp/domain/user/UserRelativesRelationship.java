package pro.api4.jsonapi4j.sampleapp.domain.user;

import pro.api4.jsonapi4j.domain.ToManyRelationship;
import pro.api4.jsonapi4j.domain.annotation.JsonApiRelationship;
import pro.api4.jsonapi4j.domain.plugin.oas.model.OasRelationshipInfo;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.UserDbEntity;

@JsonApiRelationship(relationshipName = "relatives", parentResource = UserResource.class)
@OasRelationshipInfo(
        relationshipTypes = {UserResource.class}
)
public class UserRelativesRelationship implements ToManyRelationship<UserDbEntity, UserDbEntity> {

    @Override
    public String resolveResourceIdentifierType(UserDbEntity userDbEntity) {
        return "users";
    }

    @Override
    public String resolveResourceIdentifierId(UserDbEntity userDbEntity) {
        return userDbEntity.getId();
    }

}
