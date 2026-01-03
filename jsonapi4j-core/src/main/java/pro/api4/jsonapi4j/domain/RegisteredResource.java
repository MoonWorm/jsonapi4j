package pro.api4.jsonapi4j.domain;

import lombok.*;

import java.util.Map;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
@EqualsAndHashCode
public class RegisteredResource<T extends Resource<?>> implements Comparable<RegisteredResource<T>> {

    private T resource;
    private ResourceType resourceType;
    private Class<?> registeredAs;
    private Map<String, Object> pluginInfo;

    @Override
    public int compareTo(RegisteredResource<T> o) {
        return this.resourceType.compareTo(o.getResourceType());
    }

}
