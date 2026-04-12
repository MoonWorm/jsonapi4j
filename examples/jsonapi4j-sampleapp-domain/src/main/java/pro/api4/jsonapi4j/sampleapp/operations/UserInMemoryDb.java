package pro.api4.jsonapi4j.sampleapp.operations;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.processor.exception.ResourceNotFoundException;
import pro.api4.jsonapi4j.util.CustomCollectors;
import pro.api4.jsonapi4j.response.pagination.LimitOffsetToCursorAdapter;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.UserDbEntity;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.UserRelationshipInfo;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.UserRelationshipInfo.RelationshipType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

public class UserInMemoryDb implements UserDb {

    private static AtomicInteger ID_COUNTER;

    private final Map<String, UserDbEntity> users = new ConcurrentHashMap<>();
    private final Map<String, List<String>> userCitizenships = new ConcurrentHashMap<>();
    private final Map<String, String> userPlaceOfBirth = new ConcurrentHashMap<>();
    private final Map<String, List<UserRelationshipInfo>> userRalatives = new ConcurrentHashMap<>();

    {
        users.put("1", new UserDbEntity("1", "John", "Doe", "john@doe.com", "123456789"));
        userCitizenships.put("1", List.of("NO", "FI", "US"));
        userPlaceOfBirth.put("1", "US");
        userRalatives.put(
                "1",
                List.of(
                        new UserRelationshipInfo("2", RelationshipType.HUSBAND),
                        new UserRelationshipInfo("3", RelationshipType.BROTHER)
                )
        );

        users.put("2", new UserDbEntity("2", "Jane", "Doe", "jane@doe.com", "222456789"));
        userCitizenships.put("2", List.of("US"));
        userPlaceOfBirth.put("2", "FI");
        userRalatives.put(
                "2",
                List.of(
                        new UserRelationshipInfo("1", RelationshipType.WIFE),
                        new UserRelationshipInfo("4", RelationshipType.SON)
                )
        );

        users.put("3", new UserDbEntity("3", "Jack", "Doe", "jack@doe.com", "333456789"));
        userCitizenships.put("3", List.of("US", "FI"));
        userPlaceOfBirth.put("3", "NO");
        userRalatives.put("3", Collections.emptyList());

        users.put("4", new UserDbEntity("4", "Jessy", "Doe", "jessy@doe.com", "444456789"));
        userCitizenships.put("4", List.of("NO", "US"));
        userPlaceOfBirth.put("4", "US");
        userRalatives.put(
                "4",
                List.of(
                        new UserRelationshipInfo("1", RelationshipType.FATHER),
                        new UserRelationshipInfo("2", RelationshipType.MOTHER)
                )
        );

        users.put("5", new UserDbEntity("5", "Jared", "Doe", "jared@doe.com", "555456789"));
        userCitizenships.put("5", List.of("US"));
        userPlaceOfBirth.put("5", "NO");
        userRalatives.put(
                "5",
                List.of(
                        new UserRelationshipInfo("1", RelationshipType.BROTHER),
                        new UserRelationshipInfo("2", RelationshipType.DAUGHTER),
                        new UserRelationshipInfo("3", RelationshipType.FATHER),
                        new UserRelationshipInfo("4", RelationshipType.BROTHER)
                )
        );

        ID_COUNTER = new AtomicInteger(6);
    }

    @Override
    public UserDbEntity readById(String id) {
        return users.get(id);
    }

    @Override
    public List<UserDbEntity> readByIds(List<String> ids) {
        return ids.stream().map(this::readById).toList();
    }

    @Override
    public UserDbEntity createUser(String firstName,
                                   String lastName,
                                   String email,
                                   String creditCardNumber) {
        Validate.notBlank(firstName, "firstName is required");
        Validate.notBlank(lastName, "lastName is required");
        Validate.notBlank(email, "email is required");
        UserDbEntity newUser = new UserDbEntity(
                String.valueOf(ID_COUNTER.getAndIncrement()),
                firstName,
                lastName,
                email,
                creditCardNumber
        );
        users.put(newUser.getId(), newUser);
        return newUser;
    }

    @Override
    public UserDbEntity updateUser(String userId,
                                   String firstName,
                                   String lastName,
                                   String email,
                                   String creditCardNumber) {
        if (!users.containsKey(userId)) {
            throw new RuntimeException("User with id " + userId + "doesn't exist");
        }
        UserDbEntity updatedUser = users.get(userId);
        if (StringUtils.isNotBlank(firstName)) {
            updatedUser = updatedUser.withFirstName(firstName);
        }
        if (StringUtils.isNotBlank(lastName)) {
            updatedUser = updatedUser.withLastName(lastName);
        }
        if (StringUtils.isNotBlank(email)) {
            updatedUser = updatedUser.withEmail(email);
        }
        if (StringUtils.isNotBlank(creditCardNumber)) {
            updatedUser = updatedUser.withCreditCardNumber(creditCardNumber);
        }
        users.put(updatedUser.getId(), updatedUser);
        return updatedUser;
    }

    @Override
    public void deleteUser(String userId) {
        users.remove(userId);
        userCitizenships.remove(userId);
        userPlaceOfBirth.remove(userId);
        userRalatives.remove(userId);
    }

    @Override
    public List<String> getUserCitizenships(String userId) {
        return userCitizenships.get(userId);
    }

    @Override
    public List<UserRelationshipInfo> getUserRelatives(String userId) {
        return userRalatives.get(userId);
    }

    @Override
    public void updateUserCitizenships(String userId, List<String> cca2s) {
        userCitizenships.put(userId, cca2s);
    }

    @Override
    public Map<String, List<String>> getUsersCitizenships(Set<String> userIds) {
        return userIds.stream().collect(
                Collectors.toMap(
                        userId -> userId,
                        userId -> emptyIfNull(userCitizenships.get(userId))
                )
        );
    }

    @Override
    public Map<String, List<UserRelationshipInfo>> getUsersRelatives(Set<String> userIds) {
        return userIds.stream().collect(
                Collectors.toMap(
                        userId -> userId,
                        userId -> ListUtils.emptyIfNull(userRalatives.get(userId))
                )
        );
    }

    @Override
    public void updateUserRelatives(String userId, Map<String, RelationshipType> relationsMap) {
        List<UserRelationshipInfo> relations = relationsMap.entrySet()
                .stream()
                .filter(e -> StringUtils.isNotBlank(e.getKey()))
                .filter(e -> e.getValue() != null)
                .map(e -> new UserRelationshipInfo(e.getKey(), e.getValue()))
                .toList();
        userRalatives.put(userId, relations);
    }

    @Override
    public void updateUserPlaceOfBirth(String userId, String cca2) {
        userPlaceOfBirth.put(userId, cca2);
    }

    @Override
    public String getUserPlaceOfBirth(String userId) {
        return userPlaceOfBirth.get(userId);
    }

    @Override
    public Map<String, String> getUsersPlaceOfBirth(Set<String> userIds) {
        return userIds.stream().collect(
                CustomCollectors.toMapThatSupportsNullValues(
                        userId -> userId,
                        userPlaceOfBirth::get
                )
        );
    }

    @Override
    public DbPage<UserDbEntity> readAllUsers(String cursor) {
        LimitOffsetToCursorAdapter adapter = new LimitOffsetToCursorAdapter(cursor).withDefaultLimit(2);
        LimitOffsetToCursorAdapter.LimitAndOffset limitAndOffset = adapter.decodeLimitAndOffset();

        long effectiveFrom = limitAndOffset.getOffset() < users.size() ? limitAndOffset.getOffset() : users.size() - 1;
        long effectiveTo = Math.min(effectiveFrom + limitAndOffset.getLimit(), users.size());

        List<UserDbEntity> result = new ArrayList<>(users.values()).subList((int) effectiveFrom, (int) effectiveTo);
        String nextCursor = adapter.nextCursor(users.size());
        return new DbPage<>(result, nextCursor);
    }

    @Override
    public DbPage<UserDbEntity> readAllUsers(long limit, long offset) {
        long effectiveFrom = offset < users.size() ? offset : users.size() - 1;
        long effectiveTo = Math.min(effectiveFrom + limit, users.size());

        List<UserDbEntity> result = new ArrayList<>(users.values()).subList((int) effectiveFrom, (int) effectiveTo);
        return new DbPage<>(result, users.size());
    }
}
