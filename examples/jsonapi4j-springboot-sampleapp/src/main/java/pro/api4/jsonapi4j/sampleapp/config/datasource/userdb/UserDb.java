package pro.api4.jsonapi4j.sampleapp.config.datasource.userdb;

import org.springframework.stereotype.Component;
import pro.api4.jsonapi4j.processor.util.CustomCollectors;
import pro.api4.jsonapi4j.request.pagination.LimitOffsetToCursorAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

@Component
public class UserDb {

    private static AtomicInteger ID_COUNTER;

    private Map<String, UserDbEntity> users = new ConcurrentHashMap<>();
    private Map<String, List<String>> userCitizenships = new ConcurrentHashMap<>();
    private Map<String, String> userPlaceOfBirth = new ConcurrentHashMap<>();
    private Map<String, List<String>> userRalatives = new ConcurrentHashMap<>();

    {
        users.put("1", new UserDbEntity("1", "John Doe", "john@doe.com", "123456789"));
        userCitizenships.put("1", List.of("NO", "FI", "US"));
        userPlaceOfBirth.put("1", "US");
        userRalatives.put("1", List.of("2", "3"));

        users.put("2", new UserDbEntity("2", "Jane Doe", "jane@doe.com", "222456789"));
        userCitizenships.put("2", List.of("US"));
        userPlaceOfBirth.put("2", "FI");
        userRalatives.put("2", List.of("1", "4"));

        users.put("3", new UserDbEntity("3", "Jack Doe", "jack@doe.com", "333456789"));
        userCitizenships.put("3", List.of("US", "FI"));
        userPlaceOfBirth.put("3", "NO");
        userRalatives.put("3", Collections.emptyList());

        users.put("4", new UserDbEntity("4", "Jessy Doe", "jessy@doe.com", "444456789"));
        userCitizenships.put("4", List.of("NO", "US"));
        userPlaceOfBirth.put("4", "US");
        userRalatives.put("4", List.of("1"));

        users.put("5", new UserDbEntity("5", "Jared Doe", "jared@doe.com", "555456789"));
        userCitizenships.put("5", List.of("US"));
        userPlaceOfBirth.put("5", "NO");
        userRalatives.put("5", List.of("1", "2", "3", "4"));

        ID_COUNTER = new AtomicInteger(6);
    }

    public UserDbEntity readById(String id) {
        return users.get(id);
    }

    public List<UserDbEntity> readByIds(List<String> ids) {
        return ids.stream().map(this::readById).toList();
    }


    public UserDbEntity createUser(String firstName,
                                   String lastName,
                                   String email,
                                   String creditCardNumber) {
        UserDbEntity newUser = new UserDbEntity(
                String.valueOf(ID_COUNTER.getAndIncrement()),
                firstName + " " + lastName,
                email,
                creditCardNumber
        );
        users.put(newUser.getId(), newUser);
        return newUser;
    }

    public UserDbEntity createUser(String firstName,
                                   String lastName,
                                   String email,
                                   String creditCardNumber,
                                   List<String> cca2s) {
        UserDbEntity result = createUser(firstName, lastName, email, creditCardNumber);
        userCitizenships.put(result.getId(), cca2s);
        return result;
    }

    public List<String> getUserCitizenships(String userId) {
        return userCitizenships.get(userId);
    }

    public List<String> getUserRelatives(String userId) {
        return userRalatives.get(userId);
    }

    public void updateUserCitizenships(String userId, List<String> cca2s) {
        userCitizenships.remove(userId);
        userCitizenships.put(userId, cca2s);
    }

    public Map<String, List<String>> getUsersCitizenships(Set<String> userIds) {
        return userIds.stream().collect(
                Collectors.toMap(
                        userId -> userId,
                        userId -> emptyIfNull(userCitizenships.get(userId))
                )
        );
    }

    public Map<String, List<String>> getUsersRelatives(Set<String> userIds) {
        return userIds.stream().collect(
                Collectors.toMap(
                        userId -> userId,
                        userId -> emptyIfNull(userRalatives.get(userId))
                )
        );
    }

    public String getUserPlaceOfBirth(String userId) {
        return userPlaceOfBirth.get(userId);
    }

    public Map<String, String> getUsersPlaceOfBirth(Set<String> userIds) {
        return userIds.stream().collect(
                CustomCollectors.toMapThatSupportsNullValues(
                        userId -> userId,
                        userId -> userPlaceOfBirth.get(userId)
                )
        );
    }

    public DbPage<UserDbEntity> readAllUsers(String cursor) {
        LimitOffsetToCursorAdapter adapter = new LimitOffsetToCursorAdapter(cursor).withDefaultLimit(2);
        LimitOffsetToCursorAdapter.LimitAndOffset limitAndOffset = adapter.decodeLimitAndOffset();

        int effectiveFrom = limitAndOffset.getOffset() < users.size() ? limitAndOffset.getOffset() : users.size() - 1;
        int effectiveTo = Math.min(effectiveFrom + limitAndOffset.getLimit(), users.size());

        List<UserDbEntity> result = new ArrayList<>(users.values()).subList(effectiveFrom, effectiveTo);
        String nextCursor = adapter.nextCursor(users.size());
        return new DbPage<>(nextCursor, result);
    }

    public static class DbPage<E> {

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
