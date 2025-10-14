package pro.api4.jsonapi4j.processor;

public interface IdSupplier<T> {

    String getId(T t);

}
