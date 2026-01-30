package pro.api4.jsonapi4j.plugin;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipDoc;
import pro.api4.jsonapi4j.processor.single.relationship.ToOneRelationshipJsonApiContext;

public interface ToOneRelationshipVisitors {

    default <REQUEST> DataPreRetrievalPhase<?> onDataPreRetrieval(
            REQUEST request,
            ToOneRelationshipJsonApiContext<REQUEST, ?> context,
            JsonApiPluginInfo pluginInfo
    ) {
        return DataPreRetrievalPhase.doNothing();
    }

    default <REQUEST, DATA_SOURCE_DTO> DataPostRetrievalPhase<?> onDataPostRetrieval(
            REQUEST request,
            DATA_SOURCE_DTO dataSourceDto,
            ToOneRelationshipDoc doc,
            ToOneRelationshipJsonApiContext<REQUEST, DATA_SOURCE_DTO> context,
            JsonApiPluginInfo pluginInfo
    ) {
        return DataPostRetrievalPhase.doNothing();
    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    final class DataPreRetrievalPhase<T> {

        private final Continuation continuation;
        private T result;

        public enum Continuation {
            DO_NOTHING, RETURN_DOC, MUTATE_REQUEST
        }

        public static DataPreRetrievalPhase<?> doNothing() {
            return new DataPreRetrievalPhase<>(DataPreRetrievalPhase.Continuation.DO_NOTHING);
        }

        public static DataPreRetrievalPhase<ToOneRelationshipDoc> returnDoc(ToOneRelationshipDoc doc) {
            return new DataPreRetrievalPhase<>(Continuation.RETURN_DOC, doc);
        }

        public static <REQUEST> DataPreRetrievalPhase<REQUEST> mutatedRequest(REQUEST mutatedRequest) {
            return new DataPreRetrievalPhase<>(Continuation.MUTATE_REQUEST, mutatedRequest);
        }

    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    final class DataPostRetrievalPhase<T> {

        private final Continuation continuation;
        private T result;

        public enum Continuation {
            DO_NOTHING, RETURN_DOC, MUTATE_DOC
        }

        public static DataPostRetrievalPhase<?> doNothing() {
            return new DataPostRetrievalPhase<>(Continuation.DO_NOTHING);
        }

        public static DataPostRetrievalPhase<ToOneRelationshipDoc> returnDoc(ToOneRelationshipDoc doc) {
            return new DataPostRetrievalPhase<>(Continuation.RETURN_DOC, doc);
        }

        public static DataPostRetrievalPhase<ToOneRelationshipDoc> mutatedDoc(ToOneRelationshipDoc mutatedDoc) {
            return new DataPostRetrievalPhase<>(Continuation.MUTATE_DOC, mutatedDoc);
        }

    }

}
