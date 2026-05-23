package pro.api4.jsonapi4j;

import pro.api4.jsonapi4j.request.JsonApiRequest;

public interface JsonApiBuildInRequestValidator {

    JsonApiBuildInRequestValidator NO_OP = new JsonApiBuildInRequestValidator() {

        @Override
        public void validateReadResourceById(JsonApiRequest request) {
            // no op
        }

        @Override
        public void validateReadMultipleResources(JsonApiRequest request) {
            // no op
        }

        @Override
        public void validateCreateResource(JsonApiRequest request) {
            // no op
        }

        @Override
        public void validateUpdateResource(JsonApiRequest request) {
            // no op
        }

        @Override
        public void validateDeleteResource(JsonApiRequest request) {
            // no op
        }

        @Override
        public void validateReadToOneRelationship(JsonApiRequest request) {
            // no op
        }

        @Override
        public void validateUpdateToOneRelationship(JsonApiRequest request) {
            // no op
        }

        @Override
        public void validateReadToManyRelationship(JsonApiRequest request) {
            // no op
        }

        @Override
        public void validateUpdateToManyRelationship(JsonApiRequest request) {
            // no op
        }

        @Override
        public void validateAddToManyRelationship(JsonApiRequest request) {
            // no op
        }

        @Override
        public void validateDeleteToManyRelationship(JsonApiRequest request) {
            // no op
        }
    };

    void validateReadResourceById(JsonApiRequest request);

    void validateReadMultipleResources(JsonApiRequest request);

    void validateCreateResource(JsonApiRequest request);

    void validateUpdateResource(JsonApiRequest request);

    void validateDeleteResource(JsonApiRequest request);

    void validateReadToOneRelationship(JsonApiRequest request);

    void validateUpdateToOneRelationship(JsonApiRequest request);

    void validateReadToManyRelationship(JsonApiRequest request);

    void validateUpdateToManyRelationship(JsonApiRequest request);

    void validateAddToManyRelationship(JsonApiRequest request);

    void validateDeleteToManyRelationship(JsonApiRequest request);

}
