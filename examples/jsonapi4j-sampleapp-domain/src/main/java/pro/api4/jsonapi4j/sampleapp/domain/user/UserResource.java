package pro.api4.jsonapi4j.sampleapp.domain.user;

import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.domain.annotation.JsonApiResource;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControl;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlEntitlements;
import pro.api4.jsonapi4j.plugin.ac.annotation.EntitlementsGroup;
import pro.api4.jsonapi4j.plugin.oas.domain.annotation.OasResourceInfo;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.UserDbEntity;

import java.util.Map;

import static pro.api4.jsonapi4j.principal.entitlement.DefaultEntitlements.ADMIN;
import static pro.api4.jsonapi4j.principal.entitlement.DefaultEntitlements.PARTNER;
import static pro.api4.jsonapi4j.principal.entitlement.DefaultEntitlements.PUBLIC;
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
                userDbEntity.getCreditCardNumber()
        );
    }

    /**
     * Internal bookkeeping exposed only to an administrator acting on behalf of a partner integration.
     * <p>
     * The requirement is deliberately two-level, and each clause uses a different mode: the caller must
     * hold {@code ADMIN} <em>or</em> {@code ROOT_ADMIN}, <em>and</em> must hold both {@code PARTNER}
     * <em>and</em> {@code PUBLIC}. Callers that fail either clause simply receive no {@code meta} member.
     */
    @AccessControl(entitlements = @AccessControlEntitlements(
            description = "internal administrator acting for a partner integration",
            value = {
                    @EntitlementsGroup(value = {ADMIN, ROOT_ADMIN}, mode = EntitlementsGroup.Mode.ANY_OF),
                    @EntitlementsGroup(value = {PARTNER, PUBLIC}, mode = EntitlementsGroup.Mode.ALL_OF)
            }))
    @Override
    public Object resolveResourceMeta(JsonApiRequest request, UserDbEntity userDbEntity) {
        return Map.of("internalUserRef", "internal-" + userDbEntity.getId());
    }

}
