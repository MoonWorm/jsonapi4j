package pro.api4.jsonapi4j.request;

@FunctionalInterface
public interface JsonApiRequestSupplier<T> {

    JsonApiRequest from(T request);

}
