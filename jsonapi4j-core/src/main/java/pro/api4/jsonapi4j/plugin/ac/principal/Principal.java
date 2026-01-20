package pro.api4.jsonapi4j.plugin.ac.principal;

import pro.api4.jsonapi4j.plugin.ac.tier.AccessTier;

import java.util.Set;

public interface Principal {

    String authenticatedUserId();

    AccessTier authenticatedClientAccessTier();

    Set<String> authenticatedClientScopes();

}
