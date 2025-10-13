package io.jsonapi4j.processor.util;

import io.jsonapi4j.processor.exception.MappingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static java.util.Collections.singletonList;
import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;

public final class MappingUtil {

    private final static Logger log = LoggerFactory.getLogger(MappingUtil.class);

    private MappingUtil() {

    }

    public static <FROM, TO> LinkedHashMap<FROM, TO> mapCollection(List<FROM> from,
                                                                   Function<FROM, TO> mapper) {
        try {
            LinkedHashMap<FROM, TO> resultCollection = new LinkedHashMap<>();
            for (FROM fromItem : emptyIfNull(from)) {
                if (fromItem != null) {
                    TO resultItem = mapper.apply(fromItem);
                    if (resultItem != null) {
                        resultCollection.put(fromItem, resultItem);
                    }
                }
            }
            return resultCollection;
        } catch (MappingException e) {
            throw e;
        } catch (RuntimeException e) {
            String errMsg = "Error mapping source objects into collection";
            log.error("{}, Error message: {}", errMsg, e.getMessage());
            throw new MappingException(errMsg, e);
        }
    }

    public static <FROM, TO> TO mapSingleLenient(FROM fromItem,
                                                 Function<FROM, TO> mapper) {
        if (fromItem == null) {
            return null;
        }
        return mapCollection(singletonList(fromItem), mapper)
                .values()
                .stream()
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    public static <FROM, TO> TO mapSingleStrict(FROM dataSourceDto,
                                                Function<FROM, TO> mapper) {
        if (dataSourceDto == null) {
            return null;
        }
        return mapCollection(singletonList(dataSourceDto), mapper)
                .values()
                .stream()
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new MappingException("Mapped result object is null."));
    }

}
