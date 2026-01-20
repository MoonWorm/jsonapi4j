package pro.api4.jsonapi4j.sampleapp.domain.user;

import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.domain.annotation.JsonApiResource;
import pro.api4.jsonapi4j.plugin.oas.domain.annotation.OasResourceInfo;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.UserDbEntity;

@JsonApiResource(resourceType = "users")
@OasResourceInfo(
        resourceNameSingle = "user",
        attributes = UserAttributes.class
)
public class UserResource implements Resource<UserDbEntity> {

    @Override
    public String resolveResourceId(UserDbEntity userDbEntity) {
        return userDbEntity.getId();
    }

    @Override
    public UserAttributes resolveAttributes(UserDbEntity userDbEntity) {
        return new UserAttributes(
                userDbEntity.getFirstName() + " " + userDbEntity.getLastName(),
                userDbEntity.getEmail(),
                userDbEntity.getCreditCardNumber()
        );
    }

}
