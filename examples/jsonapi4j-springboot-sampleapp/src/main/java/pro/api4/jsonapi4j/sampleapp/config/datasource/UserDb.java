package pro.api4.jsonapi4j.sampleapp.config.datasource;

import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.UserDbEntity;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.UserRelationshipInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface UserDb {

    UserDbEntity readById(String id);

    List<UserDbEntity> readByIds(List<String> ids);

    UserDbEntity createUser(String firstName,
                            String lastName,
                            String email,
                            String creditCardNumber);

    List<String> getUserCitizenships(String userId);

    void updateUserCitizenships(String userId, List<String> cca2s);

    List<UserRelationshipInfo> getUserRelatives(String userId);

    Map<String, List<String>> getUsersCitizenships(Set<String> userIds);

    Map<String, List<UserRelationshipInfo>> getUsersRelatives(Set<String> userIds);

    String getUserPlaceOfBirth(String userId);

    Map<String, String> getUsersPlaceOfBirth(Set<String> userIds);

    DbPage<UserDbEntity> readAllUsers(String cursor);

    class DbPage<E> {

        private final String cursor;
        private final List<E> entities;

        public DbPage(String cursor, List<E> entities) {
            this.cursor = cursor;
            this.entities = entities;
        }

        public String getCursor() {
            return cursor;
        }

        public List<E> getEntities() {
            return entities;
        }
    }
}
