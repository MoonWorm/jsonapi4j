package pro.api4.jsonapi4j.operation;

import lombok.*;

import java.util.Map;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
@EqualsAndHashCode
public class RegisteredOperation<T extends Operation> {

    private final T operation;
    private final Map<String, Object> pluginInfo;

}
