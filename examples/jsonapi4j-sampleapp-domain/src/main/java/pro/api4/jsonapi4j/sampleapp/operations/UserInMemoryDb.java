package pro.api4.jsonapi4j.sampleapp.operations;

import org.apache.commons.collections4.ListUtils;
import pro.api4.jsonapi4j.processor.util.CustomCollectors;
import pro.api4.jsonapi4j.request.pagination.LimitOffsetToCursorAdapter;
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

    private Map<String, UserDbEntity> users = new ConcurrentHashMap<>();
    private Map<String, List<String>> userCitizenships = new ConcurrentHashMap<>();
    private Map<String, String> userPlaceOfBirth = new ConcurrentHashMap<>();
    private Map<String, List<UserRelationshipInfo>> userRalatives = new ConcurrentHashMap<>();

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
    public List<String> getUserCitizenships(String userId) {
        return userCitizenships.get(userId);
    }

    @Override
    public List<UserRelationshipInfo> getUserRelatives(String userId) {
        return userRalatives.get(userId);
    }

    @Override
    public void updateUserCitizenships(String userId, List<String> cca2s) {
        userCitizenships.remove(userId);
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
    public String getUserPlaceOfBirth(String userId) {
        return userPlaceOfBirth.get(userId);
    }

    @Override
    public Map<String, String> getUsersPlaceOfBirth(Set<String> userIds) {
        return userIds.stream().collect(
                CustomCollectors.toMapThatSupportsNullValues(
                        userId -> userId,
                        userId -> userPlaceOfBirth.get(userId)
                )
        );
    }

    @Override
    public DbPage<UserDbEntity> readAllUsers(String cursor) {
        LimitOffsetToCursorAdapter adapter = new LimitOffsetToCursorAdapter(cursor).withDefaultLimit(2);
        LimitOffsetToCursorAdapter.LimitAndOffset limitAndOffset = adapter.decodeLimitAndOffset();

        int effectiveFrom = limitAndOffset.getOffset() < users.size() ? limitAndOffset.getOffset() : users.size() - 1;
        int effectiveTo = Math.min(effectiveFrom + limitAndOffset.getLimit(), users.size());

        List<UserDbEntity> result = new ArrayList<>(users.values()).subList(effectiveFrom, effectiveTo);
        String nextCursor = adapter.nextCursor(users.size());
        return new DbPage<>(nextCursor, result);
    }

}
