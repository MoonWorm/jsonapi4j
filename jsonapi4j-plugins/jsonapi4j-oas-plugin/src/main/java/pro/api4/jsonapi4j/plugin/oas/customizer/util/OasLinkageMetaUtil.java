package pro.api4.jsonapi4j.plugin.oas.customizer.util;

import io.swagger.v3.oas.models.media.Schema;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.domain.RegisteredRelationship;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;
import pro.api4.jsonapi4j.plugin.oas.JsonApiOasPlugin;
import pro.api4.jsonapi4j.plugin.oas.customizer.util.SchemaGeneratorUtil.PrimaryAndNestedSchemas;
import pro.api4.jsonapi4j.plugin.oas.domain.model.NoLinkageMeta;
import pro.api4.jsonapi4j.plugin.oas.domain.model.OasRelationshipInfoModel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.apache.commons.collections4.MapUtils.emptyIfNull;
import static pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject.META_FIELD;
import static pro.api4.jsonapi4j.plugin.oas.customizer.util.OasSchemaNamesUtil.customResourceIdentifierMetaSchemaName;
import static pro.api4.jsonapi4j.plugin.oas.customizer.util.OasSchemaNamesUtil.customResourceIdentifierSchemaName;
import static pro.api4.jsonapi4j.plugin.oas.customizer.util.SchemaGeneratorUtil.generateAllSchemasFromType;
import static pro.api4.jsonapi4j.plugin.oas.customizer.util.SchemaGeneratorUtil.generateSchemaFromType;

/**
 * Resolves the resource linkage meta a relationship declares via
 * {@link pro.api4.jsonapi4j.plugin.oas.domain.annotation.OasRelationshipInfo#resourceLinkageMetaType()}, and builds the
 * resource identifier that carries it.
 * <p>
 * The identifier is shared between requests and responses: {@code meta} on a linkage object has the same shape whether
 * the client sends it or the server returns it, so both sides {@code $ref} the same schema.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public final class OasLinkageMetaUtil {

    private OasLinkageMetaUtil() {
    }

    /**
     * Returns the declared linkage meta type, or {@code null} when the relationship declares none.
     */
    public static Class<?> resolveLinkageMetaType(RegisteredRelationship<?> registeredRelationship) {
        if (registeredRelationship == null) {
            return null;
        }
        Object pluginInfo = emptyIfNull(registeredRelationship.getPluginInfo()).get(JsonApiOasPlugin.NAME);
        if (pluginInfo instanceof OasRelationshipInfoModel oasRelationshipInfo
                && oasRelationshipInfo.getResourceLinkageMetaType() != NoLinkageMeta.class) {
            return oasRelationshipInfo.getResourceLinkageMetaType();
        }
        return null;
    }

    public static Class<?> resolveLinkageMetaType(DomainRegistry domainRegistry,
                                                  ResourceType resourceType,
                                                  RelationshipName relationshipName) {
        if (relationshipName == null) {
            return null;
        }
        return Stream.concat(
                        domainRegistry.getToOneRelationships(resourceType).stream(),
                        domainRegistry.getToManyRelationships(resourceType).stream()
                )
                .filter(relationship -> relationshipName.equals(relationship.getRelationshipName()))
                .map(OasLinkageMetaUtil::resolveLinkageMetaType)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /**
     * Builds the resource identifier whose {@code meta} is typed by {@code linkageMetaType}, together with the schemas
     * that meta type itself pulls in.
     */
    public static PrimaryAndNestedSchemas customResourceIdentifierSchemas(ResourceType resourceType,
                                                                          RelationshipName relationshipName,
                                                                          Class<?> linkageMetaType) {
        Schema<?> resourceIdentifierSchema = generateSchemaFromType(ResourceIdentifierObject.class);
        resourceIdentifierSchema.setName(customResourceIdentifierSchemaName(resourceType, relationshipName));

        PrimaryAndNestedSchemas metaSchemas = generateAllSchemasFromType(linkageMetaType);
        metaSchemas.getPrimarySchema().setName(customResourceIdentifierMetaSchemaName(resourceType, relationshipName));

        resourceIdentifierSchema.getProperties().put(
                META_FIELD,
                new Schema<>().$ref(metaSchemas.getPrimarySchema().getName())
        );

        List<Schema> nested = new ArrayList<>();
        nested.add(metaSchemas.getPrimarySchema());
        nested.addAll(metaSchemas.getNestedSchemas());
        return new PrimaryAndNestedSchemas(resourceIdentifierSchema, nested);
    }

}
