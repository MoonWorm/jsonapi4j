package pro.api4.jsonapi4j.ac;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.ac.annotation.AccessControlAccessTier;
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

    private boolean evaluateOwnershipAgainstResourceObject(Object resourceObject,
                                                           AccessControlOwnershipModel ac) {
        if (ac == null || StringUtils.isBlank(ac.getOwnerIdFieldPath())) {
            return true;
        }
        String ownerId = extractOwnerIdFromResourceObject(resourceObject, ac.getOwnerIdFieldPath());
        return evaluateOwnership(ownerId);
    }

    private String extractOwnerIdFromResourceObject(Object resourceObject,
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

    private <REQUEST> String getOwnerIdFromRequest(AccessControlModel accessControlModel,
                                                   REQUEST request) {
        OwnerIdExtractor<REQUEST> ownerIdExtractor = getOwnerIdExtractor(accessControlModel);
        return ownerIdExtractor == null ? null : ownerIdExtractor.fromRequest(request);
    }

    private <REQUEST> OwnerIdExtractor<REQUEST> getOwnerIdExtractor(AccessControlModel accessControlModel) {
        if (accessControlModel.getRequiredOwnership() == null
                || accessControlModel.getRequiredOwnership().getOwnerIdExtractor() == null) {
            return null;
        }
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

    private boolean evaluateOwnership(String currentResourceOwnerId) {
        String authenticatedUserId = AuthenticatedPrincipalContextHolder.getAuthenticatedUserId().orElse(null);
        if (currentResourceOwnerId != null) {
            return currentResourceOwnerId.equals(authenticatedUserId);
        }
        return true;
    }

    private boolean checkIsAuthenticated(AccessControlAuthenticatedModel ac) {
        if (ac == null
                || ac.getAuthenticated() == Authenticated.ANONYMOUS
                || ac.getAuthenticated() == Authenticated.NOT_SET
        ) {
            return true;
        }
        return AuthenticatedPrincipalContextHolder.getAuthenticatedUserId().isPresent();
    }

    private boolean evaluateAccessTier(AccessControlAccessTierModel ac) {
        if (ac == null
                || ac.getRequiredAccessTier() == null
                || AccessControlAccessTier.NOT_SET.equals(ac.getRequiredAccessTier())) {
            return true;
        }
        AccessTier expectedAccessTier = accessTierRegistry.getAccessTier(ac.getRequiredAccessTier());
        if (expectedAccessTier == null) {
            throw new IllegalArgumentException("Invalid value is set for an AccessTier: " + ac.getRequiredAccessTier());
        }
        Optional<AccessTier> actualAccessTier = AuthenticatedPrincipalContextHolder.getAccessTier();
        return actualAccessTier
                .filter(accessTier -> accessTier.compareTo(expectedAccessTier) >= 0)
                .isPresent();
    }

    private boolean evaluateScopes(AccessControlScopesModel ac) {
        if (ac == null) {
            return true;
        }
        String expectedScopesExpression = ac.getRequiredScopesExpression();
        Set<String> expectedScopes = ac.getRequiredScopes();
        Set<String> actualScopes = AuthenticatedPrincipalContextHolder.getScopes().orElse(null);

        // no scopes requirements
        if (StringUtils.isBlank(expectedScopesExpression) && CollectionUtils.isEmpty(expectedScopes)) {
            return true;
        }

        // no info about the current request's Scopes
        if (CollectionUtils.isEmpty(actualScopes)) {
            return false;
        }

        if (StringUtils.isNotBlank(expectedScopesExpression)) {
            // scopes expression has higher priority
            return ScopesUtils.matches(actualScopes, expectedScopesExpression);
        } else {
            // list of scopes has lower priority
            return ScopesUtils.matches(
                    actualScopes,
                    ScopesUtils.toScopesExpression(expectedScopes)
            );
        }
    }

}
