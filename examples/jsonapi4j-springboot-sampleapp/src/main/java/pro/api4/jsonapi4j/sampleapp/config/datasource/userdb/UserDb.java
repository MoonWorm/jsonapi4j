package pro.api4.jsonapi4j.sampleapp.config.datasource.userdb;

import pro.api4.jsonapi4j.request.pagination.LimitOffsetToCursorAdapter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

@Component
public class UserDb {

    private static final AtomicInteger COUNTER = new AtomicInteger(1);

    private Map<String, UserDbEntity> users = new ConcurrentHashMap<>();
    private Map<String, List<String>> userIdToCountryCca2 = new ConcurrentHashMap<>();
    {
        String id = String.valueOf(COUNTER.getAndIncrement());
        users.put(id, new UserDbEntity(id, "John Doe", "john@doe.com", "123456789"));
        userIdToCountryCca2.put(id, List.of("NO", "FI", "US"));

        id = String.valueOf(COUNTER.getAndIncrement());
        users.put(id, new UserDbEntity(id, "Jane Doe", "jane@doe.com", "222456789"));
        userIdToCountryCca2.put("2", List.of("US"));

        id = String.valueOf(COUNTER.getAndIncrement());
        users.put(id, new UserDbEntity(id, "Jack Doe", "jack@doe.com", "333456789"));
        userIdToCountryCca2.put(id, List.of("US", "FI"));

        id = String.valueOf(COUNTER.getAndIncrement());
        users.put(id, new UserDbEntity(id, "Jessy Doe", "jessy@doe.com", "444456789"));
        userIdToCountryCca2.put(id, List.of("NO", "US"));

        id = String.valueOf(COUNTER.getAndIncrement());
        users.put(id, new UserDbEntity(id, "Jared Doe", "jared@doe.com", "555456789"));
        userIdToCountryCca2.put(id, List.of("US"));
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
                String.valueOf(COUNTER.getAndIncrement()),
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
        userIdToCountryCca2.put(result.getId(), cca2s);
        return result;
    }

    public List<String> getUserCitizenships(String userId) {
        return userIdToCountryCca2.get(userId);
    }

    public Map<String, List<String>> getUsersCitizenships(Set<String> userIds) {
        return userIds.stream().collect(
                Collectors.toMap(
                        userId -> userId,
                        userId -> emptyIfNull(userIdToCountryCca2.get(userId))
                )
        );
    }

    public void updateUserCitizenships(String userId, List<String> cca2s) {
        userIdToCountryCca2.remove(userId);
        userIdToCountryCca2.put(userId, cca2s);
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
