package pro.api4.jsonapi4j.plugin;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import pro.api4.jsonapi4j.model.document.data.MultipleResourcesDoc;
import pro.api4.jsonapi4j.plugin.context.MultipleResourcesVisitorContext;

/**
 * Plugin visitor hooks for the multiple-resources processing pipeline.
 * <p>
 * Returned by {@link JsonApi4jPlugin#multipleResourcesVisitors()} and invoked by the framework
 * at four well-defined points during every multi-resource request (e.g. {@code GET /users}):
 * <ol>
 *   <li>{@link #onDataPreRetrieval} — before the resource list is fetched from the operation.</li>
 *   <li>{@link #onDataPostRetrieval} — after the resource list is fetched.</li>
 *   <li>{@link #onRelationshipsPreRetrieval} — after the doc array is built but before relationships are resolved.</li>
 *   <li>{@link #onRelationshipsPostRetrieval} — after all relationships have been resolved.</li>
 * </ol>
 * Each hook returns a typed {@code *Phase} result object that controls pipeline continuation.
 * All methods have default no-op implementations that return {@code DO_NOTHING}.
 */
public interface MultipleResourcesVisitors {

    /**
     * Called before the resource list is retrieved from the operation.
     *
     * @param ctx the visitor context (request, operationMeta, jsonApiContext, pluginInfo)
     * @param <REQUEST>        request type
     * @param <DATA_SOURCE_DTO> downstream DTO type
     * @param <ATTRIBUTES>     attributes type
     * @return a {@link DataPreRetrievalPhase} describing the continuation behaviour
     */
    default <REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> DataPreRetrievalPhase<?> onDataPreRetrieval(
            MultipleResourcesVisitorContext<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> ctx
    ) {
        return DataPreRetrievalPhase.doNothing();
    }

    /**
     * Called after the resource list has been retrieved from the operation.
     *
     * @param ctx the visitor context (request, operationMeta, paginationAwareResponse, jsonApiContext, pluginInfo)
     * @param <REQUEST>        request type
     * @param <DATA_SOURCE_DTO> downstream DTO type
     * @param <ATTRIBUTES>     attributes type
     * @return a {@link DataPostRetrievalPhase} describing the continuation behaviour
     */
    default <REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> DataPostRetrievalPhase<?> onDataPostRetrieval(
            MultipleResourcesVisitorContext<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> ctx
    ) {
        return DataPostRetrievalPhase.doNothing();
    }

    /**
     * Called after the resource array doc is built but before any relationships are resolved.
     *
     * @param ctx the visitor context (request, operationMeta, paginationAwareResponse, doc, jsonApiContext, pluginInfo)
     * @param <REQUEST>        request type
     * @param <DATA_SOURCE_DTO> downstream DTO type
     * @param <ATTRIBUTES>     attributes type
     * @return a {@link RelationshipsPreRetrievalPhase} describing the continuation behaviour
     */
    default <REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> RelationshipsPreRetrievalPhase<?> onRelationshipsPreRetrieval(
            MultipleResourcesVisitorContext<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> ctx
    ) {
        return RelationshipsPreRetrievalPhase.doNothing();
    }

    /**
     * Called after all relationships have been resolved and added to the doc.
     *
     * @param ctx the visitor context (request, operationMeta, paginationAwareResponse, doc, jsonApiContext, pluginInfo)
     * @param <REQUEST>        request type
     * @param <DATA_SOURCE_DTO> downstream DTO type
     * @param <ATTRIBUTES>     attributes type
     * @return a {@link RelationshipsPostRetrievalPhase} describing the continuation behaviour
     */
    default <REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> RelationshipsPostRetrievalPhase<?> onRelationshipsPostRetrieval(
            MultipleResourcesVisitorContext<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> ctx
    ) {
        return RelationshipsPostRetrievalPhase.doNothing();
    }

    /** Result type returned by {@link #onDataPreRetrieval}. */
    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    final class DataPreRetrievalPhase<T> {

        private final DataPreRetrievalPhase.Continuation continuation;
        private T result;

        /** Possible continuation actions after the pre-retrieval hook. */
        public enum Continuation {
            /** Continue processing normally. */
            DO_NOTHING,
            /** Skip data retrieval and return the provided doc immediately. */
            RETURN_DOC,
            /** Replace the request with a mutated version before the operation is invoked. */
            MUTATE_REQUEST
        }

        /** Continue with normal processing. */
        public static DataPreRetrievalPhase<?> doNothing() {
            return new DataPreRetrievalPhase<>(DataPreRetrievalPhase.Continuation.DO_NOTHING);
        }

        /** Short-circuit and return {@code doc} as the response. */
        public static <DOC extends MultipleResourcesDoc<?>> DataPreRetrievalPhase<DOC> returnDoc(DOC doc) {
            return new DataPreRetrievalPhase<>(DataPreRetrievalPhase.Continuation.RETURN_DOC, doc);
        }

        /** Replace the request with {@code mutatedRequest} before the operation is invoked. */
        public static <REQUEST> DataPreRetrievalPhase<REQUEST> mutatedRequest(REQUEST mutatedRequest) {
            return new DataPreRetrievalPhase<>(DataPreRetrievalPhase.Continuation.MUTATE_REQUEST, mutatedRequest);
        }

    }

    /** Result type returned by {@link #onDataPostRetrieval}. */
    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    final class DataPostRetrievalPhase<T> {

        private final DataPostRetrievalPhase.Continuation continuation;
        private T result;

        /** Possible continuation actions after the post-retrieval hook. */
        public enum Continuation {
            /** Continue processing normally. */
            DO_NOTHING,
            /** Skip doc building and return the provided doc immediately. */
            RETURN_DOC,
            /** Replace the request with a mutated version before doc building. */
            MUTATE_REQUEST
        }

        /** Continue with normal processing. */
        public static DataPostRetrievalPhase<?> doNothing() {
            return new DataPostRetrievalPhase<>(DataPostRetrievalPhase.Continuation.DO_NOTHING);
        }

        /** Short-circuit and return {@code doc} as the response. */
        public static <DOC extends MultipleResourcesDoc<?>> DataPostRetrievalPhase<DOC> returnDoc(DOC doc) {
            return new DataPostRetrievalPhase<>(DataPostRetrievalPhase.Continuation.RETURN_DOC, doc);
        }

        /** Replace the request with {@code mutatedRequest} before doc building. */
        public static <REQUEST> DataPostRetrievalPhase<REQUEST> mutatedRequest(REQUEST mutatedRequest) {
            return new DataPostRetrievalPhase<>(DataPostRetrievalPhase.Continuation.MUTATE_REQUEST, mutatedRequest);
        }

    }

    /** Result type returned by {@link #onRelationshipsPreRetrieval}. */
    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    final class RelationshipsPreRetrievalPhase<T> {

        private final RelationshipsPreRetrievalPhase.Continuation continuation;
        private T result;

        /** Possible continuation actions before relationship resolution. */
        public enum Continuation {
            /** Continue processing normally. */
            DO_NOTHING,
            /** Skip relationship resolution and return the provided doc immediately. */
            RETURN_DOC,
            /** Replace the partially-built doc with a mutated version before relationship resolution. */
            MUTATE_DOC
        }

        /** Continue with normal processing. */
        public static RelationshipsPreRetrievalPhase<?> doNothing() {
            return new RelationshipsPreRetrievalPhase<>(RelationshipsPreRetrievalPhase.Continuation.DO_NOTHING);
        }

        /** Short-circuit and return {@code doc} as the response. */
        public static <DOC extends MultipleResourcesDoc<?>> RelationshipsPreRetrievalPhase<DOC> returnDoc(DOC doc) {
            return new RelationshipsPreRetrievalPhase<>(RelationshipsPreRetrievalPhase.Continuation.RETURN_DOC, doc);
        }

        /** Replace the doc with {@code mutatedDoc} before relationship resolution proceeds. */
        public static <DOC extends MultipleResourcesDoc<?>> RelationshipsPreRetrievalPhase<DOC> mutatedDoc(DOC mutatedDoc) {
            return new RelationshipsPreRetrievalPhase<>(RelationshipsPreRetrievalPhase.Continuation.MUTATE_DOC, mutatedDoc);
        }

    }

    /** Result type returned by {@link #onRelationshipsPostRetrieval}. */
    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    final class RelationshipsPostRetrievalPhase<T> {

        private final RelationshipsPostRetrievalPhase.Continuation continuation;
        private T result;

        /** Possible continuation actions after relationship resolution. */
        public enum Continuation {
            /** Continue processing normally — return the doc as-is. */
            DO_NOTHING,
            /** Replace the fully-built doc with the provided doc before returning. */
            RETURN_DOC,
            /** Replace the fully-built doc with a mutated version before returning. */
            MUTATE_DOC
        }

        /** Continue with normal processing. */
        public static RelationshipsPostRetrievalPhase<?> doNothing() {
            return new RelationshipsPostRetrievalPhase<>(RelationshipsPostRetrievalPhase.Continuation.DO_NOTHING);
        }

        /** Return {@code doc} as the response instead of the framework-built doc. */
        public static <DOC extends MultipleResourcesDoc<?>> RelationshipsPostRetrievalPhase<DOC> returnDoc(DOC doc) {
            return new RelationshipsPostRetrievalPhase<>(RelationshipsPostRetrievalPhase.Continuation.RETURN_DOC, doc);
        }

        /** Replace the fully-built doc with {@code mutatedDoc} before returning. */
        public static <DOC extends MultipleResourcesDoc<?>> RelationshipsPostRetrievalPhase<DOC> mutatedDoc(DOC mutatedDoc) {
            return new RelationshipsPostRetrievalPhase<>(RelationshipsPostRetrievalPhase.Continuation.MUTATE_DOC, mutatedDoc);
        }

    }

}
