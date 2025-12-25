package pro.api4.jsonapi4j.sampleapp.domain.user.jsonapi;

import org.springframework.stereotype.Component;
import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.domain.plugin.oas.model.OasResourceInfo;
import pro.api4.jsonapi4j.sampleapp.config.datasource.userdb.UserDbEntity;
import pro.api4.jsonapi4j.sampleapp.domain.country.jsonapi.CountryResource;
import pro.api4.jsonapi4j.sampleapp.domain.user.UserAttributes;

import static pro.api4.jsonapi4j.sampleapp.domain.SampleAppDomainResourceTypes.USERS;

@OasResourceInfo(
        attributes = UserAttributes.class,
        includes = {CountryResource.class}
)
@Component
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
