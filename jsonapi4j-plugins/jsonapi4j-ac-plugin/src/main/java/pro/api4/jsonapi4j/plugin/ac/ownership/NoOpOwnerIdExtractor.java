package pro.api4.jsonapi4j.plugin.ac.ownership;

public class NoOpOwnerIdExtractor implements OwnerIdExtractor<Object> {

    @Override
    public String fromRequest(Object request) {
        return null;
    }

}
