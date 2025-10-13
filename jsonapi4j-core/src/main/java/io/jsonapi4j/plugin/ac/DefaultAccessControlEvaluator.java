package io.jsonapi4j.plugin.ac;

import io.jsonapi4j.plugin.ac.exception.AccessControlMisconfigurationException;
import io.jsonapi4j.plugin.ac.ownership.OwnerIdExtractor;
import io.jsonapi4j.plugin.ac.scope.ScopesUtils;
import io.jsonapi4j.plugin.ac.tier.AccessTier;
import io.jsonapi4j.plugin.ac.tier.AccessTierRegistry;
import io.jsonapi4j.plugin.ac.model.AccessControlAccessTierModel;
import io.jsonapi4j.plugin.ac.model.AccessControlAuthenticatedModel;
import io.jsonapi4j.plugin.ac.model.AccessControlRequirements;
import io.jsonapi4j.plugin.ac.model.AccessControlOwnershipModel;
import io.jsonapi4j.plugin.ac.model.AccessControlScopesModel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Data
public class DefaultAccessControlEvaluator implements AccessControlEvaluator {

    private final AccessTierRegistry accessTierRegistry;

    @Override
    public <REQUEST> boolean evaluateInboundRequirements(REQUEST request,
                                                         AccessControlRequirements accessControlRequirements) {

        if (accessControlRequirements == null) {
            return true;
        }

        String ownerId = getOwnerIdFromRequest(accessControlRequirements, request);

        return checkIsAuthenticated(accessControlRequirements.getRequireAuthenticatedUser())
                && evaluateAccessTier(accessControlRequirements.getRequiredAccessTier())
                && evaluateScopes(accessControlRequirements.getRequiredScopes())
                && evaluateOwnership(ownerId);
    }


    @Override
    public boolean evaluateOutboundRequirements(Object ownerIdHolder,
                                                AccessControlRequirements accessControlRequirements) {
        if (accessControlRequirements == null) {
            return true;
        }
        return checkIsAuthenticated(accessControlRequirements.getRequireAuthenticatedUser())
                && evaluateAccessTier(accessControlRequirements.getRequiredAccessTier())
                && evaluateScopes(accessControlRequirements.getRequiredScopes())
                && evaluateOwnershipAgainstObject(ownerIdHolder, accessControlRequirements.getRequiredOwnership());
    }

    private <REQUEST> String getOwnerIdFromRequest(AccessControlRequirements accessControlRequirements, REQUEST request) {
        OwnerIdExtractor<REQUEST> ownerIdExtractor = getOwnerIdExtractor(accessControlRequirements);
        if (ownerIdExtractor != null) {
            return ownerIdExtractor.fromRequest(request);
        }
        return null;
    }

    private <REQUEST> OwnerIdExtractor<REQUEST> getOwnerIdExtractor(AccessControlRequirements accessControlRequirements) {
        if (accessControlRequirements.getRequiredOwnership() != null
                && accessControlRequirements.getRequiredOwnership().getOwnerIdExtractor() != null) {
            try {
                //noinspection rawtypes
                OwnerIdExtractor ownerIdExtractor = accessControlRequirements
                        .getRequiredOwnership()
                        .getOwnerIdExtractor()
                        .getDeclaredConstructor()
                        .newInstance();
                @SuppressWarnings("unchecked")
                OwnerIdExtractor<REQUEST> ownerIdExtractorForRequest = (OwnerIdExtractor<REQUEST>) ownerIdExtractor;
                return ownerIdExtractorForRequest;
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                     NoSuchMethodException e) {
                throw new AccessControlMisconfigurationException("Failed to instantiate a custom OwnerIdExtractor. It must has the default constructor only.", e);
            }
        }
        return null;
    }

    private static boolean checkIsAuthenticated() {
        return AuthenticatedPrincipalContextHolder.getAuthenticatedUserId().isPresent();
    }

    private static boolean checkIsAuthenticated(AccessControlAuthenticatedModel ac) {
        if (ac == null || !ac.isRequireAuthentication()) {
            return true;
        }
        return checkIsAuthenticated();
    }

    private static boolean evaluateAccessTier(AccessTier expectedAccessTier) {
        Optional<AccessTier> actualAccessTier = AuthenticatedPrincipalContextHolder.getAccessTier();
        return actualAccessTier
                .filter(accessTier -> accessTier.compareTo(expectedAccessTier) >= 0)
                .isPresent();
    }

    private static boolean evaluateScopes(Set<String> actualScopes,
                                          String scopesExpression,
                                          Set<String> requiredScopes) {
        // no scopes requirements
        if (StringUtils.isBlank(scopesExpression) && CollectionUtils.isEmpty(requiredScopes)) {
            return true;
        }

        // no info about the current request's Scopes
        if (CollectionUtils.isEmpty(actualScopes)) {
            return false;
        }

        if (StringUtils.isNotBlank(scopesExpression)) {
            // scopes expression has higher priority
            return ScopesUtils.matches(actualScopes, scopesExpression);
        } else {
            // list of scopes has lower priority
            return ScopesUtils.matches(
                    actualScopes,
                    ScopesUtils.toScopesExpression(requiredScopes)
            );
        }
    }

    private static boolean evaluateScopes(String scopesExpression,
                                          Set<String> requiredScopes) {

        Optional<Set<String>> actualScopes = AuthenticatedPrincipalContextHolder.getScopes();
        return evaluateScopes(actualScopes.orElse(null), scopesExpression, requiredScopes);
    }

    private static boolean evaluateOwnership(String authenticatedUserId,
                                             String currentResourceOwnerId) {
        if (currentResourceOwnerId != null) {
            return currentResourceOwnerId.equals(authenticatedUserId);
        }
        return true;
    }

    private static boolean evaluateOwnership(String currentResourceOwnerId) {
        String authenticatedUserId = AuthenticatedPrincipalContextHolder.getAuthenticatedUserId().orElse(null);
        return evaluateOwnership(authenticatedUserId, currentResourceOwnerId);
    }

    private static boolean evaluateScopes(AccessControlScopesModel ac) {
        if (ac == null) {
            return true;
        }
        return evaluateScopes(ac.getRequiredScopesExpression(), ac.getRequiredScopes());
    }

    private static boolean evaluateOwnershipAgainstObject(Object ownerIdHolder, AccessControlOwnershipModel ac) {
        if (ac == null) {
            return true;
        }
        if (!ac.getOwnerIdFieldPath().trim().isEmpty()) {
            String ownerId = extractOwnerIdFromResource(ownerIdHolder, ac.getOwnerIdFieldPath());
            return evaluateOwnership(ownerId);
        }
        return true;
    }

    private static String extractOwnerIdFromResource(Object ownerIdHolder, String ownerIdFieldName) {
        try {
            Object ownerId = ReflectionUtils.getFieldValue(ownerIdHolder, ownerIdFieldName);
            if (ownerId instanceof String ownerIdStr) {
                return ownerIdStr;
            } else {
                throw new AccessControlMisconfigurationException("Owner ID field must be of type String");
            }
        } catch (RuntimeException e) {
            throw new AccessControlMisconfigurationException("Failed to read 'ownerId' field value", e);
        }
    }

    private boolean evaluateAccessTier(AccessControlAccessTierModel ac) {
        if (ac == null || ac.getRequiredAccessTier() == null) {
            return true;
        }
        AccessTier accessTier = accessTierRegistry.getAccessTier(ac.getRequiredAccessTier());
        if (accessTier == null) {
            throw new IllegalArgumentException("Invalid value is set for an AccessTier: " + ac.getRequiredAccessTier());
        }
        return evaluateAccessTier(accessTier);
    }

}
