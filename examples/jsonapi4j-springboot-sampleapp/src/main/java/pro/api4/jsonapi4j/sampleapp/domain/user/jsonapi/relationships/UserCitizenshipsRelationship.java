package pro.api4.jsonapi4j.sampleapp.domain.user.jsonapi.relationships;

import pro.api4.jsonapi4j.ac.annotation.AccessControl;
import pro.api4.jsonapi4j.ac.annotation.AccessControlOwnership;
import pro.api4.jsonapi4j.ac.annotation.AccessControlScopes;
import pro.api4.jsonapi4j.ac.annotation.Authenticated;
import pro.api4.jsonapi4j.ac.ownership.ResourceIdFromUrlPathExtractor;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.domain.ToManyRelationship;
import pro.api4.jsonapi4j.domain.plugin.oas.RelationshipOasPlugin;
import pro.api4.jsonapi4j.plugin.RelationshipPlugin;
import pro.api4.jsonapi4j.sampleapp.config.datasource.restcountries.DownstreamCountry;
import pro.api4.jsonapi4j.sampleapp.config.datasource.userdb.UserDbEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

import static pro.api4.jsonapi4j.sampleapp.domain.SampleAppDomainResourceTypes.COUNTRIES;
import static pro.api4.jsonapi4j.sampleapp.domain.SampleAppDomainResourceTypes.USERS;
import static pro.api4.jsonapi4j.sampleapp.domain.user.UserRelationshipsRegistry.USER_CITIZENSHIPS;

@AccessControl(
        authenticated = Authenticated.AUTHENTICATED,
        scopes = @AccessControlScopes(requiredScopes = {"users.citizenships.read"}),
        ownership = @AccessControlOwnership(ownerIdExtractor = ResourceIdFromUrlPathExtractor.class)
)
@Component
public class UserCitizenshipsRelationship implements ToManyRelationship<UserDbEntity, DownstreamCountry> {

    @Override
    public RelationshipName relationshipName() {
        return USER_CITIZENSHIPS;
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

    @Override
    public List<RelationshipPlugin<?>> plugins() {
        return List.of(
                RelationshipOasPlugin.builder()
                        .relationshipTypes(Set.of(COUNTRIES))
                        .build()
        );
    }

}
