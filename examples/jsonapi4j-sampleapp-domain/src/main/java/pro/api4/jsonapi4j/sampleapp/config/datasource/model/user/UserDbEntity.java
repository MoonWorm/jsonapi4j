package pro.api4.jsonapi4j.sampleapp.config.datasource.model.user;

import lombok.Data;

// fake DB entity
@Data
public class UserDbEntity {

    private final String id;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String creditCardNumber;

}
