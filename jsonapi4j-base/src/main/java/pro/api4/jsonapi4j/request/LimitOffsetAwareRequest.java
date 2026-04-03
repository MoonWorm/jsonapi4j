package pro.api4.jsonapi4j.request;


public interface LimitOffsetAwareRequest {

    String LIMIT_PARAM = "page[limit]";
    String OFFSET_PARAM = "page[offset]";

    Long getLimit();
    Long getOffset();

}
