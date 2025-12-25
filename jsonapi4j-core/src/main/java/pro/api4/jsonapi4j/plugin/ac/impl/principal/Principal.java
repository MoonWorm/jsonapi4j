package pro.api4.jsonapi4j.plugin.ac.impl.principal;

import pro.api4.jsonapi4j.plugin.ac.impl.tier.AccessTier;

import java.util.Set;

public interface Principal {

    String authenticatedUserId();

    AccessTier authenticatedClientAccessTier();

    Set<String> authenticatedClientScopes();

}
