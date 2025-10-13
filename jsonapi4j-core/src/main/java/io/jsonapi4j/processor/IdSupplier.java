package io.jsonapi4j.processor;

public interface IdSupplier<T> {

    String getId(T t);

}
