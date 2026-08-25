package pro.api4.jsonapi4j.plugin.ac.anonymization;

import lombok.extern.slf4j.Slf4j;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;
import pro.api4.jsonapi4j.plugin.ac.AnonymizationResult;
import pro.api4.jsonapi4j.plugin.ac.context.AccessControlContext;
import pro.api4.jsonapi4j.plugin.ac.diagnostics.AccessControlDiagnostics;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlModel;
import pro.api4.jsonapi4j.plugin.ac.model.outbound.OutboundAccessControlForCustomClass;
import pro.api4.jsonapi4j.util.ObjectCopier;
import pro.api4.jsonapi4j.util.Containers;
import pro.api4.jsonapi4j.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;

/**
 * Walks a composed response and returns a view of it carrying only what the caller may see.
 *
 * <p>Nothing reachable from the input is modified. Where something has to be hidden, the objects on the
 * path to it are copied and rewired; everything else is carried over by reference, and when nothing is
 * hidden the original instance is returned.
 *
 * <p>Requirements are resolved from the <b>runtime</b> type of each value met, so a rule declared on a
 * class applies wherever an instance of it appears — held directly, behind a field declared as an interface
 * or a supertype, or inside a collection, array or map.
 */
@Slf4j
public final class OutboundAnonymizer {

    /**
     * Decides a single set of requirements — supplied by the evaluator so that a custom one governs the
     * decisions without having to reimplement the walk.
     */
    private final BiPredicate<AccessControlContext, AccessControlModel> requirementsSatisfied;

    public OutboundAnonymizer(BiPredicate<AccessControlContext, AccessControlModel> requirementsSatisfied) {
        this.requirementsSatisfied = requirementsSatisfied;
    }

    /**
     * @param rootRequirements requirements for the root object, already merged with any declared at
     *                         registration; {@code null} when its own class declares none, which does not
     *                         end the walk — the objects it holds are resolved by their own types
     */
    public <T> AnonymizationResult<T> anonymize(T root,
                                                AccessControlContext context,
                                                OutboundAccessControlForCustomClass rootRequirements) {
        if (root == null) {
            return new AnonymizationResult<>(root, false, Collections.emptySet());
        }
        Set<Object> branch = Collections.newSetFromMap(new IdentityHashMap<>());
        return walk("", root, context, rootRequirements, branch);
    }

    @SuppressWarnings("unchecked")
    private <T> AnonymizationResult<T> walk(String path,
                                            T target,
                                            AccessControlContext context,
                                            OutboundAccessControlForCustomClass requirements,
                                            Set<Object> branch) {
        AnonymizationPlan plan = AnonymizationPlan.of(target.getClass());
        AccessControlModel classLevel = requirements != null ? requirements.getClassLevel() : plan.classLevel();
        Map<String, AccessControlModel> fieldLevel
                = requirements != null ? requirements.getFieldLevel() : plan.fieldLevel();

        if (classLevel != null && !requirementsSatisfied.test(context, classLevel)) {
            return new AnonymizationResult<>(null, true, Collections.emptySet());
        }
        if (!branch.add(target)) {
            log.debug("Access control walk met {} again in the same branch, stopping there",
                    target.getClass().getName());
            return new AnonymizationResult<>(target, false, Collections.emptySet());
        }
        try {
            return walkFields(path, target, context, plan, fieldLevel, branch);
        } finally {
            branch.remove(target);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> AnonymizationResult<T> walkFields(String path,
                                                  T target,
                                                  AccessControlContext context,
                                                  AnonymizationPlan plan,
                                                  Map<String, AccessControlModel> fieldLevel,
                                                  Set<Object> branch) {
        Set<String> denied = deniedFields(target, context, fieldLevel);
        Map<String, Object> replacements = new HashMap<>();
        denied.forEach(fieldName -> replacements.put(fieldName, null));
        Set<String> hidden = new LinkedHashSet<>(denied);

        for (Field field : plan.descendable()) {
            String fieldName = field.getName();
            if (denied.contains(fieldName)) {
                continue;
            }
            Object value = read(field, target);
            if (value == null) {
                continue;
            }
            Redacted redacted = redactValue(fieldName, value, field.getType(), context, branch);
            if (redacted == null) {
                continue;
            }
            hidden.addAll(redacted.hidden());
            if (redacted.value() != value) {
                replacements.put(fieldName, redacted.value());
            }
        }

        T result = replacements.isEmpty() ? target : apply(target, replacements);
        return new AnonymizationResult<>(result, false, prefixed(path, hidden));
    }

    /**
     * Redacts one value — an object, or the elements of a container.
     *
     * @return the redacted value and what it hid, or {@code null} when the value is carried through as-is
     */
    private Redacted redactValue(String path,
                                 Object value,
                                 Class<?> declaredType,
                                 AccessControlContext context,
                                 Set<Object> branch) {
        if (Containers.isContainer(value)) {
            return redactContainer(path, value, declaredType, context, branch);
        }
        if (AnonymizationPlan.of(value.getClass()).isLeaf()) {
            return null;
        }
        AnonymizationResult<Object> result = walk(path, value, context, null, branch);
        if (result.isFullyAnonymized()) {
            return new Redacted(null, Set.of(path));
        }
        return new Redacted(result.targetObject(), result.anonymizedFields());
    }

    private Redacted redactContainer(String path,
                                     Object container,
                                     Class<?> declaredType,
                                     AccessControlContext context,
                                     Set<Object> branch) {
        List<Object> kept = new ArrayList<>();
        List<Object> keptKeys = new ArrayList<>();
        Set<String> hidden = new LinkedHashSet<>();
        boolean changed = false;
        int index = 0;

        for (Entry entry : entriesOf(container)) {
            String elementPath = entry.key() == null
                    ? path + "[" + index + "]"
                    : path + "[" + entry.key() + "]";
            index++;
            Object element = entry.value();
            if (element == null) {
                kept.add(null);
                keptKeys.add(entry.key());
                continue;
            }
            Redacted redacted = redactValue(elementPath, element, null, context, branch);
            if (redacted == null) {
                kept.add(element);
                keptKeys.add(entry.key());
                continue;
            }
            hidden.addAll(redacted.hidden());
            if (redacted.value() == null) {
                changed = true;
                continue;
            }
            changed |= redacted.value() != element;
            kept.add(redacted.value());
            keptKeys.add(entry.key());
        }

        if (!changed) {
            return hidden.isEmpty() ? null : new Redacted(container, hidden);
        }
        Object rebuilt = Containers.rebuild(container, kept, keptKeys, declaredType);
        if (rebuilt == null) {
            throw AccessControlDiagnostics.unrebuildableContainer(container.getClass(), declaredType, path);
        }
        return new Redacted(rebuilt, hidden);
    }

    private Set<String> deniedFields(Object target,
                                     AccessControlContext context,
                                     Map<String, AccessControlModel> fieldLevel) {
        if (fieldLevel == null || fieldLevel.isEmpty()) {
            return Set.of();
        }
        Set<String> denied = new HashSet<>();
        for (Map.Entry<String, AccessControlModel> e : fieldLevel.entrySet()) {
            Object value = ReflectionUtils.getFieldValueThrowing(target, e.getKey());
            if (value != null && !requirementsSatisfied.test(context, e.getValue())) {
                denied.add(e.getKey());
            }
        }
        return denied;
    }

    /**
     * Writes the hidden fields into a copy — except for the envelopes the framework builds per response,
     * which are edited in place.
     */
    @SuppressWarnings("unchecked")
    private static <T> T apply(T target, Map<String, Object> replacements) {
        try {
            if (target instanceof ResourceIdentifierObject) {
                replacements.forEach((fieldName, value) ->
                        ReflectionUtils.setFieldValueThrowing(target, fieldName, value));
                return target;
            }
            return ObjectCopier.copyWith(target, replacements);
        } catch (Exception ex) {
            throw AccessControlDiagnostics.unredactable(target.getClass(), replacements.keySet(), ex);
        }
    }

    private static Object read(Field field, Object target) {
        try {
            return field.get(target);
        } catch (IllegalAccessException e) {
            throw AccessControlDiagnostics.unreadableField(field.getName(), target.getClass(), e);
        }
    }

    private static Set<String> prefixed(String path, Set<String> names) {
        if (path.isEmpty() || names.isEmpty()) {
            return Collections.unmodifiableSet(names);
        }
        Set<String> result = new LinkedHashSet<>();
        names.forEach(name -> result.add(path + "." + name));
        return Collections.unmodifiableSet(result);
    }

    private static List<Entry> entriesOf(Object container) {
        List<Entry> entries = new ArrayList<>();
        if (container instanceof Map<?, ?> map) {
            map.forEach((key, value) -> entries.add(new Entry(key, value)));
        } else if (container instanceof Optional<?> optional) {
            optional.ifPresent(value -> entries.add(new Entry(null, value)));
        } else if (container instanceof Collection<?> collection) {
            collection.forEach(value -> entries.add(new Entry(null, value)));
        } else {
            int length = java.lang.reflect.Array.getLength(container);
            for (int i = 0; i < length; i++) {
                entries.add(new Entry(null, java.lang.reflect.Array.get(container, i)));
            }
        }
        return entries;
    }

    private record Entry(Object key, Object value) {
    }

    private record Redacted(Object value, Set<String> hidden) {
    }

}
