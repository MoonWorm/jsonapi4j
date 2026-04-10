package pro.api4.jsonapi4j.sampleapp.config.datasource.model.user;

import lombok.Data;
import lombok.With;

// fake DB entity
@Data
@With
public class UserDbEntity {

    private final String id;
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String creditCardNumber;

}
