package pro.api4.jsonapi4j.plugin.ac.ownership;

public interface OwnerIdExtractor<REQUEST> {

    String fromRequest(REQUEST request);

}
