package pro.api4.jsonapi4j.sampleapp.domain.user;

import pro.api4.jsonapi4j.domain.ToOneRelationship;
import pro.api4.jsonapi4j.domain.annotation.JsonApiRelationship;
import pro.api4.jsonapi4j.plugin.oas.domain.annotation.OasRelationshipInfo;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.country.CountryRef;
import pro.api4.jsonapi4j.sampleapp.domain.country.CountryResource;

import static pro.api4.jsonapi4j.sampleapp.domain.country.CountryResource.COUNTRIES;
import static pro.api4.jsonapi4j.sampleapp.domain.user.UserPlaceOfBirthRelationship.PLACE_OF_BIRTH;

@JsonApiRelationship(relationshipName = PLACE_OF_BIRTH, parentResource = UserResource.class)
@OasRelationshipInfo(
        relationshipTypes = {CountryResource.class}
)
public class UserPlaceOfBirthRelationship implements ToOneRelationship<CountryRef> {

    public static final String PLACE_OF_BIRTH = "placeOfBirth";

    @Override
    public String resolveResourceIdentifierType(CountryRef countryRef) {
        return COUNTRIES;
    }

    @Override
    public String resolveResourceIdentifierId(CountryRef countryRef) {
        return countryRef.id();
    }

}
