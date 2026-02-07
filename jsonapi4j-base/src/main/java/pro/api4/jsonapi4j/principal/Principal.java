package pro.api4.jsonapi4j.principal;

import pro.api4.jsonapi4j.principal.tier.AccessTier;

import java.util.Set;

public interface Principal {

    String authenticatedUserId();

    AccessTier authenticatedClientAccessTier();

    Set<String> authenticatedClientScopes();

}
