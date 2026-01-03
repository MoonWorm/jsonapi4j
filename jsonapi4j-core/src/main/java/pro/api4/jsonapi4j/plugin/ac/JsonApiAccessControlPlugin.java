package pro.api4.jsonapi4j.plugin.ac;

import pro.api4.jsonapi4j.plugin.ac.impl.model.AccessControlModel;
import pro.api4.jsonapi4j.operation.*;
import pro.api4.jsonapi4j.plugin.JsonApi4jPlugin;

import java.util.Map;
import java.util.function.Supplier;

public class JsonApiAccessControlPlugin implements JsonApi4jPlugin {

    public static final String NAME = JsonApiAccessControlPlugin.class.getSimpleName();

    @Override
    public String pluginName() {
        return NAME;
    }

    @Override
    public Object extractPluginInfoFromOperation(Operation operation, Class<?> operationClass) {
        Map<String, AccessControlModel> methodsAnnotations
                = AccessControlModel.fromFieldsAnnotations(operation.getClass());
        AccessControlModel classLevel = AccessControlModel.fromClassAnnotation(operation.getClass());
        if (ReadResourceByIdOperation.class.isAssignableFrom(operationClass)) {
            return getOrDefault(() -> methodsAnnotations.get("readById"), classLevel);
        }
        if (ReadMultipleResourcesOperation.class.isAssignableFrom(operationClass)) {
            return getOrDefault(() -> methodsAnnotations.get("readPage"), classLevel);
        }
        if (CreateResourceOperation.class.isAssignableFrom(operationClass)) {
            return getOrDefault(() -> methodsAnnotations.get("create"), classLevel);
        }
        if (UpdateResourceOperation.class.isAssignableFrom(operationClass)) {
            return getOrDefault(() -> methodsAnnotations.get("update"), classLevel);
        }
        if (DeleteResourceOperation.class.isAssignableFrom(operationClass)) {
            return getOrDefault(() -> methodsAnnotations.get("delete"), classLevel);
        }
        if (ReadToOneRelationshipOperation.class.isAssignableFrom(operationClass)) {
            return getOrDefault(() -> methodsAnnotations.get("readOne"), classLevel);
        }
        if (ReadToManyRelationshipOperation.class.isAssignableFrom(operationClass)) {
            return getOrDefault(() -> methodsAnnotations.get("readMany"), classLevel);
        }
        if (UpdateToOneRelationshipOperation.class.isAssignableFrom(operationClass)) {
            return getOrDefault(() -> methodsAnnotations.get("update"), classLevel);
        }
        if (UpdateToManyRelationshipOperation.class.isAssignableFrom(operationClass)) {
            return getOrDefault(() -> methodsAnnotations.get("update"), classLevel);
        }
        return classLevel;
    }

    private Object getOrDefault(Supplier<AccessControlModel> primarySupplier,
                                                             AccessControlModel secondary) {
        AccessControlModel primary = primarySupplier.get();
        if (primary == null) {
            return secondary;
        }
        return primary;
    }

}
