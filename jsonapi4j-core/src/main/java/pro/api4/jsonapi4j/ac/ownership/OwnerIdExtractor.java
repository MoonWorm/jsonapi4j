package pro.api4.jsonapi4j.ac.ownership;

public interface OwnerIdExtractor<REQUEST> {

    String fromRequest(REQUEST request);

}
