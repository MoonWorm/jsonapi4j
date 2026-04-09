package pro.api4.jsonapi4j.operation;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;

import java.util.Map;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
@EqualsAndHashCode
public class OperationMeta {
    private final Class<?> registeredAs;
    private final ResourceType resourceType;
    private final RelationshipName relationshipName;
    private final OperationType operationType;
    private final Map<String, Object> pluginInfo;
}
