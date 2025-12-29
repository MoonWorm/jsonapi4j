package pro.api4.jsonapi4j.sampleapp.domain.user;

import pro.api4.jsonapi4j.domain.plugin.oas.model.OasRelationshipInfo;
import pro.api4.jsonapi4j.plugin.ac.impl.annotation.AccessControl;
import pro.api4.jsonapi4j.plugin.ac.impl.annotation.AccessControlOwnership;
import pro.api4.jsonapi4j.plugin.ac.impl.annotation.AccessControlScopes;
import pro.api4.jsonapi4j.plugin.ac.impl.annotation.Authenticated;
import pro.api4.jsonapi4j.plugin.ac.impl.ownership.ResourceIdFromUrlPathExtractor;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.domain.ToManyRelationship;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.country.DownstreamCountry;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.UserDbEntity;
import pro.api4.jsonapi4j.sampleapp.domain.country.CountryResource;

import static pro.api4.jsonapi4j.sampleapp.domain.SampleAppDomainResourceTypes.COUNTRIES;
import static pro.api4.jsonapi4j.sampleapp.domain.SampleAppDomainResourceTypes.USERS;
import static pro.api4.jsonapi4j.sampleapp.domain.user.UserRelationshipsRegistry.USER_CITIZENSHIPS;

@AccessControl(
        authenticated = Authenticated.AUTHENTICATED,
        scopes = @AccessControlScopes(requiredScopes = {"users.citizenships.read"}),
        ownership = @AccessControlOwnership(ownerIdExtractor = ResourceIdFromUrlPathExtractor.class)
)
@OasRelationshipInfo(
        relationshipTypes = {CountryResource.class}
)
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

}
