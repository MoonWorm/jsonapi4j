package pro.api4.jsonapi4j.sampleapp.domain.user;

import org.springframework.stereotype.Component;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.domain.ToManyRelationship;
import pro.api4.jsonapi4j.domain.plugin.oas.model.OasRelationshipInfo;
import pro.api4.jsonapi4j.sampleapp.config.datasource.userdb.UserDbEntity;

import static pro.api4.jsonapi4j.sampleapp.domain.SampleAppDomainResourceTypes.USERS;
import static pro.api4.jsonapi4j.sampleapp.domain.user.UserRelationshipsRegistry.USER_RELATIVES;

@OasRelationshipInfo(
        relationshipTypes = {UserResource.class}
)
@Component
public class UserRelativesRelationship implements ToManyRelationship<UserDbEntity, UserDbEntity> {

    @Override
    public RelationshipName relationshipName() {
        return USER_RELATIVES;
    }

    @Override
    public ResourceType resourceType() {
        return USERS;
    }

    @Override
    public ResourceType resolveResourceIdentifierType(UserDbEntity userDbEntity) {
        return USERS;
    }

    @Override
    public String resolveResourceIdentifierId(UserDbEntity userDbEntity) {
        return userDbEntity.getId();
    }

}
