package pro.api4.jsonapi4j.sampleapp.domain.user;

import pro.api4.jsonapi4j.operation.OperationType;
import pro.api4.jsonapi4j.plugin.ac.context.AccessControlContext;
import pro.api4.jsonapi4j.plugin.ac.policy.AccessPolicy;

/**
 * Restricts internal bookkeeping to a deliberate lookup of one user, keeping it out of list responses.
 * <p>
 * Expressible only as a policy: the declarative requirements describe <em>who</em> the caller is, while this
 * one turns on <em>what they are doing</em> — the operation being performed, which the policy reads from
 * {@link AccessControlContext#operation()}.
 */
public class SingleUserLookupPolicy implements AccessPolicy {

    @Override
    public boolean isSatisfiedBy(AccessControlContext context) {
        return context.operation() != null
                && context.operation().getOperationType() == OperationType.READ_RESOURCE_BY_ID;
    }

}
