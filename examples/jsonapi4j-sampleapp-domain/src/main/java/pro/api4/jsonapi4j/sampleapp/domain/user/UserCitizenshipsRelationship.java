package pro.api4.jsonapi4j.sampleapp.domain.user;

import pro.api4.jsonapi4j.domain.ToManyRelationship;
import pro.api4.jsonapi4j.domain.annotation.JsonApiRelationship;
import pro.api4.jsonapi4j.domain.plugin.oas.model.OasRelationshipInfo;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.country.DownstreamCountry;
import pro.api4.jsonapi4j.sampleapp.domain.country.CountryResource;

@JsonApiRelationship(relationshipName = "citizenships", parentResource = UserResource.class)
@OasRelationshipInfo(
        relationshipTypes = {CountryResource.class}
)
public class UserCitizenshipsRelationship implements ToManyRelationship<DownstreamCountry> {

    @Override
    public String resolveResourceIdentifierType(DownstreamCountry downstreamCountry) {
        return "countries";
    }

    @Override
    public String resolveResourceIdentifierId(DownstreamCountry downstreamCountry) {
        return downstreamCountry.getCca2();
    }

}
