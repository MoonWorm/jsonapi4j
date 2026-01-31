package pro.api4.jsonapi4j.plugin;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import pro.api4.jsonapi4j.model.document.data.MultipleResourcesDoc;
import pro.api4.jsonapi4j.processor.CursorPageableResponse;
import pro.api4.jsonapi4j.processor.multi.resource.MultipleResourcesJsonApiContext;

public interface MultipleResourcesVisitors {

    default <REQUEST> DataPreRetrievalPhase<?> onDataPreRetrieval(
            REQUEST request,
            MultipleResourcesJsonApiContext<REQUEST, ?, ?> context,
            JsonApiPluginInfo pluginInfo
    ) {
        return DataPreRetrievalPhase.doNothing();
    }

    default <REQUEST, DATA_SOURCE_DTO> DataPostRetrievalPhase<?> onDataPostRetrieval(
            REQUEST request,
            CursorPageableResponse<DATA_SOURCE_DTO> cursorPageableResponse,
            MultipleResourcesJsonApiContext<REQUEST, DATA_SOURCE_DTO, ?> context,
            JsonApiPluginInfo pluginInfo
    ) {
        return DataPostRetrievalPhase.doNothing();
    }

    default <REQUEST, DATA_SOURCE_DTO, DOC extends MultipleResourcesDoc<?>> RelationshipsPreRetrievalPhase<?> onRelationshipsPreRetrieval(
            REQUEST request,
            CursorPageableResponse<DATA_SOURCE_DTO> cursorPageableResponse,
            DOC doc,
            MultipleResourcesJsonApiContext<REQUEST, DATA_SOURCE_DTO, ?> context,
            JsonApiPluginInfo pluginInfo
    ) {
        return RelationshipsPreRetrievalPhase.doNothing();
    }

    default <REQUEST, DATA_SOURCE_DTO, DOC extends MultipleResourcesDoc<?>> RelationshipsPostRetrievalPhase<?> onRelationshipsPostRetrieval(
            REQUEST request,
            CursorPageableResponse<DATA_SOURCE_DTO> cursorPageableResponse,
            DOC doc,
            MultipleResourcesJsonApiContext<REQUEST, DATA_SOURCE_DTO, ?> context,
            JsonApiPluginInfo pluginInfo
    ) {
        return RelationshipsPostRetrievalPhase.doNothing();
    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    final class DataPreRetrievalPhase<T> {

        private final DataPreRetrievalPhase.Continuation continuation;
        private T result;

        public enum Continuation {
            DO_NOTHING, RETURN_DOC, MUTATE_REQUEST
        }

        public static DataPreRetrievalPhase<?> doNothing() {
            return new DataPreRetrievalPhase<>(DataPreRetrievalPhase.Continuation.DO_NOTHING);
        }

        public static <DOC extends MultipleResourcesDoc<?>> DataPreRetrievalPhase<DOC> returnDoc(DOC doc) {
            return new DataPreRetrievalPhase<>(DataPreRetrievalPhase.Continuation.RETURN_DOC, doc);
        }

        public static <REQUEST> DataPreRetrievalPhase<REQUEST> mutatedRequest(REQUEST mutatedRequest) {
            return new DataPreRetrievalPhase<>(DataPreRetrievalPhase.Continuation.MUTATE_REQUEST, mutatedRequest);
        }

    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    final class DataPostRetrievalPhase<T> {

        private final DataPostRetrievalPhase.Continuation continuation;
        private T result;

        public enum Continuation {
            DO_NOTHING, RETURN_DOC, MUTATE_REQUEST
        }

        public static DataPostRetrievalPhase<?> doNothing() {
            return new DataPostRetrievalPhase<>(DataPostRetrievalPhase.Continuation.DO_NOTHING);
        }

        public static <DOC extends MultipleResourcesDoc<?>> DataPostRetrievalPhase<DOC> returnDoc(DOC doc) {
            return new DataPostRetrievalPhase<>(DataPostRetrievalPhase.Continuation.RETURN_DOC, doc);
        }

        public static <REQUEST> DataPostRetrievalPhase<REQUEST> mutatedRequest(REQUEST mutatedRequest) {
            return new DataPostRetrievalPhase<>(DataPostRetrievalPhase.Continuation.MUTATE_REQUEST, mutatedRequest);
        }

    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    final class RelationshipsPreRetrievalPhase<T> {

        private final RelationshipsPreRetrievalPhase.Continuation continuation;
        private T result;

        public enum Continuation {
            DO_NOTHING, RETURN_DOC, MUTATE_DOC
        }

        public static RelationshipsPreRetrievalPhase<?> doNothing() {
            return new RelationshipsPreRetrievalPhase<>(RelationshipsPreRetrievalPhase.Continuation.DO_NOTHING);
        }

        public static <DOC extends MultipleResourcesDoc<?>> RelationshipsPreRetrievalPhase<DOC> returnDoc(DOC doc) {
            return new RelationshipsPreRetrievalPhase<>(RelationshipsPreRetrievalPhase.Continuation.RETURN_DOC, doc);
        }

        public static <DOC extends MultipleResourcesDoc<?>> RelationshipsPreRetrievalPhase<DOC> mutatedDoc(DOC mutatedDoc) {
            return new RelationshipsPreRetrievalPhase<>(RelationshipsPreRetrievalPhase.Continuation.MUTATE_DOC, mutatedDoc);
        }

    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    final class RelationshipsPostRetrievalPhase<T> {

        private final RelationshipsPostRetrievalPhase.Continuation continuation;
        private T result;

        public enum Continuation {
            DO_NOTHING, RETURN_DOC, MUTATE_DOC
        }

        public static RelationshipsPostRetrievalPhase<?> doNothing() {
            return new RelationshipsPostRetrievalPhase<>(RelationshipsPostRetrievalPhase.Continuation.DO_NOTHING);
        }

        public static <DOC extends MultipleResourcesDoc<?>> RelationshipsPostRetrievalPhase<DOC> returnDoc(DOC doc) {
            return new RelationshipsPostRetrievalPhase<>(RelationshipsPostRetrievalPhase.Continuation.RETURN_DOC, doc);
        }

        public static <DOC extends MultipleResourcesDoc<?>> RelationshipsPostRetrievalPhase<DOC> mutatedDoc(DOC mutatedDoc) {
            return new RelationshipsPostRetrievalPhase<>(RelationshipsPostRetrievalPhase.Continuation.MUTATE_DOC, mutatedDoc);
        }

    }

}
