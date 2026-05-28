package pro.api4.jsonapi4j.plugin;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import pro.api4.jsonapi4j.model.document.data.SingleResourceDoc;
import pro.api4.jsonapi4j.operation.OperationMeta;
import pro.api4.jsonapi4j.processor.single.resource.SingleResourceJsonApiContext;

/**
 * Plugin visitor hooks for the single-resource processing pipeline.
 * <p>
 * Returned by {@link JsonApi4jPlugin#singleResourceVisitors()} and invoked by the framework
 * at four well-defined points during every single-resource request
 * (e.g. {@code GET /users/1}, {@code POST /users}, etc.):
 * <ol>
 *   <li>{@link #onDataPreRetrieval} — before the primary data is fetched from the operation.</li>
 *   <li>{@link #onDataPostRetrieval} — after the primary data is fetched.</li>
 *   <li>{@link #onRelationshipsPreRetrieval} — after the doc is built but before relationships are resolved.</li>
 *   <li>{@link #onRelationshipsPostRetrieval} — after all relationships have been resolved.</li>
 * </ol>
 * Each hook returns a typed {@code *Phase} result object that tells the framework whether to
 * continue normally ({@code DO_NOTHING}), short-circuit and return a specific doc ({@code RETURN_DOC}),
 * or substitute a mutated request/doc ({@code MUTATE_REQUEST} / {@code MUTATE_DOC}).
 * <p>
 * All methods have default no-op implementations that return {@code DO_NOTHING}.
 */
public interface SingleResourceVisitors {

    /**
     * Called before the primary resource DTO is retrieved from the operation.
     * <p>
     * Can be used to short-circuit the entire pipeline (e.g. return a cached doc) or
     * to substitute a mutated request before the operation is invoked.
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
            SingleResourceJsonApiContext<REQUEST, ?, ?> context,
            JsonApiPluginInfo pluginInfo
    ) {
        return DataPreRetrievalPhase.doNothing();
    }

    /**
     * Called after the primary resource DTO has been retrieved from the operation.
     * <p>
     * Can be used to inspect or transform the retrieved DTO, or to short-circuit
     * relationship resolution by returning a pre-built doc.
     *
     * @param request       the original request
     * @param operationMeta metadata about the current operation
     * @param dataSourceDto the primary resource DTO returned by the operation
     * @param context       the processing context containing all configured resolvers
     * @param pluginInfo    pre-extracted plugin-specific metadata
     * @param <REQUEST>     request type
     * @param <DATA_SOURCE_DTO> downstream DTO type
     * @return a {@link DataPostRetrievalPhase} describing the continuation behaviour
     */
    default <REQUEST, DATA_SOURCE_DTO> DataPostRetrievalPhase<?> onDataPostRetrieval(
            REQUEST request,
            OperationMeta operationMeta,
            DATA_SOURCE_DTO dataSourceDto,
            SingleResourceJsonApiContext<REQUEST, DATA_SOURCE_DTO, ?> context,
            JsonApiPluginInfo pluginInfo
    ) {
        return DataPostRetrievalPhase.doNothing();
    }

    /**
     * Called after the primary resource doc (attributes + resource-level links/meta) is built
     * but before any relationships are resolved.
     * <p>
     * Can be used to modify the partially-built doc or to skip relationship resolution entirely.
     *
     * @param request       the original request
     * @param operationMeta metadata about the current operation
     * @param dataSourceDto the primary resource DTO
     * @param doc           the partially-built single-resource doc (no relationships yet)
     * @param context       the processing context containing all configured resolvers
     * @param pluginInfo    pre-extracted plugin-specific metadata
     * @param <REQUEST>     request type
     * @param <DATA_SOURCE_DTO> downstream DTO type
     * @param <DOC>         concrete doc type
     * @return a {@link RelationshipsPreRetrievalPhase} describing the continuation behaviour
     */
    default <REQUEST, DATA_SOURCE_DTO, DOC extends SingleResourceDoc<?>> RelationshipsPreRetrievalPhase<?> onRelationshipsPreRetrieval(
            REQUEST request,
            OperationMeta operationMeta,
            DATA_SOURCE_DTO dataSourceDto,
            DOC doc,
            SingleResourceJsonApiContext<REQUEST, DATA_SOURCE_DTO, ?> context,
            JsonApiPluginInfo pluginInfo
    ) {
        return RelationshipsPreRetrievalPhase.doNothing();
    }

    /**
     * Called after all relationships have been resolved and added to the doc.
     * <p>
     * Can be used to post-process the fully-built doc (e.g. anonymize fields)
     * or to replace it entirely.
     *
     * @param request       the original request
     * @param operationMeta metadata about the current operation
     * @param dataSourceDto the primary resource DTO
     * @param doc           the fully-built single-resource doc (including relationships)
     * @param context       the processing context containing all configured resolvers
     * @param pluginInfo    pre-extracted plugin-specific metadata
     * @param <REQUEST>     request type
     * @param <DATA_SOURCE_DTO> downstream DTO type
     * @param <DOC>         concrete doc type
     * @return a {@link RelationshipsPostRetrievalPhase} describing the continuation behaviour
     */
    default <REQUEST, DATA_SOURCE_DTO, DOC extends SingleResourceDoc<?>> RelationshipsPostRetrievalPhase<?> onRelationshipsPostRetrieval(
            REQUEST request,
            OperationMeta operationMeta,
            DATA_SOURCE_DTO dataSourceDto,
            DOC doc,
            SingleResourceJsonApiContext<REQUEST, DATA_SOURCE_DTO, ?> context,
            JsonApiPluginInfo pluginInfo
    ) {
        return RelationshipsPostRetrievalPhase.doNothing();
    }

    /**
     * Result type returned by {@link #onDataPreRetrieval}.
     * Instructs the framework on how to continue after the pre-retrieval hook.
     */
    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    final class DataPreRetrievalPhase<T> {

        private final Continuation continuation;
        private T result;

        /** Possible continuation actions after the pre-retrieval hook. */
        public enum Continuation {
            /** Continue processing normally — call the operation to fetch data. */
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

        /**
         * Short-circuit: skip the operation call and return {@code doc} directly as the response.
         *
         * @param doc the doc to return
         */
        public static <DOC extends SingleResourceDoc<?>> DataPreRetrievalPhase<DOC> returnDoc(DOC doc) {
            return new DataPreRetrievalPhase<>(Continuation.RETURN_DOC, doc);
        }

        /**
         * Replace the request with {@code mutatedRequest} before the operation is invoked.
         *
         * @param mutatedRequest the replacement request
         */
        public static <REQUEST> DataPreRetrievalPhase<REQUEST> mutatedRequest(REQUEST mutatedRequest) {
            return new DataPreRetrievalPhase<>(Continuation.MUTATE_REQUEST, mutatedRequest);
        }

    }

    /**
     * Result type returned by {@link #onDataPostRetrieval}.
     * Instructs the framework on how to continue after the post-retrieval hook.
     */
    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    final class DataPostRetrievalPhase<T> {

        private final Continuation continuation;
        private T result;

        /** Possible continuation actions after the post-retrieval hook. */
        public enum Continuation {
            /** Continue processing normally — build the doc from the retrieved DTO. */
            DO_NOTHING,
            /** Skip doc building and return the provided doc immediately. */
            RETURN_DOC,
            /** Replace the request with a mutated version before doc building. */
            MUTATE_REQUEST
        }

        /** Continue with normal processing. */
        public static DataPostRetrievalPhase<?> doNothing() {
            return new DataPostRetrievalPhase<>(Continuation.DO_NOTHING);
        }

        /**
         * Short-circuit: skip doc building and return {@code doc} directly as the response.
         *
         * @param doc the doc to return
         */
        public static <DOC extends SingleResourceDoc<?>> DataPostRetrievalPhase<DOC> returnDoc(DOC doc) {
            return new DataPostRetrievalPhase<>(Continuation.RETURN_DOC, doc);
        }

        /**
         * Replace the request with {@code mutatedRequest} before doc building.
         *
         * @param mutatedRequest the replacement request
         */
        public static <REQUEST> DataPostRetrievalPhase<REQUEST> mutatedRequest(REQUEST mutatedRequest) {
            return new DataPostRetrievalPhase<>(Continuation.MUTATE_REQUEST, mutatedRequest);
        }

    }

    /**
     * Result type returned by {@link #onRelationshipsPreRetrieval}.
     * Instructs the framework on how to continue before relationship resolution.
     */
    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    final class RelationshipsPreRetrievalPhase<T> {

        private final Continuation continuation;
        private T result;

        /** Possible continuation actions before relationship resolution. */
        public enum Continuation {
            /** Continue processing normally — proceed with relationship resolution. */
            DO_NOTHING,
            /** Skip relationship resolution and return the provided doc immediately. */
            RETURN_DOC,
            /** Replace the partially-built doc with a mutated version before relationship resolution. */
            MUTATE_DOC
        }

        /** Continue with normal processing. */
        public static RelationshipsPreRetrievalPhase<?> doNothing() {
            return new RelationshipsPreRetrievalPhase<>(Continuation.DO_NOTHING);
        }

        /**
         * Short-circuit: skip relationship resolution and return {@code doc} as the response.
         *
         * @param doc the doc to return
         */
        public static <DOC extends SingleResourceDoc<?>> RelationshipsPreRetrievalPhase<DOC> returnDoc(DOC doc) {
            return new RelationshipsPreRetrievalPhase<>(Continuation.RETURN_DOC, doc);
        }

        /**
         * Replace the doc with {@code mutatedDoc} before relationship resolution proceeds.
         *
         * @param mutatedDoc the replacement doc
         */
        public static <DOC extends SingleResourceDoc<?>> RelationshipsPreRetrievalPhase<DOC> mutatedDoc(DOC mutatedDoc) {
            return new RelationshipsPreRetrievalPhase<>(Continuation.MUTATE_DOC, mutatedDoc);
        }

    }

    /**
     * Result type returned by {@link #onRelationshipsPostRetrieval}.
     * Instructs the framework on how to continue after relationship resolution.
     */
    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    final class RelationshipsPostRetrievalPhase<T> {

        private final Continuation continuation;
        private T result;

        /** Possible continuation actions after relationship resolution. */
        public enum Continuation {
            /** Continue processing normally — finalize and return the doc as-is. */
            DO_NOTHING,
            /** Replace the fully-built doc with the provided doc before returning. */
            RETURN_DOC,
            /** Replace the fully-built doc with a mutated version before returning. */
            MUTATE_DOC
        }

        /** Continue with normal processing. */
        public static RelationshipsPostRetrievalPhase<?> doNothing() {
            return new RelationshipsPostRetrievalPhase<>(Continuation.DO_NOTHING);
        }

        /**
         * Return {@code doc} as the response instead of the framework-built doc.
         *
         * @param doc the doc to return
         */
        public static <DOC extends SingleResourceDoc<?>> RelationshipsPostRetrievalPhase<DOC> returnDoc(DOC doc) {
            return new RelationshipsPostRetrievalPhase<>(Continuation.RETURN_DOC, doc);
        }

        /**
         * Replace the fully-built doc with {@code mutatedDoc} before returning.
         *
         * @param mutatedDoc the replacement doc
         */
        public static <DOC extends SingleResourceDoc<?>> RelationshipsPostRetrievalPhase<DOC> mutatedDoc(DOC mutatedDoc) {
            return new RelationshipsPostRetrievalPhase<>(Continuation.MUTATE_DOC, mutatedDoc);
        }

    }

}
