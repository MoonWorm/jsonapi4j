package pro.api4.jsonapi4j.sampleapp.domain.user;

import pro.api4.jsonapi4j.domain.ResourceRef;
import pro.api4.jsonapi4j.domain.ToManyRefRelationship;
import pro.api4.jsonapi4j.domain.annotation.JsonApiRelationship;
import pro.api4.jsonapi4j.plugin.oas.domain.annotation.OasRelationshipInfo;
import pro.api4.jsonapi4j.sampleapp.domain.country.CountryResource;

import static pro.api4.jsonapi4j.sampleapp.domain.user.UserCitizenshipsRelationship.CITIZENSHIPS;

@JsonApiRelationship(relationshipName = CITIZENSHIPS, parentResource = UserResource.class)
@OasRelationshipInfo(
        relationshipTypes = {CountryResource.class}
)
public class UserCitizenshipsRelationship implements ToManyRefRelationship<ResourceRef> {

    public static final String CITIZENSHIPS = "citizenships";

}
