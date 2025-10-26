package pro.api4.jsonapi4j.sampleapp.config.datasource.userdb;

import lombok.Data;

// fake DB entity
@Data
public class UserDbEntity {

    private final String id;
    private final String fullName;
    private final String email;
    private final String creditCardNumber;

}
