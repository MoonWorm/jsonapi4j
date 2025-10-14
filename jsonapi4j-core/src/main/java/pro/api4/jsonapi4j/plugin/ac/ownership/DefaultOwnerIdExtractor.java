package pro.api4.jsonapi4j.plugin.ac.ownership;

import pro.api4.jsonapi4j.request.JsonApiRequest;

public class DefaultOwnerIdExtractor implements OwnerIdExtractor<JsonApiRequest> {

    @Override
    public String fromRequest(JsonApiRequest request) {
        return String.valueOf(request.getResourceId());
    }

}
