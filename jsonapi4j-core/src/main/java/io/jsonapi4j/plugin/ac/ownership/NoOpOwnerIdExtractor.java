package io.jsonapi4j.plugin.ac.ownership;

import io.jsonapi4j.request.JsonApiRequest;

public class NoOpOwnerIdExtractor implements OwnerIdExtractor<JsonApiRequest> {

    @Override
    public String fromRequest(JsonApiRequest request) {
        return null;
    }

}
