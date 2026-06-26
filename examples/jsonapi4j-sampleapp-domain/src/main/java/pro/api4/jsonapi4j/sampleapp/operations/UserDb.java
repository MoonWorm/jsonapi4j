package pro.api4.jsonapi4j.sampleapp.operations;

import pro.api4.jsonapi4j.sampleapp.config.datasource.model.country.CountryRef;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.UserDbEntity;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.RelativeRef;

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

    List<CountryRef> getUserCitizenships(String userId);

    Map<String, List<CountryRef>> getUsersCitizenships(Set<String> userIds);

    void updateUserCitizenships(String userId, List<CountryRef> citizenships);

    void addUserCitizenships(String userId, List<CountryRef> citizenships);

    void removeUserCitizenships(String userId, List<CountryRef> citizenships);

    List<RelativeRef> getUserRelatives(String userId);

    Map<String, List<RelativeRef>> getUsersRelatives(Set<String> userIds);

    void updateUserRelatives(String userId, List<RelativeRef> relatives);

    void addUserRelatives(String userId, List<RelativeRef> relatives);

    void removeUserRelatives(String userId, Set<String> relativeIds);

    CountryRef getUserPlaceOfBirth(String userId);

    Map<String, CountryRef> getUsersPlaceOfBirth(Set<String> userIds);

    void updateUserPlaceOfBirth(String userId, CountryRef placeOfBirth);

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
