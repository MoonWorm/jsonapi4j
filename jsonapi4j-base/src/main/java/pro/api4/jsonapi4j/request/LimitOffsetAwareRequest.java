package pro.api4.jsonapi4j.request;


public interface LimitOffsetAwareRequest {

    long DEFAULT_LIMIT = 20;
    long DEFAULT_OFFSET = 0;

    String LIMIT_PARAM = "page[limit]";
    String OFFSET_PARAM = "page[offset]";

    Long getLimit();
    Long getOffset();

}
