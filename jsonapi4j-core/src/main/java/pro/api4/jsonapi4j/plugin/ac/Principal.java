package pro.api4.jsonapi4j.plugin.ac;

import pro.api4.jsonapi4j.plugin.ac.tier.AccessTier;

import java.util.Set;

public interface Principal {

    String getAuthenticatedUserId();

    AccessTier getAuthenticatedClientAccessTier();

    Set<String> getAuthenticatedClientScopes();

}
