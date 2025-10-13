package io.jsonapi4j.servlet.filter.ac;

import io.jsonapi4j.plugin.ac.tier.AccessTier;
import jakarta.servlet.ServletRequest;

import java.util.Set;

public interface PrincipalResolver {

    AccessTier resolveAccessTier(ServletRequest servletRequest);

    Set<String> resolveScopes(ServletRequest servletRequest);

    String resolveUserId(ServletRequest servletRequest);

}
