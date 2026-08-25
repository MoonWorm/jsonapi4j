package pro.api4.jsonapi4j.plugin.sf;

import lombok.extern.slf4j.Slf4j;
import pro.api4.jsonapi4j.model.document.data.ResourceObject;
import pro.api4.jsonapi4j.plugin.sf.config.SfProperties;
import pro.api4.jsonapi4j.plugin.sf.config.SfProperties.RequestedFieldsDontExistMode;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.util.Containers;
import pro.api4.jsonapi4j.util.ObjectCopier;
import pro.api4.jsonapi4j.util.ReflectionUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
class SparseFieldsetsHelper {

    private final SfProperties sfProperties;

    SparseFieldsetsHelper(SfProperties sfProperties) {
        this.sfProperties = sfProperties;
    }

    public boolean enabled() {
        return sfProperties.enabled();
    }

    public void sparseFieldsets(JsonApiRequest jsonApiRequest,
                         ResourceObject<?, ?> resourceObject) {
        String resourceType = resourceObject.getType();
        List<String> requestedPaths = jsonApiRequest.getFieldSets().get(resourceType);
        if (requestedPaths == null) {
            log.debug("No sparse fieldsets requested for type '{}'", resourceType);
            // no 'fields' param for this resource type
            return;
        }
        Object attributes = resourceObject.getAttributes();
        if (attributes != null) {
            if (requestedPaths.isEmpty()) {
                sparseAllFields(resourceObject);
            } else {
                nonEmptyFieldsParamRequested(resourceObject, requestedPaths, sfProperties.requestedFieldsDontExistMode());
            }
        }
    }

    private void sparseAllFields(ResourceObject<?, ?> resourceObject) {
        log.debug("Sparse fieldsets: removing all attributes for resource type '{}'", resourceObject.getType());
        ReflectionUtils.setFieldPathValueSilent(resourceObject, ResourceObject.ATTRIBUTES_FIELD, null);
    }

    private void nonEmptyFieldsParamRequested(ResourceObject<?, ?> resourceObject,
                                              List<String> requestedPaths,
                                              RequestedFieldsDontExistMode requestedFieldsDontExistMode) {
        List<String> existingPathsToInclude = requestedPaths.stream()
                .filter(p -> ReflectionUtils.fieldPathExists(resourceObject.getAttributes(), p))
                .toList();
        if (existingPathsToInclude.isEmpty()) {
            switch (requestedFieldsDontExistMode) {
                case RETURN_ALL_FIELDS -> {
                    // do nothing
                }
                case SPARSE_ALL_FIELDS -> sparseAllFields(resourceObject);
            }
        } else {
            sparseNonRequestedFields(resourceObject, existingPathsToInclude);
        }
    }

    /**
     * Replaces the attributes with a copy that carries only the requested fields.
     *
     * <p>The original attributes object is never modified. Objects are copied only along a path to an
     * excluded field; anything untouched is carried over by reference, and when nothing is excluded the
     * original instance is used as-is.
     */
    private void sparseNonRequestedFields(ResourceObject<?, ?> resourceObject,
                                          List<String> existingPathsToInclude) {
        Object attributes = resourceObject.getAttributes();
        Set<String> pathsToInclude = existingPathsToInclude.stream()
                .flatMap(p -> denormalizePath(p).stream())
                .collect(Collectors.toSet());
        Set<String> allPaths = ReflectionUtils.getAllFieldPaths(attributes.getClass());
        Set<String> pathsToExclude = allPaths.stream()
                .filter(p -> !pathsToInclude.contains(p))
                .collect(Collectors.toSet());
        if (pathsToExclude.isEmpty()) {
            return;
        }
        try {
            Object sparsed = sparse(attributes, allPaths, pathsToExclude, "");
            if (sparsed != attributes) {
                ReflectionUtils.setFieldPathValueSilent(resourceObject, ResourceObject.ATTRIBUTES_FIELD, sparsed);
            }
        } catch (RuntimeException e) {
            // A class that cannot be copied returns all of its fields; sparse fieldsets is a convenience
            // over the response shape, not a security control.
            log.warn("Sparse fieldsets: could not build a reduced copy of {}, returning all of its fields.",
                    attributes.getClass().getName(), e);
        }
    }

    /**
     * Builds a copy of {@code source} without the excluded fields, recursing into the objects that hold
     * them. Returns {@code source} untouched when nothing below it is excluded.
     */
    private Object sparse(Object source, Set<String> allPaths, Set<String> pathsToExclude, String prefix) {
        Map<String, Field> fields = ReflectionUtils.fetchFields(source.getClass());
        Map<String, Object> replacements = new HashMap<>();
        for (String fieldName : directChildrenOf(allPaths, prefix)) {
            String path = prefix.isEmpty() ? fieldName : prefix + "." + fieldName;
            Field field = fields.get(fieldName);
            if (pathsToExclude.contains(path)) {
                // A primitive cannot hold the absence of a value, so it stays — as it always has.
                if (field != null && !field.getType().isPrimitive()) {
                    replacements.put(fieldName, null);
                }
                continue;
            }
            if (!hasExcludedDescendant(pathsToExclude, path)) {
                continue;
            }
            Object child = ReflectionUtils.getFieldValueThrowing(source, fieldName);
            if (child != null) {
                Object sparsedChild = Containers.isContainer(child)
                        ? sparseElements(child, allPaths, pathsToExclude, path, field)
                        : sparse(child, allPaths, pathsToExclude, path);
                if (sparsedChild != child) {
                    replacements.put(fieldName, sparsedChild);
                }
            }
        }
        return replacements.isEmpty() ? source : ObjectCopier.copyWith(source, replacements);
    }

    /**
     * Applies the same paths to every element of a container.
     *
     * <p>A requested path names a field of each element rather than of one, so there is no index to match:
     * {@code fields[users]=addresses.zip} keeps {@code zip} in every address. The container is rebuilt only
     * when an element changed, and elements are never dropped — sparse fieldsets narrows what each object
     * carries, not how many there are.
     */
    private Object sparseElements(Object container,
                                  Set<String> allPaths,
                                  Set<String> pathsToExclude,
                                  String path,
                                  Field field) {
        List<Object> elements = new ArrayList<>();
        List<Object> keys = new ArrayList<>();
        boolean changed = false;
        for (Map.Entry<Object, Object> entry : entriesOf(container)) {
            Object element = entry.getValue();
            Object sparsedElement = element == null || Containers.isContainer(element)
                    ? element
                    : sparse(element, allPaths, pathsToExclude, path);
            changed |= sparsedElement != element;
            elements.add(sparsedElement);
            keys.add(entry.getKey());
        }
        if (!changed) {
            return container;
        }
        Object rebuilt = Containers.rebuild(container, elements, keys,
                field != null ? field.getType() : null);
        if (rebuilt != null) {
            return rebuilt;
        }
        log.warn("Sparse fieldsets: could not rebuild {} for '{}', returning it unchanged.",
                container.getClass().getName(), path);
        return container;
    }

    private static List<Map.Entry<Object, Object>> entriesOf(Object container) {
        List<Map.Entry<Object, Object>> entries = new ArrayList<>();
        if (container instanceof Map<?, ?> map) {
            map.forEach((key, value) -> entries.add(new AbstractMap.SimpleEntry<>(key, value)));
        } else if (container instanceof Optional<?> optional) {
            optional.ifPresent(value -> entries.add(new AbstractMap.SimpleEntry<>(null, value)));
        } else if (container instanceof Collection<?> collection) {
            collection.forEach(value -> entries.add(new AbstractMap.SimpleEntry<>(null, value)));
        } else {
            int length = Array.getLength(container);
            for (int i = 0; i < length; i++) {
                entries.add(new AbstractMap.SimpleEntry<>(null, Array.get(container, i)));
            }
        }
        return entries;
    }


    private static Set<String> directChildrenOf(Set<String> allPaths, String prefix) {
        String childPrefix = prefix.isEmpty() ? "" : prefix + ".";
        Set<String> children = new LinkedHashSet<>();
        for (String path : allPaths) {
            if (!path.startsWith(childPrefix)) {
                continue;
            }
            String remainder = path.substring(childPrefix.length());
            if (!remainder.isEmpty()) {
                int dot = remainder.indexOf('.');
                children.add(dot < 0 ? remainder : remainder.substring(0, dot));
            }
        }
        return children;
    }

    private static boolean hasExcludedDescendant(Set<String> pathsToExclude, String path) {
        String descendantPrefix = path + ".";
        return pathsToExclude.stream().anyMatch(p -> p.startsWith(descendantPrefix));
    }

    /**
     * Denormalizes path like "a.b.c" to {"a", "a.b", "a.b.c"} so previous subpaths will not be set to null.
     *
     * @param path original path e.g. "a.b.c"
     * @return denormalized list of paths
     */
    List<String> denormalizePath(String path) {
        String[] pathFragments = path.split("\\.");
        if (pathFragments.length == 1) {
            return Collections.singletonList(path);
        }
        List<String> result = new ArrayList<>();
        String currentSubpath = pathFragments[0];
        result.add(pathFragments[0]);
        for (int i = 1; i < pathFragments.length; i++) {
            result.add(currentSubpath + "." + pathFragments[i]);
            currentSubpath = currentSubpath + "." + pathFragments[i];
        }
        return Collections.unmodifiableList(result);
    }

}
