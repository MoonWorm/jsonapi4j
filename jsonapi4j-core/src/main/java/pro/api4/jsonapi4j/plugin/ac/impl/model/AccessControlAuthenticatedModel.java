package pro.api4.jsonapi4j.plugin.ac.impl.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import pro.api4.jsonapi4j.plugin.ac.impl.annotation.Authenticated;

@EqualsAndHashCode
@ToString
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(access = AccessLevel.PACKAGE)
public class AccessControlAuthenticatedModel {

    private Authenticated authenticated;

    static AccessControlAuthenticatedModel fromValue(Authenticated authenticated) {
        if (authenticated == null || authenticated == Authenticated.NOT_SET) {
            return null;
        }
        return AccessControlAuthenticatedModel.builder()
                .authenticated(authenticated)
                .build();
    }

}
