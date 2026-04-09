package pro.api4.jsonapi4j.operation;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
@EqualsAndHashCode
public class RegisteredOperation<T extends ResourceOperation> {

    private final T operation;
    private final OperationMeta operationMeta;

}
