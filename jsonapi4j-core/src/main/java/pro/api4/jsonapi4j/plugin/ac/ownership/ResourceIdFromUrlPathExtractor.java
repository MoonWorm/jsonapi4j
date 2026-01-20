package pro.api4.jsonapi4j.plugin.ac.ownership;

import pro.api4.jsonapi4j.request.JsonApiRequest;

public class ResourceIdFromUrlPathExtractor implements OwnerIdExtractor<JsonApiRequest> {

    @Override
    public String fromRequest(JsonApiRequest request) {
        if (request == null) {
            return null;
        }
        return String.valueOf(request.getResourceId());
    }

}
