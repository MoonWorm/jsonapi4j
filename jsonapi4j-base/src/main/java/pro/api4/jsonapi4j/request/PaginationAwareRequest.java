package pro.api4.jsonapi4j.request;


public interface PaginationAwareRequest {

    static boolean isJsonApiPaginationParam(String paramName) {
        if (paramName == null) {
            return false;
        }
        String regex = "^page\\[.+]$";
        return paramName.matches(regex);
    }

}
