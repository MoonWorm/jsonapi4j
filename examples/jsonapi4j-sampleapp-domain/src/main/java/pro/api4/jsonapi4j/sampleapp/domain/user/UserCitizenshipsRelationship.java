package pro.api4.jsonapi4j.sampleapp.domain.user;

import pro.api4.jsonapi4j.domain.ToManyRelationship;
import pro.api4.jsonapi4j.domain.annotation.JsonApiRelationship;
import pro.api4.jsonapi4j.domain.plugin.oas.model.OasRelationshipInfo;
import pro.api4.jsonapi4j.plugin.ac.impl.annotation.AccessControl;
import pro.api4.jsonapi4j.plugin.ac.impl.annotation.AccessControlOwnership;
import pro.api4.jsonapi4j.plugin.ac.impl.annotation.AccessControlScopes;
import pro.api4.jsonapi4j.plugin.ac.impl.annotation.Authenticated;
import pro.api4.jsonapi4j.plugin.ac.impl.ownership.ResourceIdFromUrlPathExtractor;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.country.DownstreamCountry;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.UserDbEntity;
import pro.api4.jsonapi4j.sampleapp.domain.country.CountryResource;

@JsonApiRelationship(relationshipName = "citizenships", parentResource = UserResource.class)
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
    public String resolveResourceIdentifierType(DownstreamCountry downstreamCountry) {
        return "countries";
    }

    @Override
    public String resolveResourceIdentifierId(DownstreamCountry downstreamCountry) {
        return downstreamCountry.getCca2();
    }

}
