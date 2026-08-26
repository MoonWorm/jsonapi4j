package pro.api4.jsonapi4j.sampleapp.operations;

import pro.api4.jsonapi4j.sampleapp.config.datasource.model.country.CountryRef;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.AddressRow;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.UserDbEntity;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.RelativeRef;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface UserDb {

    void reset();

    UserDbEntity readById(String id);

    List<UserDbEntity> readByIds(List<String> ids);

    UserDbEntity createUser(String firstName,
                            String lastName,
                            String email,
                            String creditCardNumber,
                            List<AddressRow> addresses);

    /**
     * Applies a partial update, changing only the fields the change set names.
     *
     * <p>A key that is absent leaves its field alone; a key mapped to {@code null} clears it. Nullable
     * parameters cannot express that difference, which is why the change set is a map:
     * {@code null} as a value and no entry at all are different instructions.
     *
     * @param userId    the user to update
     * @param changes   field names of {@link UserDbEntity} mapped to their new values
     * @return the updated user
     */
    UserDbEntity updateUser(String userId, Map<String, Object> changes);

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
