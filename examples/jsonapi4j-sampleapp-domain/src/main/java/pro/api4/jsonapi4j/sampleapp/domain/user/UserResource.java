package pro.api4.jsonapi4j.sampleapp.domain.user;

import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.domain.plugin.oas.model.OasResourceInfo;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.UserDbEntity;

import static pro.api4.jsonapi4j.sampleapp.domain.SampleAppDomainResourceTypes.USERS;

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
    public ResourceType resourceType() {
        return USERS;
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
