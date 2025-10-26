package pro.api4.jsonapi4j.sampleapp.domain.user.jsonapi;

import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.domain.plugin.oas.ResourceOasPlugin;
import pro.api4.jsonapi4j.plugin.ResourcePlugin;
import pro.api4.jsonapi4j.sampleapp.config.datasource.userdb.UserDbEntity;
import pro.api4.jsonapi4j.sampleapp.domain.country.jsonapi.CountryJsonApiResource;
import pro.api4.jsonapi4j.sampleapp.domain.user.UserAttributes;
import org.springframework.stereotype.Component;

import java.util.List;

import static pro.api4.jsonapi4j.sampleapp.domain.SampleAppDomainResourceTypes.USERS;

@Component
public class UserJsonApiResource implements Resource<UserAttributes, UserDbEntity> {

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
                userDbEntity.getFullName().split("\\s+")[0],
                userDbEntity.getFullName().split("\\s+")[1],
                userDbEntity.getEmail(),
                userDbEntity.getCreditCardNumber()
        );
    }

    @Override
    public List<ResourcePlugin<?>> plugins() {
        return List.of(
                ResourceOasPlugin.builder()
                        .attributes(UserAttributes.class)
                        .includes(List.of(
                                CountryJsonApiResource.class
                        )).build()
        );
    }
}
