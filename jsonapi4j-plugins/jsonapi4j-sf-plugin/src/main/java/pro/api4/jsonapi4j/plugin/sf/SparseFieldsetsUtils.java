package pro.api4.jsonapi4j.plugin.sf;

import pro.api4.jsonapi4j.model.document.data.ResourceObject;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.util.ReflectionUtils;

import java.util.List;
import java.util.Set;

public final class SparseFieldsetsUtils {

    private SparseFieldsetsUtils() {

    }

    public static void doSparseFieldsets(JsonApiRequest jsonApiRequest,
                           ResourceObject<?, ?> resourceObject) {
        String resourceType = resourceObject.getType();
        if (jsonApiRequest.getFieldSets().containsKey(resourceType)) {
            Object attributes = resourceObject.getAttributes();
            if (attributes != null) {
                List<String> fieldsToInclude = jsonApiRequest.getFieldSets().get(resourceType);
                Set<String> allPaths = ReflectionUtils.getAllFieldPaths(attributes.getClass());
                for (String path : allPaths) {
                    if (!fieldsToInclude.contains(path)) {
                        ReflectionUtils.setFieldPathValueSilent(attributes, path, null);
                    }
                }
            }
        }
    }

}
