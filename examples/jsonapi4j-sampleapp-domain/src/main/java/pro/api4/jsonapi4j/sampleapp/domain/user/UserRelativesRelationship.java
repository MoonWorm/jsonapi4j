package pro.api4.jsonapi4j.sampleapp.domain.user;

import pro.api4.jsonapi4j.domain.ToManyRelationship;
import pro.api4.jsonapi4j.domain.annotation.JsonApiRelationship;
import pro.api4.jsonapi4j.plugin.oas.domain.annotation.OasRelationshipInfo;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.RelativeRef;

import static pro.api4.jsonapi4j.sampleapp.domain.user.UserRelativesRelationship.RELATIVES;
import static pro.api4.jsonapi4j.sampleapp.domain.user.UserResource.USERS;

@JsonApiRelationship(relationshipName = RELATIVES, parentResource = UserResource.class)
@OasRelationshipInfo(
        relationshipTypes = {UserResource.class},
        resourceLinkageMetaType = RelativeLinkageMeta.class
)
public class UserRelativesRelationship implements ToManyRelationship<RelativeRef> {

    public static final String RELATIVES = "relatives";

    @Override
    public String resolveResourceIdentifierType(RelativeRef userRelationshipInfo) {
        return USERS;
    }

    @Override
    public String resolveResourceIdentifierId(RelativeRef userRelationshipInfo) {
        return userRelationshipInfo.getRelativeUserId();
    }

    @Override
    public Object resolveResourceIdentifierMeta(JsonApiRequest relationshipRequest, RelativeRef userRelationshipInfo) {
        return new RelativeLinkageMeta(userRelationshipInfo.getRelationshipType());
    }

}
