package pro.api4.jsonapi4j.sampleapp.operations;

import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.UserDbEntity;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.UserRelationshipInfo;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.UserRelationshipInfo.RelationshipType;

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

    UserDbEntity updateUser(String userId,
                            String firstName,
                            String lastName,
                            String email,
                            String creditCardNumber);

    void deleteUser(String userId);

    List<String> getUserCitizenships(String userId);

    Map<String, List<String>> getUsersCitizenships(Set<String> userIds);

    void updateUserCitizenships(String userId, List<String> cca2s);

    List<UserRelationshipInfo> getUserRelatives(String userId);

    Map<String, List<UserRelationshipInfo>> getUsersRelatives(Set<String> userIds);

    void updateUserRelatives(String userId, Map<String, RelationshipType> relations);

    String getUserPlaceOfBirth(String userId);

    Map<String, String> getUsersPlaceOfBirth(Set<String> userIds);

    void updateUserPlaceOfBirth(String userId, String cca2);

    DbPage<UserDbEntity> readAllUsers(String cursor);

    DbPage<UserDbEntity> readAllUsers(long limit, long offset);

    class DbPage<E> {

        private final List<E> entities;
        private String cursor;
        private long totalItems;

        public DbPage(List<E> entities, String cursor) {
            this.entities = entities;
            this.cursor = cursor;
        }

        public DbPage(List<E> entities, long totalItems) {
            this.entities = entities;
            this.totalItems = totalItems;
        }

        public String getCursor() {
            return cursor;
        }

        public List<E> getEntities() {
            return entities;
        }

        public long getTotalItems() {
            return totalItems;
        }
    }
}
