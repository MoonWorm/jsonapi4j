package io.jsonapi4j.oas.customizer.util;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import lombok.Data;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

@SuppressWarnings("rawtypes")
public final class SchemaGeneratorUtil {

    private static final ModelConverters MODEL_CONVERTERS = ModelConverters.getInstance();

    private SchemaGeneratorUtil() {
    }

    public static Schema<?> generateSchemaFromType(Class<?> clazz) {
        return MODEL_CONVERTERS.read(clazz).values().stream().findFirst().get();
    }

    public static PrimaryAndNestedSchemas generateAllSchemasFromType(Class<?> clazz) {
        Schema<?> parentSchema = generateSchemaFromType(clazz);
        Collection<Schema> parentSchemaWithNested = MODEL_CONVERTERS.readAll(clazz).values();
        List<Schema> parentSchemaWithoutNested = parentSchemaWithNested
                .stream()
                .filter(s -> !s.getName().equals(parentSchema.getName()))
                .toList();
        return new PrimaryAndNestedSchemas(parentSchema, parentSchemaWithoutNested);
    }

    public static String getSchemaName(Class<?> clazz) {
        if (clazz == null) {
            return null;
        }
        return generateSchemaFromType(clazz).getName();
    }

    public static void registerSchemaIfNotExists(Schema<?> schemaToAdd, OpenAPI openApi) {
        if (openApi.getComponents() == null) {
            openApi.setComponents(new Components());
        }
        if (openApi.getComponents().getSchemas() == null) {
            openApi.getComponents().setSchemas(new LinkedHashMap<>());
        }
        if (!openApi.getComponents().getSchemas().containsKey(schemaToAdd.getName())) {
            openApi.getComponents().getSchemas().put(schemaToAdd.getName(), schemaToAdd);
        }
    }

    @SuppressWarnings("rawtypes")
    @Data
    public static class PrimaryAndNestedSchemas {
        private final Schema primarySchema;
        private final Collection<Schema> nestedSchemas;

        public void addToNested(PrimaryAndNestedSchemas other) {
            this.nestedSchemas.add(other.getPrimarySchema());
            this.nestedSchemas.addAll(other.getNestedSchemas());
        }
    }
}
