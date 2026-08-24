package pro.api4.jsonapi4j.plugin.ac.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlPolicy;
import pro.api4.jsonapi4j.plugin.ac.context.AccessControlContext;
import pro.api4.jsonapi4j.plugin.ac.diagnostics.AccessControlDiagnostics;
import pro.api4.jsonapi4j.plugin.ac.exception.AccessControlMisconfigurationException;
import pro.api4.jsonapi4j.plugin.ac.policy.AccessPolicy;
import pro.api4.jsonapi4j.plugin.ac.policy.NoOpAccessPolicy;

import java.lang.reflect.InvocationTargetException;

@EqualsAndHashCode
@ToString
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PACKAGE)
public class AccessControlPolicyModel {

    private AccessPolicy policy;

    private String description;

    /**
     * Builds the model for an {@code @AccessControlPolicy} requirement.
     * <p>
     * The policy is instantiated once here rather than per request, so a policy class that cannot be
     * constructed fails at registration instead of on the first request that reaches the annotated element.
     *
     * @param annotation the annotation to read
     * @return the model, or {@code null} when no policy is declared
     * @throws AccessControlMisconfigurationException if the policy cannot be instantiated
     */
    static AccessControlPolicyModel fromAnnotation(AccessControlPolicy annotation) {
        if (annotation == null || NoOpAccessPolicy.class.equals(annotation.value())) {
            return null;
        }
        return AccessControlPolicyModel.builder()
                .policy(instantiate(annotation.value()))
                .description(StringUtils.trimToNull(annotation.description()))
                .build();
    }

    private static AccessPolicy instantiate(Class<? extends AccessPolicy> policyType) {
        try {
            return policyType.getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException
                 | NoSuchMethodException e) {
            throw AccessControlDiagnostics.uninstantiablePolicy(policyType, e);
        }
    }

    /**
     * Decides whether the declared policy allows the current evaluation.
     *
     * @param context everything the decision may be based on
     * @return {@code true} when the policy allows it
     */
    public boolean isSatisfiedBy(AccessControlContext context) {
        return policy.isSatisfiedBy(context);
    }

}
