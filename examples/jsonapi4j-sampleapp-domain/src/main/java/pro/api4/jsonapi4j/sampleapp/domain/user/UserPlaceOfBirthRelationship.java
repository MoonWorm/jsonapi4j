package pro.api4.jsonapi4j.sampleapp.domain.user;

import pro.api4.jsonapi4j.domain.ResourceRef;
import pro.api4.jsonapi4j.domain.ToOneRefRelationship;
import pro.api4.jsonapi4j.domain.annotation.JsonApiRelationship;
import pro.api4.jsonapi4j.plugin.oas.domain.annotation.OasRelationshipInfo;
import pro.api4.jsonapi4j.sampleapp.domain.country.CountryResource;

import static pro.api4.jsonapi4j.sampleapp.domain.user.UserPlaceOfBirthRelationship.PLACE_OF_BIRTH;

@JsonApiRelationship(relationshipName = PLACE_OF_BIRTH, parentResource = UserResource.class)
@OasRelationshipInfo(
        relationshipTypes = {CountryResource.class}
)
public class UserPlaceOfBirthRelationship implements ToOneRefRelationship<ResourceRef> {

    public static final String PLACE_OF_BIRTH = "placeOfBirth";

}
