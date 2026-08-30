package pro.api4.jsonapi4j.plugin.oas.customizer.util;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.model.document.data.ResourceObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;

import static pro.api4.jsonapi4j.model.document.data.ResourceObject.LINKS_FIELD;

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

    /**
     * Points a JSON:API {@code links} member at the shared {@link LinksObject} schema, and returns the same schema for
     * chaining. Only call this on schemas generated from framework document/resource classes, where {@code links} is
     * known to be a {@link LinksObject}.
     * <p>
     * Reflecting that class in place instead publishes its {@code isEmpty()} accessor as an {@code empty} boolean
     * property that no response ever carries, and leaves the hand-written {@code LinksObject} schema — the one that
     * actually describes a link as either a URL string or a link object — referenced by nothing.
     */
    public static Schema<?> withLinksObjectRef(Schema<?> schema) {
        if (schema.getProperties() != null && schema.getProperties().containsKey(LINKS_FIELD)) {
            schema.getProperties().put(LINKS_FIELD, new Schema<>().$ref(LinksObject.class.getSimpleName()));
        }
        return schema;
    }

    public static PrimaryAndNestedSchemas withLinksObjectRef(PrimaryAndNestedSchemas schemas) {
        withLinksObjectRef(schemas.getPrimarySchema());
        schemas.getNestedSchemas().forEach(SchemaGeneratorUtil::withLinksObjectRef);
        return schemas;
    }

    /**
     * The name reflection gives a {@code ResourceObject} whose type arguments are wildcards, as {@code included}
     * declares it: swagger appends one resolved argument name per type parameter, so {@code ResourceObject<?, ?>}
     * becomes {@code ResourceObjectObjectObject}.
     * <p>
     * The schema behind that name has {@code attributes} and {@code relationships} typed as bare objects — nothing a
     * client can act on — so it is never published. Every member that needs a resource object points at a concrete
     * {@code <Type>Resource} instead.
     */
    private static final String UNTYPED_RESOURCE_OBJECT_SCHEMA_NAME =
            ResourceObject.class.getSimpleName() + "Object".repeat(ResourceObject.class.getTypeParameters().length);

    public static boolean isUntypedResourceObject(Schema<?> schema) {
        return UNTYPED_RESOURCE_OBJECT_SCHEMA_NAME.equals(schema.getName());
    }

    public static void registerSchemaIfNotExists(Schema<?> schemaToAdd, OpenAPI openApi) {
        if (isUntypedResourceObject(schemaToAdd)) {
            return;
        }
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
    @Getter
    @EqualsAndHashCode
    @ToString
    public static class PrimaryAndNestedSchemas {
        private final Schema primarySchema;
        private final Collection<Schema> nestedSchemas;

        /**
         * Copies the nested schemas so that {@link #addToNested(PrimaryAndNestedSchemas)} stays available whatever the
         * caller passed in — {@link #generateAllSchemasFromType(Class)} supplies an immutable list.
         */
        public PrimaryAndNestedSchemas(Schema primarySchema,
                                       Collection<Schema> nestedSchemas) {
            this.primarySchema = primarySchema;
            this.nestedSchemas = new ArrayList<>(nestedSchemas);
        }

        public void addToNested(PrimaryAndNestedSchemas other) {
            this.nestedSchemas.add(other.getPrimarySchema());
            this.nestedSchemas.addAll(other.getNestedSchemas());
        }
    }
}
