package pro.api4.jsonapi4j.plugin.sf;

import pro.api4.jsonapi4j.model.document.data.ResourceObject;
import pro.api4.jsonapi4j.plugin.sf.config.SfProperties;
import pro.api4.jsonapi4j.plugin.sf.config.SfProperties.RequestedFieldsDontExistMode;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.util.ReflectionUtils;

import java.util.*;

public final class SparseFieldsetsUtils {

    private SparseFieldsetsUtils() {

    }

    public static void sparseFieldsets(JsonApiRequest jsonApiRequest,
                                       ResourceObject<?, ?> resourceObject,
                                       SfProperties sfProperties) {
        String resourceType = resourceObject.getType();
        List<String> requestedPaths = jsonApiRequest.getFieldSets().get(resourceType);
        if (requestedPaths == null) {
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

    private static void sparseAllFields(ResourceObject<?, ?> resourceObject) {
        ReflectionUtils.setFieldPathValueSilent(resourceObject, ResourceObject.ATTRIBUTES_FIELD, null);
    }

    private static void nonEmptyFieldsParamRequested(ResourceObject<?, ?> resourceObject,
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

    private static void sparseNonRequestedFields(ResourceObject<?, ?> resourceObject,
                                                 List<String> existingPathsToInclude) {
        Object attributes = resourceObject.getAttributes();
        List<String> denormalizedPathsToInclude = existingPathsToInclude.stream()
                .flatMap(p -> denormalizePath(p).stream())
                .distinct()
                .toList();
        Set<String> allPaths = ReflectionUtils.getAllFieldPaths(attributes.getClass());
        for (String path : allPaths) {
            if (!denormalizedPathsToInclude.contains(path)) {
                ReflectionUtils.setFieldPathValueSilent(attributes, path, null);
            }
        }
    }

    /**
     * Denormalizes path like "a.b.c" to {"a", "a.b", "a.b.c"} so previous subpaths will not be set to null.
     *
     * @param path original path e.g. "a.b.c"
     * @return denormalized list of paths
     */
    private static List<String> denormalizePath(String path) {
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
