package pro.api4.jsonapi4j.plugin.oas.customizer.util;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;
import pro.api4.jsonapi4j.model.document.data.ResourceObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import static pro.api4.jsonapi4j.model.document.data.ResourceObject.LINKS_FIELD;

@SuppressWarnings("rawtypes")
public final class SchemaGeneratorUtil {

    private static final ModelConverters MODEL_CONVERTERS = ModelConverters.getInstance();

    private SchemaGeneratorUtil() {
    }

    public static Schema<?> generateSchemaFromType(Class<?> clazz) {
        return MODEL_CONVERTERS.read(clazz).values().stream().findFirst().get();
    }

    /**
     * The schemas the plugin writes by hand and publishes once, described. Reflection reaches them too - a document
     * class declares a {@code links} member and a list of resource identifiers - but the copies it produces carry no
     * descriptions, and {@link LinksObject} additionally gains an {@code empty} boolean off its {@code isEmpty()}
     * accessor. Both are dropped here, at the one place nested schemas are produced, so that no caller has to
     * remember to.
     */
    private static final Set<String> HAND_WRITTEN_SCHEMA_NAMES = Set.of(
            LinksObject.class.getSimpleName(),
            ResourceIdentifierObject.class.getSimpleName()
    );

    public static PrimaryAndNestedSchemas generateAllSchemasFromType(Class<?> clazz) {
        Schema<?> parentSchema = generateSchemaFromType(clazz);
        Collection<Schema> parentSchemaWithNested = MODEL_CONVERTERS.readAll(clazz).values();
        List<Schema> parentSchemaWithoutNested = parentSchemaWithNested
                .stream()
                .filter(s -> !s.getName().equals(parentSchema.getName()))
                .filter(s -> !HAND_WRITTEN_SCHEMA_NAMES.contains(s.getName()))
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

    /**
     * Registers a schema under its name, refusing a second schema of a different shape under the same one. Names come
     * from Java simple names, so nothing stops two resources from carrying a nested {@code Address} apiece; first-write
     * -wins would publish one of them under the other's shape and say nothing about it.
     */
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
        Schema<?> registered = openApi.getComponents().getSchemas().get(schemaToAdd.getName());
        if (registered == null) {
            openApi.getComponents().getSchemas().put(schemaToAdd.getName(), schemaToAdd);
            return;
        }
        if (!registered.equals(schemaToAdd)) {
            throw new IllegalStateException(String.format(
                    "Two different schemas claim the name '%s'. Schema names are derived from Java simple names, so "
                            + "two resources carrying same-named nested types collide - one would silently win and the "
                            + "other would be published with the wrong shape. Rename one of the Java types.",
                    schemaToAdd.getName()
            ));
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
