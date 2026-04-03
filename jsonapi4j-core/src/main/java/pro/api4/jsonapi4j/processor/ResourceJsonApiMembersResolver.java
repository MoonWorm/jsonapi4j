package pro.api4.jsonapi4j.processor;

import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.model.document.data.RelationshipObject;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipObject;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipObject;
import pro.api4.jsonapi4j.processor.resolvers.*;
import pro.api4.jsonapi4j.processor.util.MappingUtil;

import java.util.Map;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;
import static pro.api4.jsonapi4j.domain.RelationshipType.TO_MANY;
import static pro.api4.jsonapi4j.domain.RelationshipType.TO_ONE;

public abstract class ResourceJsonApiMembersResolver<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> {

    private final ResourceJsonApiContext<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> jsonApiContext;

    public ResourceJsonApiMembersResolver(
            ResourceJsonApiContext<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> jsonApiContext
    ) {
        this.jsonApiContext = jsonApiContext;
        Validate.notNull(jsonApiContext.getAttributesResolver(), "attributesResolver can't be null");
        Validate.notNull(jsonApiContext.getResourceTypeAndIdResolver(), "resourceTypeAndIdResolver can't be null");
        validateRelationshipResolvers();
    }

    protected static <T> T unwrapCompletionException(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (CompletionException ce) {
            Throwable cause = ce.getCause();
            if (cause != null) {
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                }
            }
            throw ce;
        }
    }

    private void validateRelationshipResolvers() {
        jsonApiContext.getDefaultRelationshipResolvers()
                .keySet()
                .stream()
                .filter(rel -> !jsonApiContext.relationshipResolversConfiguredFor(rel))
                .findAny()
                .ifPresent(rel -> {
                    throw new IllegalStateException("Every declared 'default' relationship must also has either " +
                            "'ToOneRelationshipResolver' or 'ToManyRelationshipsResolver' being configured (or their " +
                            "batch alternatives). Missing for '" + rel.getName() + "' relationship.");
                });
    }

    public IdAndType resolveResourceIdAndType(DATA_SOURCE_DTO dataSourceDto) {
        IdAndType idAndType = jsonApiContext.getResourceTypeAndIdResolver().resolveTypeAndId(dataSourceDto);
        Validate.notNull(idAndType, "idAndType can't be null");
        Validate.notNull(idAndType.getId(), "idAndType.id can't be null");
        Validate.notNull(idAndType.getType(), "idAndType.type can't be null");
        return idAndType;
    }

    public ATTRIBUTES resolveAttributes(DATA_SOURCE_DTO dataSourceDto) {
        return MappingUtil.mapSingleLenient(
                dataSourceDto,
                jsonApiContext.getAttributesResolver()::resolveAttributes
        );
    }

    public LinksObject resolveResourceLinks(REQUEST request, DATA_SOURCE_DTO dataSourceDto) {
        return jsonApiContext.getResourceLinksResolver() != null
                ? jsonApiContext.getResourceLinksResolver().resolve(request, dataSourceDto)
                : null;
    }

    public Object resolveResourceMeta(REQUEST request, DATA_SOURCE_DTO dataSourceDto) {
        return jsonApiContext.getResourceMetaResolver() != null
                ? jsonApiContext.getResourceMetaResolver().resolve(request, dataSourceDto)
                : null;
    }

    protected boolean isToOneRelationship(RelationshipName relationshipName) {
        return jsonApiContext.relationshipResolversConfiguredFor(relationshipName, TO_ONE);
    }

    protected boolean isToManyRelationship(RelationshipName relationshipName) {
        return jsonApiContext.relationshipResolversConfiguredFor(relationshipName, TO_MANY);
    }

    protected Map<RelationshipName, DefaultRelationshipResolver<REQUEST, DATA_SOURCE_DTO>> getDefaultRelationshipResolvers() {
        return jsonApiContext.getDefaultRelationshipResolvers();
    }

    protected Map<RelationshipName, ToManyRelationshipResolver<REQUEST, DATA_SOURCE_DTO>> getToManyRelationshipResolvers() {
        return jsonApiContext.getToManyRelationshipResolvers();
    }

    protected Map<RelationshipName, BatchToManyRelationshipResolver<REQUEST, DATA_SOURCE_DTO>> getBatchToManyRelationshipResolvers() {
        return jsonApiContext.getBatchToManyRelationshipResolvers();
    }

    protected Map<RelationshipName, ToOneRelationshipResolver<REQUEST, DATA_SOURCE_DTO>> getToOneRelationshipResolvers() {
        return jsonApiContext.getToOneRelationshipResolvers();
    }

    protected Map<RelationshipName, BatchToOneRelationshipResolver<REQUEST, DATA_SOURCE_DTO>> getBatchToOneRelationshipResolvers() {
        return jsonApiContext.getBatchToOneRelationshipResolvers();
    }

    protected ToOneRelationshipObject createToOneRelationshipWithNullData(
            RelationshipName relationshipName,
            REQUEST request,
            DATA_SOURCE_DTO dataSourceDto
    ) {
        RelationshipObject relationshipObject = getDefaultRelationshipResolvers()
                .get(relationshipName)
                .resolveDefaultRelationship(relationshipName, request, dataSourceDto);
        return ToOneRelationshipObject.fromRelationshipObject(null, relationshipObject);
    }

    protected ToManyRelationshipObject createToManyRelationshipsWithNullData(
            RelationshipName relationshipName,
            REQUEST request,
            DATA_SOURCE_DTO dataSourceDto
    ) {
        RelationshipObject relationshipObject = getDefaultRelationshipResolvers()
                .get(relationshipName)
                .resolveDefaultRelationship(relationshipName, request, dataSourceDto);
        return ToManyRelationshipObject.fromRelationshipObject(null, relationshipObject);
    }

    protected ToManyRelationshipObject createToManyRelationshipsWithEmptyData(
            RelationshipName relationshipName,
            REQUEST request,
            DATA_SOURCE_DTO dataSourceDto
    ) {
        RelationshipObject relationshipObject = getDefaultRelationshipResolvers()
                .get(relationshipName)
                .resolveDefaultRelationship(relationshipName, request, dataSourceDto);
        return ToManyRelationshipObject.fromRelationshipObject(emptyList(), relationshipObject);
    }

}
