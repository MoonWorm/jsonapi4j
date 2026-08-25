package pro.api4.jsonapi4j.plugin.ac.anonymization;

import lombok.extern.slf4j.Slf4j;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;
import pro.api4.jsonapi4j.model.document.data.RelationshipObject;
import pro.api4.jsonapi4j.plugin.ac.diagnostics.AccessControlDiagnostics;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlModel;
import pro.api4.jsonapi4j.plugin.ac.model.outbound.OutboundAccessControlForCustomClass;
import pro.api4.jsonapi4j.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * What anonymization needs to know about one class: the requirements it declares, and which of its fields
 * are worth reading.
 *
 * <p>Computed once per class and reused. Fields are resolved and made accessible here so that walking an
 * object costs a field read per entry and nothing else.
 */
@Slf4j
public final class AnonymizationPlan {

    private static final ClassValue<AnonymizationPlan> PLANS = new ClassValue<>() {
        @Override
        protected AnonymizationPlan computeValue(Class<?> clazz) {
            return new AnonymizationPlan(clazz);
        }
    };

    private final AccessControlModel classLevel;
    private final Map<String, AccessControlModel> fieldLevel;
    private final List<Field> descendable;
    private final boolean leaf;

    private AnonymizationPlan(Class<?> clazz) {
        OutboundAccessControlForCustomClass requirements = OutboundAccessControlForCustomClass.forClass(clazz);
        this.classLevel = requirements == null ? null : requirements.getClassLevel();
        this.fieldLevel = requirements == null ? Map.of() : requirements.getFieldLevel();
        this.leaf = isLeafType(clazz);
        // Framework envelope types still get their fields resolved: the walk stops at them only when they
        // are reached as a value, and the response root is one of them. A JDK type or an enum gets none —
        // it holds nothing an application can annotate, and its internals are not open to reflection.
        this.descendable = isOpaque(clazz) ? List.of() : descendableFieldsOf(clazz);

        AccessControlDiagnostics.reportUnhideableFields(clazz, this.fieldLevel);
        AccessControlDiagnostics.reportShadowedFields(clazz);
    }

    public static AnonymizationPlan of(Class<?> clazz) {
        return PLANS.get(clazz);
    }

    public AccessControlModel classLevel() {
        return classLevel;
    }

    public Map<String, AccessControlModel> fieldLevel() {
        return fieldLevel;
    }

    /**
     * Fields whose values are worth reading and walking into.
     */
    public List<Field> descendable() {
        return descendable;
    }

    /**
     * Whether values of this class are carried through untouched rather than walked into.
     */
    public boolean isLeaf() {
        return leaf;
    }

    public boolean declaresRequirements() {
        return classLevel != null || !fieldLevel.isEmpty();
    }

    /**
     * Decides which fields can be skipped without missing a requirement.
     *
     * <p>A field may only be skipped when its declared type admits no subtype that could declare one:
     * primitives, statics, and {@code final} JDK types. Everything else is kept — {@code Object},
     * interfaces, abstract and non-final classes, arrays and containers — because the value's runtime type
     * is not knowable from the declaration.
     */
    private static List<Field> descendableFieldsOf(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        for (Field field : ReflectionUtils.fetchFields(clazz).values()) {
            if (Modifier.isStatic(field.getModifiers()) || field.getType().isPrimitive()) {
                continue;
            }
            if (isSkippableJdkType(field.getType())) {
                continue;
            }
            try {
                field.setAccessible(true);
            } catch (RuntimeException e) {
                log.debug("Access control cannot read {}.{}, skipping it", clazz.getName(), field.getName());
                continue;
            }
            fields.add(field);
        }
        return List.copyOf(fields);
    }

    /**
     * Whether a type carries nothing worth reading — a JDK type or an enum.
     */
    private static boolean isOpaque(Class<?> clazz) {
        return ReflectionUtils.isJdkType(clazz) || clazz.isEnum();
    }

    /**
     * A JDK type can be skipped only when it is {@code final} — so its runtime type is its declared type —
     * and it holds nothing. Containers are never skipped: {@code Optional} is {@code final}, and a
     * {@code Collection} or {@code Map} carries values whose types are decided at runtime.
     */
    private static boolean isSkippableJdkType(Class<?> type) {
        if (Containers.isContainerType(type)) {
            return false;
        }
        return ReflectionUtils.isJdkType(type) && Modifier.isFinal(type.getModifiers());
    }

    /**
     * Values of these types are never walked into.
     *
     * <p>JDK types and enums hold nothing an application can annotate. The framework's own envelope types
     * are excluded for a different reason: below the response root they are reached again through the
     * relationship visitors, which anonymize them with their own requirements, and they may be instances
     * shared with the compound-documents cache, where editing one would affect a later response.
     */
    private static boolean isLeafType(Class<?> clazz) {
        return isOpaque(clazz)
                || ResourceIdentifierObject.class.isAssignableFrom(clazz)
                || RelationshipObject.class.isAssignableFrom(clazz)
                || LinksObject.class.isAssignableFrom(clazz);
    }

}
