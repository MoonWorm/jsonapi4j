package pro.api4.jsonapi4j.sampleapp.domain.user;

import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.domain.annotation.JsonApiResource;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControl;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlEntitlements;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlPolicy;
import pro.api4.jsonapi4j.plugin.ac.annotation.EntitlementsGroup;
import pro.api4.jsonapi4j.plugin.oas.domain.annotation.OasResourceInfo;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.AddressRow;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.UserDbEntity;

import java.util.Map;

import static pro.api4.jsonapi4j.principal.entitlement.DefaultEntitlements.ADMIN;
import static pro.api4.jsonapi4j.principal.entitlement.DefaultEntitlements.ROOT_ADMIN;
import static pro.api4.jsonapi4j.sampleapp.domain.user.UserResource.USERS;

@JsonApiResource(resourceType = USERS)
@OasResourceInfo(
        resourceNameSingle = "user",
        attributes = UserAttributes.class
)
public class UserResource implements Resource<UserDbEntity> {

    public static final String USERS = "users";

    @Override
    public String resolveResourceId(UserDbEntity userDbEntity) {
        return userDbEntity.getId();
    }

    @Override
    public UserAttributes resolveAttributes(UserDbEntity userDbEntity) {
        return new UserAttributes(
                userDbEntity.getFirstName() + " " + userDbEntity.getLastName(),
                userDbEntity.getEmail(),
                userDbEntity.getCreditCardNumber(),
                userDbEntity.getAddresses().stream().map(UserResource::toAddress).toList()
        );
    }

    private static Address toAddress(AddressRow row) {
        return row.doorCode() == null
                ? new Address(row.city(), row.zip())
                : new HomeAddress(row.city(), row.zip(), row.doorCode());
    }

    @AccessControl(
            entitlements = @AccessControlEntitlements(
                    mode = AccessControlEntitlements.Mode.ANY_OF,
                    description = "a platform administrator, or HR cleared for personal data",
                    value = {
                            @EntitlementsGroup(value = {ADMIN, ROOT_ADMIN}, mode = EntitlementsGroup.Mode.ANY_OF),
                            @EntitlementsGroup(value = {"DEPARTMENT_HR", "PII_CLEARED"}, mode = EntitlementsGroup.Mode.ALL_OF)
                    }),
            policy = @AccessControlPolicy(
                    value = SingleUserLookupPolicy.class,
                    description = "only when looking up a single user"))
    @Override
    public Object resolveResourceMeta(JsonApiRequest request, UserDbEntity userDbEntity) {
        return Map.of("internalUserRef", "internal-" + userDbEntity.getId());
    }

}
