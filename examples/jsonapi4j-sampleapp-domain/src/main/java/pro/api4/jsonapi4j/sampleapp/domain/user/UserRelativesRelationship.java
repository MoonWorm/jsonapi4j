package pro.api4.jsonapi4j.sampleapp.domain.user;

import pro.api4.jsonapi4j.domain.ToManyRefRelationship;
import pro.api4.jsonapi4j.domain.annotation.JsonApiRelationship;
import pro.api4.jsonapi4j.plugin.oas.domain.annotation.OasRelationshipInfo;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.RelativeRef;

import static pro.api4.jsonapi4j.sampleapp.domain.user.UserRelativesRelationship.RELATIVES;

@JsonApiRelationship(relationshipName = RELATIVES, parentResource = UserResource.class)
@OasRelationshipInfo(
        relationshipTypes = {UserResource.class},
        resourceLinkageMetaType = RelativeLinkageMeta.class
)
public class UserRelativesRelationship implements ToManyRefRelationship<RelativeRef> {

    public static final String RELATIVES = "relatives";

    @Override
    public RelativeLinkageMeta resolveResourceIdentifierMeta(JsonApiRequest relationshipRequest, RelativeRef relativeRef) {
        return new RelativeLinkageMeta(relativeRef.relationshipType());
    }

}
