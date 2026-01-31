package pro.api4.jsonapi4j.plugin;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import pro.api4.jsonapi4j.model.document.data.SingleResourceDoc;
import pro.api4.jsonapi4j.processor.single.resource.SingleResourceJsonApiContext;

public interface SingleResourceVisitors {

    default <REQUEST> DataPreRetrievalPhase<?> onDataPreRetrieval(
            REQUEST request,
            SingleResourceJsonApiContext<REQUEST, ?, ?> context,
            JsonApiPluginInfo pluginInfo
    ) {
        return DataPreRetrievalPhase.doNothing();
    }

    default <REQUEST, DATA_SOURCE_DTO> DataPostRetrievalPhase<?> onDataPostRetrieval(
            REQUEST request,
            DATA_SOURCE_DTO dataSourceDto,
            SingleResourceJsonApiContext<REQUEST, DATA_SOURCE_DTO, ?> context,
            JsonApiPluginInfo pluginInfo
    ) {
        return DataPostRetrievalPhase.doNothing();
    }

    default <REQUEST, DATA_SOURCE_DTO, DOC extends SingleResourceDoc<?>> RelationshipsPreRetrievalPhase<?> onRelationshipsPreRetrieval(
            REQUEST request,
            DATA_SOURCE_DTO dataSourceDto,
            DOC doc,
            SingleResourceJsonApiContext<REQUEST, DATA_SOURCE_DTO, ?> context,
            JsonApiPluginInfo pluginInfo
    ) {
        return RelationshipsPreRetrievalPhase.doNothing();
    }

    default <REQUEST, DATA_SOURCE_DTO, DOC extends SingleResourceDoc<?>> RelationshipsPostRetrievalPhase<?> onRelationshipsPostRetrieval(
            REQUEST request,
            DATA_SOURCE_DTO dataSourceDto,
            DOC doc,
            SingleResourceJsonApiContext<REQUEST, DATA_SOURCE_DTO, ?> context,
            JsonApiPluginInfo pluginInfo
    ) {
        return RelationshipsPostRetrievalPhase.doNothing();
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
            return new DataPreRetrievalPhase<>(Continuation.DO_NOTHING);
        }

        public static <DOC extends SingleResourceDoc<?>> DataPreRetrievalPhase<DOC> returnDoc(DOC doc) {
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
            DO_NOTHING, RETURN_DOC, MUTATE_REQUEST
        }

        public static DataPostRetrievalPhase<?> doNothing() {
            return new DataPostRetrievalPhase<>(Continuation.DO_NOTHING);
        }

        public static <DOC extends SingleResourceDoc<?>> DataPostRetrievalPhase<DOC> returnDoc(DOC doc) {
            return new DataPostRetrievalPhase<>(Continuation.RETURN_DOC, doc);
        }

        public static <REQUEST> DataPostRetrievalPhase<REQUEST> mutatedRequest(REQUEST mutatedRequest) {
            return new DataPostRetrievalPhase<>(Continuation.MUTATE_REQUEST, mutatedRequest);
        }

    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    final class RelationshipsPreRetrievalPhase<T> {

        private final Continuation continuation;
        private T result;

        public enum Continuation {
            DO_NOTHING, RETURN_DOC, MUTATE_DOC
        }

        public static RelationshipsPreRetrievalPhase<?> doNothing() {
            return new RelationshipsPreRetrievalPhase<>(Continuation.DO_NOTHING);
        }

        public static <DOC extends SingleResourceDoc<?>> RelationshipsPreRetrievalPhase<DOC> returnDoc(DOC doc) {
            return new RelationshipsPreRetrievalPhase<>(Continuation.RETURN_DOC, doc);
        }

        public static <DOC extends SingleResourceDoc<?>> RelationshipsPreRetrievalPhase<DOC> mutatedDoc(DOC mutatedDoc) {
            return new RelationshipsPreRetrievalPhase<>(Continuation.MUTATE_DOC, mutatedDoc);
        }

    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    @RequiredArgsConstructor(access = AccessLevel.PRIVATE)
    final class RelationshipsPostRetrievalPhase<T> {

        private final Continuation continuation;
        private T result;

        public enum Continuation {
            DO_NOTHING, RETURN_DOC, MUTATE_DOC
        }

        public static RelationshipsPostRetrievalPhase<?> doNothing() {
            return new RelationshipsPostRetrievalPhase<>(Continuation.DO_NOTHING);
        }

        public static <DOC extends SingleResourceDoc<?>> RelationshipsPostRetrievalPhase<DOC> returnDoc(DOC doc) {
            return new RelationshipsPostRetrievalPhase<>(Continuation.RETURN_DOC, doc);
        }

        public static <DOC extends SingleResourceDoc<?>> RelationshipsPostRetrievalPhase<DOC> mutatedDoc(DOC mutatedDoc) {
            return new RelationshipsPostRetrievalPhase<>(Continuation.MUTATE_DOC, mutatedDoc);
        }

    }

}
