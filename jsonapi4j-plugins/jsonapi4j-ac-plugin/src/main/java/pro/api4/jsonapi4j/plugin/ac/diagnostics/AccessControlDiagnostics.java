package pro.api4.jsonapi4j.plugin.ac.diagnostics;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControl;
import pro.api4.jsonapi4j.plugin.ac.exception.AccessControlMisconfigurationException;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlModel;
import pro.api4.jsonapi4j.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.regex.Matcher;

/**
 * The misconfigurations the Access Control plugin detects, and how each is reported.
 *
 * <ul>
 *     <li><b>Rejections</b> throw {@link AccessControlMisconfigurationException}: declarations that can
 *     never be satisfied, such as a clause naming no entitlement or a policy that cannot be
 *     instantiated.</li>
 *     <li><b>Reports</b> log a warning: declarations that are accepted but will not take effect. Raised
 *     once per class, while its model is built. Turn them into rejections with
 *     {@code jsonapi4j.ac.failOnMisconfiguration}.</li>
 * </ul>
 */
@Slf4j
public final class AccessControlDiagnostics {

    private static final AtomicBoolean FAIL_ON_MISCONFIGURATION = new AtomicBoolean();

    private AccessControlDiagnostics() {

    }

    /**
     * Sets whether reports are thrown instead of logged. Process-wide, set when the plugin is configured.
     *
     * @param failOnMisconfiguration {@code true} to throw
     */
    public static void failOnMisconfiguration(boolean failOnMisconfiguration) {
        FAIL_ON_MISCONFIGURATION.set(failOnMisconfiguration);
    }

    /**
     * Logs a declaration that will not take effect, or throws when
     * {@code jsonapi4j.ac.failOnMisconfiguration} is set. For problems decidable from a class alone;
     * caller-dependent diagnostics always log.
     */
    private static void report(String message, Object... arguments) {
        if (FAIL_ON_MISCONFIGURATION.get()) {
            throw new AccessControlMisconfigurationException(format(message, arguments));
        }
        log.warn(message, arguments);
    }

    /**
     * Substitutes SLF4J {@code {}} placeholders, so an exception can carry the same text as the log line.
     */
    private static String format(String message, Object... arguments) {
        String rendered = message;
        for (Object argument : arguments) {
            rendered = rendered.replaceFirst("\\{}", Matcher.quoteReplacement(String.valueOf(argument)));
        }
        return rendered;
    }

    // ---------------------------------------------------------------------------------------------------
    // Rejections — declarations that can never be satisfied
    // ---------------------------------------------------------------------------------------------------

    /**
     * Rejects a {@code @EntitlementsGroup} that is absent where one was required.
     */
    public static AccessControlMisconfigurationException missingEntitlementsGroup() {
        return new AccessControlMisconfigurationException(
                "@EntitlementsGroup must not be null. Omit it from @AccessControlEntitlements to declare no requirement.");
    }

    /**
     * Rejects a {@code @EntitlementsGroup} naming nothing, or naming a blank entitlement. Absence of a
     * requirement is expressed by leaving the clause out, not by declaring an empty one.
     */
    public static AccessControlMisconfigurationException emptyEntitlementsGroup(String[] declared) {
        return new AccessControlMisconfigurationException(String.format(
                "@EntitlementsGroup must name at least one non-blank entitlement, but was %s. "
                        + "Omit it from @AccessControlEntitlements to declare no requirement.",
                Arrays.toString(declared)));
    }

    /**
     * Rejects a {@code @ScopesGroup} that is absent where one was required.
     */
    public static AccessControlMisconfigurationException missingScopesGroup() {
        return new AccessControlMisconfigurationException(
                "@ScopesGroup must not be null. Omit it from @AccessControlScopes to declare no requirement.");
    }

    /**
     * Rejects a {@code @ScopesGroup} naming nothing, or naming a blank scope.
     */
    public static AccessControlMisconfigurationException emptyScopesGroup(String[] declared) {
        return new AccessControlMisconfigurationException(String.format(
                "@ScopesGroup must name at least one non-blank scope, but was %s. "
                        + "Omit it from @AccessControlScopes to declare no requirement.",
                Arrays.toString(declared)));
    }

    /**
     * Rejects an {@code AccessPolicy} that cannot be constructed.
     */
    public static AccessControlMisconfigurationException uninstantiablePolicy(Class<?> policyType, Throwable cause) {
        return new AccessControlMisconfigurationException(String.format(
                "Failed to instantiate AccessPolicy %s. It must be a public class with a public "
                        + "no-argument constructor.", policyType.getName()), cause);
    }

    /**
     * Rejects an {@code OwnerIdExtractor} that cannot be constructed.
     */
    public static AccessControlMisconfigurationException uninstantiableOwnerIdExtractor(Throwable cause) {
        return new AccessControlMisconfigurationException(
                "Failed to instantiate a custom OwnerIdExtractor. It must has the default constructor only.", cause);
    }

    /**
     * Rejects an ownership field that does not hold a {@code String} id.
     */
    public static AccessControlMisconfigurationException ownerIdFieldNotAString() {
        return new AccessControlMisconfigurationException("Owner ID field must be of type String");
    }

    /**
     * Rejects an ownership field that could not be read at all.
     */
    public static AccessControlMisconfigurationException unreadableOwnerIdField(Throwable cause) {
        return new AccessControlMisconfigurationException("Failed to read 'ownerId' field value", cause);
    }

    /**
     * Rejects an object whose fields could not be hidden, because a redacted copy of it could not be built.
     */
    public static AccessControlMisconfigurationException unredactable(Class<?> type,
                                                                     Set<String> fieldNames,
                                                                     Throwable cause) {
        return new AccessControlMisconfigurationException(String.format(
                "Anonymization failed. Could not hide fields %s of %s.", fieldNames, type.getName()), cause);
    }

    // ---------------------------------------------------------------------------------------------------
    // Reports — declarations that are accepted but will not take effect
    // ---------------------------------------------------------------------------------------------------

    /**
     * Reports field-level access control that cannot take effect: on a primitive field, which cannot hold
     * an absent value, or on a static field, which is never serialized.
     *
     * @param clazz      the class carrying the requirements
     * @param fieldLevel requirements declared on its fields
     */
    public static void reportUnhideableFields(Class<?> clazz, Map<String, AccessControlModel> fieldLevel) {
        if (MapUtils.isEmpty(fieldLevel)) {
            return;
        }
        Set<String> fieldNames = fieldLevel.keySet();
        for (String fieldName : primitiveFieldsAmong(clazz, fieldNames)) {
            report("Access control on {}.{} cannot be enforced: the field is a primitive and cannot be "
                            + "blanked out, so a denied caller gets an error instead of a hidden value. "
                            + "Change it to the boxed type to make it hideable.",
                    clazz.getName(), fieldName);
        }
        for (String fieldName : staticFieldsAmong(clazz, fieldNames)) {
            report("Access control on {}.{} has no effect: the field is static and is never serialized "
                            + "into a response, so there is nothing to hide.",
                    clazz.getName(), fieldName);
        }
    }

    /**
     * Reports access control declared on a class reached only through a collection, array or map.
     *
     * <p>Requirements are not applied to elements, so a rule on {@code Address.zip} has no effect when the
     * response exposes a {@code List<Address>}. Access control on the container field itself is enforced
     * and hides the whole container.
     */
    public static void reportUnenforceableElementRequirements(Class<?> owner,
                                                              String fieldName,
                                                              Class<?> fieldClass,
                                                              Type genericType) {
        for (Class<?> elementClass : unenforceableElementRequirements(fieldClass, genericType)) {
            report("Access control declared on {} will NOT be enforced: {}.{} holds it inside a {}, and "
                            + "requirements are not applied to collection, array or map elements. Access "
                            + "control on the '{}' field itself is still enforced and hides the whole "
                            + "container.",
                    elementClass.getName(), owner.getName(), fieldName, fieldClass.getSimpleName(), fieldName);
        }
    }

    /**
     * Reports an {@code @AccessControl} with every requirement left at its default, which enforces
     * nothing. Declaring no requirement is expressed by leaving the annotation off.
     *
     * @param element a description of what carries the annotation, used in the message
     * @param model   the model built from it
     */
    public static void reportRequirementlessAccessControl(String element, AccessControlModel model) {
        if (model == null || !model.declaresNoRequirements()) {
            return;
        }
        report("@AccessControl on {} declares no requirement, so it enforces nothing. Set one of "
                        + "authenticated, entitlements, scopes, ownership or policy, or remove the "
                        + "annotation.",
                element);
    }

    /**
     * Message for a principal that authenticated but carries no entitlements at all, which fails every
     * entitlement requirement and usually means the configured {@code PrincipalResolver} produces none.
     *
     * @return the message, taking the denied requirement as its single placeholder
     */
    public static String missingEntitlementsMessage() {
        return "Access denied: {} is required, but the "
                + "authenticated principal carries no entitlement at all. The configured PrincipalResolver "
                + "resolved none — when using a JWT resolver, check that issued tokens actually carry the "
                + "configured entitlements claim. See https://api4.pro/principal-resolution/";
    }

    /**
     * Message for a principal that authenticated but was granted no scopes at all. The scopes counterpart
     * of {@link #missingEntitlementsMessage()}.
     *
     * @return the message, taking the denied requirement as its single placeholder
     */
    public static String missingScopesMessage() {
        return "Access denied: {} is required, but the authenticated principal carries no scopes "
                + "at all. The configured PrincipalResolver resolved none — when using a JWT resolver, check "
                + "that issued tokens actually carry the configured scopes claim. "
                + "See https://api4.pro/principal-resolution/";
    }

    /**
     * Reports a field shadowing an inherited one of the same name. Requirements are looked up by name and
     * resolve to the most-derived declaration, so an {@code @AccessControl} on the inherited field is never
     * seen.
     *
     * @param clazz the class to inspect
     */
    public static void reportShadowedFields(Class<?> clazz) {
        for (String fieldName : ReflectionUtils.shadowedFieldNames(clazz)) {
            if (!anyDeclarationCarriesAccessControl(clazz, fieldName)) {
                continue;
            }
            report("{}.{} shadows an inherited field of the same name. Only the most-derived declaration "
                            + "is used, so @AccessControl on the inherited one is ignored. Rename one of "
                            + "them so the requirement is unambiguous.",
                    clazz.getName(), fieldName);
        }
    }

    private static boolean anyDeclarationCarriesAccessControl(Class<?> clazz, String fieldName) {
        for (Class<?> c = clazz; c != null; c = c.getSuperclass()) {
            for (Field field : c.getDeclaredFields()) {
                if (field.getName().equals(fieldName) && field.isAnnotationPresent(AccessControl.class)) {
                    return true;
                }
            }
        }
        return false;
    }

    // ---------------------------------------------------------------------------------------------------
    // Predicates behind the reports, separated so they can be asserted on directly
    // ---------------------------------------------------------------------------------------------------

    /**
     * Returns which of the named fields are primitives, and so can never hold the absence of a value.
     */
    public static Set<String> primitiveFieldsAmong(Class<?> clazz, Set<String> fieldNames) {
        return matching(clazz, fieldNames, field -> field.getType().isPrimitive());
    }

    /**
     * Returns which of the named fields are static, and so never appear in a serialized response.
     */
    public static Set<String> staticFieldsAmong(Class<?> clazz, Set<String> fieldNames) {
        return matching(clazz, fieldNames, field -> Modifier.isStatic(field.getModifiers()));
    }

    /**
     * Returns the element types of a container field that declare access control which will never run.
     * Empty for a field that is not a container, or whose elements declare nothing.
     */
    public static Set<Class<?>> unenforceableElementRequirements(Class<?> fieldClass, Type genericType) {
        Set<Class<?>> result = new LinkedHashSet<>();
        for (Class<?> elementClass : elementTypesOf(fieldClass, genericType)) {
            if (!ReflectionUtils.isJdkType(elementClass) && declaresAccessControl(elementClass)) {
                result.add(elementClass);
            }
        }
        return Collections.unmodifiableSet(result);
    }

    private static boolean declaresAccessControl(Class<?> clazz) {
        return AccessControlModel.fromClassAnnotation(clazz) != null
                || MapUtils.isNotEmpty(AccessControlModel.fromFieldsAnnotations(clazz));
    }

    /**
     * Returns the types held <i>inside</i> a container field — the component type of an array, or the type
     * arguments of a {@code Collection}, {@code Map} or {@code Optional}. Empty for anything else.
     */
    private static Set<Class<?>> elementTypesOf(Class<?> fieldClass, Type genericType) {
        if (fieldClass.isArray()) {
            return Set.of(fieldClass.getComponentType());
        }
        boolean isContainer = Collection.class.isAssignableFrom(fieldClass)
                || Map.class.isAssignableFrom(fieldClass)
                || Optional.class.isAssignableFrom(fieldClass);
        if (!isContainer || !(genericType instanceof ParameterizedType parameterizedType)) {
            return Set.of();
        }
        Set<Class<?>> elementTypes = new LinkedHashSet<>();
        for (Type typeArgument : parameterizedType.getActualTypeArguments()) {
            if (typeArgument instanceof Class<?> typeArgumentClass) {
                elementTypes.add(typeArgumentClass);
            }
        }
        return Collections.unmodifiableSet(elementTypes);
    }

    private static Set<String> matching(Class<?> clazz, Set<String> fieldNames, Predicate<Field> predicate) {
        Map<String, Field> fields = ReflectionUtils.fetchFields(clazz);
        Set<String> result = new LinkedHashSet<>();
        for (String fieldName : fieldNames) {
            Field field = fields.get(fieldName);
            if (field != null && predicate.test(field)) {
                result.add(fieldName);
            }
        }
        return Collections.unmodifiableSet(result);
    }

}
