package pro.api4.jsonapi4j.sampleapp.domain.user.jsonapi.relationships;

import org.springframework.stereotype.Component;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.domain.ToOneRelationship;
import pro.api4.jsonapi4j.sampleapp.config.datasource.restcountries.DownstreamCountry;
import pro.api4.jsonapi4j.sampleapp.config.datasource.userdb.UserDbEntity;

import static pro.api4.jsonapi4j.sampleapp.domain.SampleAppDomainResourceTypes.COUNTRIES;
import static pro.api4.jsonapi4j.sampleapp.domain.SampleAppDomainResourceTypes.USERS;
import static pro.api4.jsonapi4j.sampleapp.domain.user.UserRelationshipsRegistry.USER_PLACE_OF_BIRTH;

@Component
public class UserPlaceOfBirthRelationship implements ToOneRelationship<UserDbEntity, DownstreamCountry> {

    @Override
    public RelationshipName relationshipName() {
        return USER_PLACE_OF_BIRTH;
    }

    @Override
    public ResourceType resourceType() {
        return USERS;
    }

    @Override
    public ResourceType resolveResourceIdentifierType(DownstreamCountry downstreamCountry) {
        return COUNTRIES;
    }

    @Override
    public String resolveResourceIdentifierId(DownstreamCountry downstreamCountry) {
        return downstreamCountry.getCca2();
    }

    /*@Override
    public List<RelationshipPlugin<?>> plugins() {
        return List.of(
                RelationshipOasPlugin.builder()
                        .relationshipTypes(Set.of(COUNTRIES))
                        .build()
        );
    }*/

}
