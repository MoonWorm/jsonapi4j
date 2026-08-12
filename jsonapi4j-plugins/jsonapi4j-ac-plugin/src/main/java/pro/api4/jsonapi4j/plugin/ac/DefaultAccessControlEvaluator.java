package pro.api4.jsonapi4j.plugin.ac;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.util.ReflectionUtils;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlAccessTier;
import pro.api4.jsonapi4j.plugin.ac.annotation.Authenticated;
import pro.api4.jsonapi4j.plugin.ac.exception.AccessControlMisconfigurationException;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlAccessTierModel;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlAuthenticatedModel;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlModel;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlOwnershipModel;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlScopesModel;
import pro.api4.jsonapi4j.plugin.ac.ownership.OwnerIdExtractor;
import pro.api4.jsonapi4j.principal.AuthenticatedPrincipalContextHolder;
import pro.api4.jsonapi4j.plugin.ac.scope.ScopesUtils;
import pro.api4.jsonapi4j.principal.tier.AccessTier;
import pro.api4.jsonapi4j.principal.tier.AccessTierRegistry;

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class DefaultAccessControlEvaluator extends AccessControlEvaluator {

    private final AccessTierRegistry accessTierRegistry;
    private final AtomicBoolean missingAccessTierReported = new AtomicBoolean();

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
            Object ownerId = ReflectionUtils.getFieldValueThrowing(resourceObject, ownerIdFieldName);
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
        if (actualAccessTier.isEmpty()) {
            reportMissingAccessTier(expectedAccessTier);
            return false;
        }
        return actualAccessTier.get().compareTo(expectedAccessTier) >= 0;
    }

    /**
     * Reports a principal that passed authentication but carries no access tier at all.
     * <p>
     * Such a principal fails every access tier requirement no matter which tier is asked for, which almost
     * always means the configured {@code PrincipalResolver} produces no tier — a JWT resolver whose tier
     * claim is absent from the tokens being issued, most commonly. It is reported once per process at
     * {@code WARN}, and on every occurrence at {@code DEBUG}.
     *
     * @param expectedAccessTier the tier the denied requirement asked for
     */
    private void reportMissingAccessTier(AccessTier expectedAccessTier) {
        if (!isMissingAccessTierMisconfiguration()) {
            return;
        }
        String message = "Access denied: an access tier of '{}' or higher is required, but the "
                + "authenticated principal carries no access tier at all. The configured PrincipalResolver "
                + "resolved none — when using a JWT resolver, check that issued tokens actually carry the "
                + "configured access tier claim. See https://api4.pro/principal-resolution/";
        if (missingAccessTierReported.compareAndSet(false, true)) {
            log.warn(message, expectedAccessTier.getName());
        } else {
            log.debug(message, expectedAccessTier.getName());
        }
    }

    /**
     * Distinguishes a misconfigured principal from an ordinary anonymous one.
     * <p>
     * An anonymous caller legitimately has no access tier, and denying it is the point of the requirement.
     * A caller that authenticated successfully and still has no tier is a configuration problem.
     *
     * @return {@code true} if the current principal is authenticated yet has no access tier
     */
    boolean isMissingAccessTierMisconfiguration() {
        return AuthenticatedPrincipalContextHolder.getAccessTier().isEmpty()
                && AuthenticatedPrincipalContextHolder.getAuthenticatedUserId()
                .filter(StringUtils::isNotBlank)
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
