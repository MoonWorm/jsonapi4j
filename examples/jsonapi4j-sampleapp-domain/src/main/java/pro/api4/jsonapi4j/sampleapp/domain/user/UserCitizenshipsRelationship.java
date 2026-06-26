package pro.api4.jsonapi4j.sampleapp.domain.user;

import pro.api4.jsonapi4j.domain.ToManyRelationship;
import pro.api4.jsonapi4j.domain.annotation.JsonApiRelationship;
import pro.api4.jsonapi4j.plugin.oas.domain.annotation.OasRelationshipInfo;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.country.CountryRef;
import pro.api4.jsonapi4j.sampleapp.domain.country.CountryResource;

import static pro.api4.jsonapi4j.sampleapp.domain.country.CountryResource.COUNTRIES;
import static pro.api4.jsonapi4j.sampleapp.domain.user.UserCitizenshipsRelationship.CITIZENSHIPS;

@JsonApiRelationship(relationshipName = CITIZENSHIPS, parentResource = UserResource.class)
@OasRelationshipInfo(
        relationshipTypes = {CountryResource.class}
)
public class UserCitizenshipsRelationship implements ToManyRelationship<CountryRef> {

    public static final String CITIZENSHIPS = "citizenships";

    @Override
    public String resolveResourceIdentifierType(CountryRef countryRef) {
        return COUNTRIES;
    }

    @Override
    public String resolveResourceIdentifierId(CountryRef countryRef) {
        return countryRef.id();
    }

}
