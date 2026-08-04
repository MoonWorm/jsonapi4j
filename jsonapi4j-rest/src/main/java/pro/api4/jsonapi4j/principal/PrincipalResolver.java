package pro.api4.jsonapi4j.principal;

import pro.api4.jsonapi4j.principal.tier.AccessTier;
import jakarta.servlet.ServletRequest;

import java.util.Map;
import java.util.Set;

public interface PrincipalResolver {

    default AccessTier resolveAccessTier(ServletRequest servletRequest) {
        return null;
    }

    default Set<String> resolveScopes(ServletRequest servletRequest) {
        return null;
    }

    default String resolveUserId(ServletRequest servletRequest) {
        return null;
    }

    default Map<String, Object> resolveAttributes(ServletRequest servletRequest) {
        return null;
    }
}
