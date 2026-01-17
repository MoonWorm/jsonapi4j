package pro.api4.jsonapi4j.sampleapp.domain.user;

import pro.api4.jsonapi4j.domain.ToOneRelationship;
import pro.api4.jsonapi4j.domain.annotation.JsonApiRelationship;
import pro.api4.jsonapi4j.domain.plugin.oas.annotation.OasRelationshipInfo;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.country.DownstreamCountry;
import pro.api4.jsonapi4j.sampleapp.domain.country.CountryResource;

@JsonApiRelationship(relationshipName = "placeOfBirth", parentResource = UserResource.class)
@OasRelationshipInfo(
        relationshipTypes = {CountryResource.class}
)
public class UserPlaceOfBirthRelationship implements ToOneRelationship<DownstreamCountry> {

    @Override
    public String resolveResourceIdentifierType(DownstreamCountry downstreamCountry) {
        return "countries";
    }

    @Override
    public String resolveResourceIdentifierId(DownstreamCountry downstreamCountry) {
        return downstreamCountry.getCca2();
    }

}
