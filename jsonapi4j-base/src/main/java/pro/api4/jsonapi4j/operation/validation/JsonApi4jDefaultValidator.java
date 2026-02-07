package pro.api4.jsonapi4j.operation.validation;

import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.request.FiltersAwareRequest;

import java.util.List;

import static pro.api4.jsonapi4j.operation.ReadMultipleResourcesOperation.ID_FILTER_NAME;

public class JsonApi4jDefaultValidator {

    public static final int MAX_ELEMENTS_IN_FILTER_PARAM = 20;
    public static final int RESOURCE_ID_MAX_LENGTH = 64;

    public void validateNonNull(Object object, String parameter) {
        if (object == null) {
            throw new JsonApi4jConstraintViolationException("value can't be null", parameter);
        }
    }

    public void validateFilterByIds(List<String> resourceIds) {
        if (resourceIds != null) {
            if (resourceIds.size() > MAX_ELEMENTS_IN_FILTER_PARAM) {
                throw new JsonApi4jConstraintViolationException(
                        "max elements number exceeded: " + MAX_ELEMENTS_IN_FILTER_PARAM,
                        FiltersAwareRequest.getFilterParam(ID_FILTER_NAME)
                );
            }
            for (String resourceId : resourceIds) {
                validateResourceId(resourceId);
            }
        }
    }

    public void validateResourceId(String resourceId) {
        if (StringUtils.isBlank(resourceId)) {
            throw new JsonApi4jConstraintViolationException(
                    "value can't be blank",
                    FiltersAwareRequest.getFilterParam(ID_FILTER_NAME) + " -> resourceId"
            );
        }
        if (resourceId.length() > RESOURCE_ID_MAX_LENGTH) {
            throw new JsonApi4jConstraintViolationException(
                    "value length can't be more than " + RESOURCE_ID_MAX_LENGTH,
                    FiltersAwareRequest.getFilterParam(ID_FILTER_NAME) + " -> resourceId"
            );
        }
    }

}
