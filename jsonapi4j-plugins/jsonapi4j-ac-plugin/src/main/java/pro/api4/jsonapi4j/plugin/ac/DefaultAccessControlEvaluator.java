package pro.api4.jsonapi4j.plugin.ac;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.plugin.ac.annotation.Authenticated;
import pro.api4.jsonapi4j.plugin.ac.context.AccessControlContext;
import pro.api4.jsonapi4j.plugin.ac.diagnostics.AccessControlDiagnostics;
import pro.api4.jsonapi4j.plugin.ac.exception.AccessControlMisconfigurationException;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlAuthenticatedModel;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlEntitlementsModel;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlModel;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlOwnershipModel;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlPolicyModel;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlScopesModel;
import pro.api4.jsonapi4j.plugin.ac.model.ScopesGroupModel;
import pro.api4.jsonapi4j.plugin.ac.model.EntitlementsGroupModel;
import pro.api4.jsonapi4j.plugin.ac.ownership.OwnerIdExtractor;
import pro.api4.jsonapi4j.principal.AuthenticatedPrincipalContextHolder;
import pro.api4.jsonapi4j.util.ReflectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
public class DefaultAccessControlEvaluator extends AccessControlEvaluator {

    private final AtomicBoolean missingEntitlementsReported = new AtomicBoolean();
    private final AtomicBoolean missingScopesReported = new AtomicBoolean();

    @Override
    public boolean evaluateInboundRequirements(AccessControlContext context,
                                               AccessControlModel accessControlModel) {

        if (accessControlModel == null) {
            return true;
        }

        String ownerId = getOwnerIdFromRequest(accessControlModel, context.request());

        return checkIsAuthenticated(accessControlModel.getAuthenticated())
                && evaluateEntitlements(accessControlModel.getRequiredEntitlements())
                && evaluateScopes(accessControlModel.getRequiredScopes())
                && evaluateOwnership(ownerId)
                && evaluatePolicy(accessControlModel.getRequiredPolicy(), context);
    }

    @Override
    public boolean evaluateOutboundRequirements(AccessControlContext context,
                                                AccessControlModel accessControlModel) {
        if (accessControlModel == null) {
            return true;
        }
        return checkIsAuthenticated(accessControlModel.getAuthenticated())
                && evaluateEntitlements(accessControlModel.getRequiredEntitlements())
                && evaluateScopes(accessControlModel.getRequiredScopes())
                && evaluateOwnershipAgainstResourceObject(context.resource().orElse(null),
                        accessControlModel.getRequiredOwnership())
                && evaluatePolicy(accessControlModel.getRequiredPolicy(), context);
    }

    /**
     * Evaluates a declared {@code AccessPolicy}, last of all the requirements: it is application code and the
     * most expensive to run, so the cheap declarative checks get to deny first.
     *
     * @param expectedPolicy the policy requirement, or {@code null} when none is declared
     * @param context        everything the policy may decide against
     * @return {@code true} when no policy is declared or the declared one allows it
     */
    private boolean evaluatePolicy(AccessControlPolicyModel expectedPolicy, AccessControlContext context) {
        if (expectedPolicy == null) {
            return true;
        }
        if (expectedPolicy.isSatisfiedBy(context)) {
            return true;
        }
        reportUnsatisfiedPolicy(expectedPolicy);
        return false;
    }

    /**
     * Explains a policy denial. Reported at {@code DEBUG} for the same reason as the other ordinary denials:
     * on any endpoint that restricts access this is business as usual.
     *
     * @param expectedPolicy the policy that denied access
     */
    private void reportUnsatisfiedPolicy(AccessControlPolicyModel expectedPolicy) {
        if (!log.isDebugEnabled()) {
            return;
        }
        log.debug("Access denied: {} denied the request. "
                        + "See https://api4.pro/access-control-plugin/",
                describePolicyRequirement(expectedPolicy));
    }

    /**
     * Renders a policy requirement for a log line, e.g. {@code policy com.acme.SameTenantPolicy}, prefixed by
     * a declared {@code description} so that a denial reads as the reason rather than only a class name.
     *
     * @param expectedPolicy the requirement to describe
     * @return human-readable description
     */
    static String describePolicyRequirement(AccessControlPolicyModel expectedPolicy) {
        String structure = String.format("policy %s", expectedPolicy.getPolicy().getClass().getName());
        return StringUtils.isBlank(expectedPolicy.getDescription())
                ? structure
                : String.format("'%s' (%s)", expectedPolicy.getDescription(), structure);
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
                throw AccessControlDiagnostics.ownerIdFieldNotAString();
            }
        } catch (RuntimeException e) {
            throw AccessControlDiagnostics.unreadableOwnerIdField(e);
        }
    }

    private String getOwnerIdFromRequest(AccessControlModel accessControlModel,
                                        Object request) {
        OwnerIdExtractor<Object> ownerIdExtractor = getOwnerIdExtractor(accessControlModel);
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
            throw AccessControlDiagnostics.uninstantiableOwnerIdExtractor(e);
        }
    }

    private boolean evaluateOwnership(String currentResourceOwnerId) {
        String authenticatedUserId = AuthenticatedPrincipalContextHolder.getAuthenticatedUserId().orElse(null);
        if (currentResourceOwnerId != null) {
            boolean satisfied = currentResourceOwnerId.equals(authenticatedUserId);
            if (!satisfied && log.isDebugEnabled()) {
                log.debug("Access denied: ownership of the target resource is required, but it is owned by "
                                + "'{}' while the authenticated user is '{}'. "
                                + "See https://api4.pro/access-control-plugin/",
                        currentResourceOwnerId, authenticatedUserId);
            }
            return satisfied;
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
        boolean satisfied = AuthenticatedPrincipalContextHolder.getAuthenticatedUserId().isPresent();
        if (!satisfied && log.isDebugEnabled()) {
            log.debug("Access denied: an authenticated principal is required, but the request carries none. "
                    + "See https://api4.pro/principal-resolution/");
        }
        return satisfied;
    }

    private boolean evaluateEntitlements(AccessControlEntitlementsModel expectedEntitlements) {
        if (expectedEntitlements == null) {
            return true;
        }
        List<String> actualEntitlements = AuthenticatedPrincipalContextHolder.getEntitlements();
        if (expectedEntitlements.isSatisfiedBy(actualEntitlements)) {
            return true;
        }
        if (actualEntitlements.isEmpty()) {
            reportMissingEntitlements(expectedEntitlements);
        } else {
            reportUnsatisfiedEntitlements(expectedEntitlements, actualEntitlements);
        }
        return false;
    }

    /**
     * Reports a principal that passed authentication but carries no entitlements at all.
     * <p>
     * Such a principal fails every entitlement requirement no matter which entitlement is asked for, which almost
     * always means the configured {@code PrincipalResolver} produces no entitlements — a JWT resolver whose entitlements
     * claim is absent from the tokens being issued, most commonly. It is reported once per process at
     * {@code WARN}, and on every occurrence at {@code DEBUG}.
     *
     * @param expectedEntitlements the requirement that was denied
     */
    private void reportMissingEntitlements(AccessControlEntitlementsModel expectedEntitlements) {
        if (!isMissingEntitlementsMisconfiguration()) {
            return;
        }
        String message = AccessControlDiagnostics.missingEntitlementsMessage();
        if (missingEntitlementsReported.compareAndSet(false, true)) {
            log.warn(message, describeRequirement(expectedEntitlements));
        } else {
            log.debug(message, describeRequirement(expectedEntitlements));
        }
    }

    /**
     * Explains an ordinary entitlements denial — the principal holds entitlements, just not the ones asked for.
     * <p>
     * Reported at {@code DEBUG} only: unlike a principal with no entitlements at all, this is business as usual on
     * any endpoint that restricts access, and reporting it louder would flood the log with healthy traffic.
     * Both sides of the comparison are printed so that a near miss caused by a misspelled entitlement name — an
     * {@code ADMNI} that was meant to be {@code ADMIN} — is visible instead of looking like a plain denial.
     *
     * @param expectedEntitlements the requirement that was not satisfied
     * @param actualEntitlements   the entitlements the principal actually carries
     */
    private void reportUnsatisfiedEntitlements(AccessControlEntitlementsModel expectedEntitlements,
                                               List<String> actualEntitlements) {
        if (!log.isDebugEnabled()) {
            return;
        }
        log.debug("Access denied: {} is required, but the authenticated principal carries {}. "
                        + "Entitlement names are matched exactly, so a name that merely looks alike does not match — "
                        + "check both sides for typos. See https://api4.pro/access-control-plugin/",
                describeRequirement(expectedEntitlements),
                describeNames(new HashSet<>(actualEntitlements)));
    }

    /**
     * Renders a requirement for a log line, e.g. {@code ANY_OF[ALL_OF[ADMIN, SUPPORT], ALL_OF[PARTNER]]}.
     * <p>
     * A declared {@code description} is quoted ahead of the structure, so a denial reads as the reason the
     * requirement exists rather than only as the entitlements it names.
     *
     * @param expectedEntitlements the requirement to describe
     * @return human-readable description
     */
    static String describeRequirement(AccessControlEntitlementsModel expectedEntitlements) {
        String structure = String.format("%s%s",
                expectedEntitlements.getMode(),
                expectedEntitlements.getGroups().stream()
                        .map(DefaultAccessControlEvaluator::describeGroup)
                        .collect(Collectors.joining(", ", "[", "]")));
        return StringUtils.isBlank(expectedEntitlements.getDescription())
                ? structure
                : String.format("'%s' (%s)", expectedEntitlements.getDescription(), structure);
    }

    /**
     * Renders a single clause of a requirement, e.g. {@code ALL_OF[ADMIN, PARTNER]}.
     *
     * @param group the clause to describe
     * @return human-readable description
     */
    static String describeGroup(EntitlementsGroupModel group) {
        return String.format("%s%s", group.getMode(), describeNames(group.getEntitlements()));
    }

    private static String describeNames(Set<String> names) {
        return names.stream().sorted().collect(Collectors.joining(", ", "[", "]"));
    }

    /**
     * Distinguishes a misconfigured principal from an ordinary anonymous one.
     * <p>
     * An anonymous caller legitimately has no entitlement, and denying it is the point of the requirement.
     * A caller that authenticated successfully and still has no entitlement is a configuration problem.
     *
     * @return {@code true} if the current principal is authenticated yet has no entitlement
     */
    boolean isMissingEntitlementsMisconfiguration() {
        return AuthenticatedPrincipalContextHolder.getEntitlements().isEmpty()
                && AuthenticatedPrincipalContextHolder.getAuthenticatedUserId()
                .filter(StringUtils::isNotBlank)
                .isPresent();
    }

    private boolean evaluateScopes(AccessControlScopesModel expectedScopes) {
        if (expectedScopes == null) {
            return true;
        }
        Set<String> grantedScopes = AuthenticatedPrincipalContextHolder.getScopes().orElse(Set.of());
        if (expectedScopes.isSatisfiedBy(grantedScopes)) {
            return true;
        }
        if (grantedScopes.isEmpty()) {
            reportMissingScopes(expectedScopes);
        } else {
            reportUnsatisfiedScopes(expectedScopes, grantedScopes);
        }
        return false;
    }

    /**
     * Reports a principal that passed authentication but carries no scopes at all.
     * <p>
     * Such a principal fails every scope requirement no matter which scope is asked for, which almost always
     * means the configured {@code PrincipalResolver} produces none — a JWT resolver whose scope claim is
     * absent from the tokens being issued, most commonly. It is reported once per process at {@code WARN},
     * and on every occurrence at {@code DEBUG}.
     *
     * @param expectedScopes the scopes requirement that was denied
     */
    private void reportMissingScopes(AccessControlScopesModel expectedScopes) {
        if (!isMissingScopesMisconfiguration()) {
            return;
        }
        String message = AccessControlDiagnostics.missingScopesMessage();
        if (missingScopesReported.compareAndSet(false, true)) {
            log.warn(message, describeScopesRequirement(expectedScopes));
        } else {
            log.debug(message, describeScopesRequirement(expectedScopes));
        }
    }

    /**
     * Explains an ordinary scopes denial — the principal holds scopes, just not the ones asked for.
     * <p>
     * Reported at {@code DEBUG} only, for the same reason as {@link #reportUnsatisfiedEntitlements}: on any
     * endpoint that restricts access this is business as usual, and reporting it louder would flood the log
     * with healthy traffic.
     *
     * @param ac           the scopes requirement that was not satisfied
     * @param actualScopes the scopes the principal actually carries
     */
    private void reportUnsatisfiedScopes(AccessControlScopesModel expectedScopes, Set<String> grantedScopes) {
        if (!log.isDebugEnabled()) {
            return;
        }
        log.debug("Access denied: {} is required, but the authenticated principal carries {}. "
                        + "Scope names are matched exactly — check both sides for typos. "
                        + "See https://api4.pro/access-control-plugin/",
                describeScopesRequirement(expectedScopes),
                describeNames(grantedScopes));
    }

    /**
     * Renders a scopes requirement, e.g. {@code scopes ALL_OF[ALL_OF[users.read, users.write]]}, prefixed by
     * a declared {@code description}.
     *
     * @param expectedScopes the requirement to describe
     * @return human-readable description
     */
    static String describeScopesRequirement(AccessControlScopesModel expectedScopes) {
        String structure = String.format("scopes %s%s",
                expectedScopes.getMode(),
                expectedScopes.getGroups().stream()
                        .map(DefaultAccessControlEvaluator::describeScopesGroup)
                        .collect(Collectors.joining(", ", "[", "]")));
        return StringUtils.isBlank(expectedScopes.getDescription())
                ? structure
                : String.format("'%s' (%s)", expectedScopes.getDescription(), structure);
    }

    /**
     * Renders a single clause of a scopes requirement, e.g. {@code ALL_OF[users.read, users.write]}.
     *
     * @param group the clause to describe
     * @return human-readable description
     */
    static String describeScopesGroup(ScopesGroupModel group) {
        return String.format("%s%s", group.getMode(), describeNames(group.getScopes()));
    }

    /**
     * Distinguishes a misconfigured principal from an ordinary anonymous one.
     * <p>
     * An anonymous caller legitimately has no scopes, and denying it is the point of the requirement.
     * A caller that authenticated successfully and still has no scopes is a configuration problem.
     *
     * @return {@code true} if the current principal is authenticated yet has no scopes
     */
    boolean isMissingScopesMisconfiguration() {
        return CollectionUtils.isEmpty(AuthenticatedPrincipalContextHolder.getScopes().orElse(null))
                && AuthenticatedPrincipalContextHolder.getAuthenticatedUserId()
                .filter(StringUtils::isNotBlank)
                .isPresent();
    }

}
