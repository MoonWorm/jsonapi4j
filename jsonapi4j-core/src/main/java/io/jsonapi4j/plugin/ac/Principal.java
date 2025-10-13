package io.jsonapi4j.plugin.ac;

import io.jsonapi4j.plugin.ac.tier.AccessTier;

import java.util.Set;

public interface Principal {

    String getAuthenticatedUserId();

    AccessTier getAuthenticatedClientAccessTier();

    Set<String> getAuthenticatedClientScopes();

}
