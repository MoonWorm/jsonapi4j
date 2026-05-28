package pro.api4.jsonapi4j.plugin;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import pro.api4.jsonapi4j.operation.OperationMeta;
import pro.api4.jsonapi4j.processor.multi.relationship.ToManyRelationshipsJsonApiContext;
import pro.api4.jsonapi4j.response.PaginationAwareResponse;

/**
 * Plugin visitor hooks for the to-many relationship processing pipeline.
 * <p>
 * Returned by {@link JsonApi4jPlugin#toManyRelationshipVisitors()} and invoked by the framework
 * at two well-defined points during every to-many relationship request
 * (e.g. {@code GET /users/1/relationships/citizenships}):
 * <ol>
 *   <li>{@link #onDataPreRetrieval} — before the relationship list is fetched from the operation.</li>
 *   <li>{@link #onDataPostRetrieval} — after the relationship list is fetched and the doc is built.</li>
 * </ol>
 * All methods have default no-op implementations that return {@code DO_NOTHING}.
 */
public interface ToManyRelationshipVisitors {

    /**
     * Called before the relationship list is retrieved from the operation.
     *
     * @param request       the original request
     * @param operationMeta metadata about the current operation
     * @param context       the processing context containing all configured resolvers
     * @param pluginInfo    pre-extracted plugin-specific metadata
     * @param <REQUEST>     request type
     * @return a {@link DataPreRetrievalPhase} describing the continuation behaviour
     */
    default <REQUEST> DataPreRetrievalPhase<?> onDataPreRetrieval(
            REQUEST request,
            OperationMeta operationMeta,
            ToManyRelationshipsJsonApiContext<REQUEST, ?> context,
            JsonApiPluginInfo pluginInfo
    ) {
        return DataPreRetrievalPhase.doNothing();
    }

    /**
     * Called after the relationship list has been retrieved and the {@link ToManyRelationshipsDoc} is built.
     *
     * @param request                  the original request
     * @param operationMeta            metadata about the current operation
     * @param paginationAwareResponse  the paginated response returned by the operation
     * @param doc                      the built to-many relationships doc
     * @param context                  the processing context containing all configured resolvers
     * @param pluginInfo               pre-extracted plugin-specific metadata
     * @param <REQUEST>                request type
     * @param <DATA_SOURCE_DTO>        downstream relationship DTO type
     * @return a {@link DataPostRetrievalPhase} describing the continuation behaviour
     */
    default <REQUEST, DATA_SOURCE_DTO> DataPostRetrievalPhase<?> onDataPostRetrieval(
            REQUEST request,
            OperationMeta operationMeta,
            PaginationAwareResponse<DATA_SOURCE_DTO> paginationAwareResponse,
            ToManyRelationshipsDoc doc,
            ToManyRelationshipsJsonApiContext<REQUEST, DATA_SOURCE_DTO> context,
            JsonApiPluginInfo pluginInfo
    ) {
        return DataPostRetrievalPhase.doNothing();
    }

    /** Result type returned by {@link #onDataPreRetrieval}. */
    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    final class DataPreRetrievalPhase<T> {

        private final Continuation continuation;
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
            return new DataPreRetrievalPhase<>(Continuation.DO_NOTHING);
        }

        /** Short-circuit and return {@code doc} as the response. */
        public static DataPreRetrievalPhase<ToManyRelationshipsDoc> returnDoc(ToManyRelationshipsDoc doc) {
            return new DataPreRetrievalPhase<>(Continuation.RETURN_DOC, doc);
        }

        /** Replace the request with {@code mutatedRequest} before the operation is invoked. */
        public static <REQUEST> DataPreRetrievalPhase<REQUEST> mutatedRequest(REQUEST mutatedRequest) {
            return new DataPreRetrievalPhase<>(Continuation.MUTATE_REQUEST, mutatedRequest);
        }

    }

    /** Result type returned by {@link #onDataPostRetrieval}. */
    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    final class DataPostRetrievalPhase<T> {

        private final Continuation continuation;
        private T result;

        /** Possible continuation actions after the post-retrieval hook. */
        public enum Continuation {
            /** Continue processing normally — return the built doc. */
            DO_NOTHING,
            /** Return the provided doc instead of the framework-built one. */
            RETURN_DOC,
            /** Replace the built doc with a mutated version before returning. */
            MUTATE_DOC
        }

        /** Continue with normal processing. */
        public static DataPostRetrievalPhase<?> doNothing() {
            return new DataPostRetrievalPhase<>(Continuation.DO_NOTHING);
        }

        /** Return {@code doc} as the response instead of the framework-built doc. */
        public static DataPostRetrievalPhase<ToManyRelationshipsDoc> returnDoc(ToManyRelationshipsDoc doc) {
            return new DataPostRetrievalPhase<>(Continuation.RETURN_DOC, doc);
        }

        /** Replace the built doc with {@code mutatedDoc} before returning. */
        public static DataPostRetrievalPhase<ToManyRelationshipsDoc> mutatedDoc(ToManyRelationshipsDoc mutatedDoc) {
            return new DataPostRetrievalPhase<>(Continuation.MUTATE_DOC, mutatedDoc);
        }

    }

}
