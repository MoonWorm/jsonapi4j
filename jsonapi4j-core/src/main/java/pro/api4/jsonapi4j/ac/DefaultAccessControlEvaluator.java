package pro.api4.jsonapi4j.ac;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.ac.annotation.Authenticated;
import pro.api4.jsonapi4j.ac.exception.AccessControlMisconfigurationException;
import pro.api4.jsonapi4j.ac.model.AccessControlAccessTierModel;
import pro.api4.jsonapi4j.ac.model.AccessControlAuthenticatedModel;
import pro.api4.jsonapi4j.ac.model.AccessControlModel;
import pro.api4.jsonapi4j.ac.model.AccessControlOwnershipModel;
import pro.api4.jsonapi4j.ac.model.AccessControlScopesModel;
import pro.api4.jsonapi4j.ac.ownership.OwnerIdExtractor;
import pro.api4.jsonapi4j.ac.principal.AuthenticatedPrincipalContextHolder;
import pro.api4.jsonapi4j.ac.scope.ScopesUtils;
import pro.api4.jsonapi4j.ac.tier.AccessTier;
import pro.api4.jsonapi4j.ac.tier.AccessTierRegistry;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.Set;

@Slf4j
public class DefaultAccessControlEvaluator extends AccessControlEvaluator {

    private final AccessTierRegistry accessTierRegistry;

    public DefaultAccessControlEvaluator(AccessTierRegistry accessTierRegistry) {
        this.accessTierRegistry = accessTierRegistry;
    }

    private static boolean checkIsAuthenticated() {
        return AuthenticatedPrincipalContextHolder.getAuthenticatedUserId().isPresent();
    }

    private static boolean checkIsAuthenticated(AccessControlAuthenticatedModel ac) {
        if (ac == null || ac.getAuthenticated() == Authenticated.ANONYMOUS) {
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

    private static boolean evaluateOwnershipAgainstResourceObject(Object resourceObject,
                                                                  AccessControlOwnershipModel ac) {
        if (ac == null) {
            return true;
        }
        if (ac.getOwnerIdFieldPath() != null && !ac.getOwnerIdFieldPath().trim().isEmpty()) {
            String ownerId = extractOwnerIdFromResourceObject(resourceObject, ac.getOwnerIdFieldPath());
            return evaluateOwnership(ownerId);
        }
        return true;
    }

    private static String extractOwnerIdFromResourceObject(Object resourceObject,
                                                           String ownerIdFieldName) {
        try {
            Object ownerId = ReflectionUtils.getFieldValue(resourceObject, ownerIdFieldName);
            if (ownerId instanceof String ownerIdStr) {
                return ownerIdStr;
            } else {
                throw new AccessControlMisconfigurationException("Owner ID field must be of type String");
            }
        } catch (RuntimeException e) {
            throw new AccessControlMisconfigurationException("Failed to read 'ownerId' field value", e);
        }
    }

    @Override
    public <REQUEST> boolean evaluateInboundRequirements(REQUEST request,
                                                         AccessControlModel accessControlModel) {

        if (accessControlModel == null) {
            return true;
        }

        String ownerId = getOwnerIdFromRequest(accessControlModel, request);

        return checkIsAuthenticated(accessControlModel.getAuthenticated())
                && evaluateAccessTier(accessControlModel.getRequiredAccessTier())
                && evaluateScopes(accessControlModel.getRequiredScopes())
                && evaluateOwnership(ownerId);
    }

    @Override
    public boolean evaluateOutboundRequirements(Object resourceObject,
                                                AccessControlModel accessControlModel) {
        if (accessControlModel == null) {
            return true;
        }
        return checkIsAuthenticated(accessControlModel.getAuthenticated())
                && evaluateAccessTier(accessControlModel.getRequiredAccessTier())
                && evaluateScopes(accessControlModel.getRequiredScopes())
                && evaluateOwnershipAgainstResourceObject(resourceObject, accessControlModel.getRequiredOwnership());
    }

    private <REQUEST> String getOwnerIdFromRequest(AccessControlModel accessControlModel,
                                                   REQUEST request) {
        OwnerIdExtractor<REQUEST> ownerIdExtractor = getOwnerIdExtractor(accessControlModel);
        if (ownerIdExtractor != null) {
            return ownerIdExtractor.fromRequest(request);
        }
        return null;
    }

    private <REQUEST> OwnerIdExtractor<REQUEST> getOwnerIdExtractor(AccessControlModel accessControlModel) {
        if (accessControlModel.getRequiredOwnership() != null
                && accessControlModel.getRequiredOwnership().getOwnerIdExtractor() != null) {
            try {
                //noinspection rawtypes
                OwnerIdExtractor ownerIdExtractor = accessControlModel
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
