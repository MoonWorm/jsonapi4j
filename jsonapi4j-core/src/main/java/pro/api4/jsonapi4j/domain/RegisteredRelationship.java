package pro.api4.jsonapi4j.domain;

import lombok.*;

import java.util.Map;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
@EqualsAndHashCode
public class RegisteredRelationship<T extends Relationship<?, ?>> {

    private T relationship;
    private Map<String, Object> pluginInfo;

}
