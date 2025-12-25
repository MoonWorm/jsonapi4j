package pro.api4.jsonapi4j.sampleapp.domain.user.jsonapi.relationships;

import org.springframework.stereotype.Component;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.domain.ToManyRelationship;
import pro.api4.jsonapi4j.sampleapp.config.datasource.userdb.UserDbEntity;

import static pro.api4.jsonapi4j.sampleapp.domain.SampleAppDomainResourceTypes.USERS;
import static pro.api4.jsonapi4j.sampleapp.domain.user.UserRelationshipsRegistry.USER_RELATIVES;

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

    /*@Override
    public List<RelationshipPlugin<?>> plugins() {
        return List.of(
                RelationshipOasPlugin.builder()
                        .relationshipTypes(Set.of(USERS))
                        .build()
        );
    }*/

}
