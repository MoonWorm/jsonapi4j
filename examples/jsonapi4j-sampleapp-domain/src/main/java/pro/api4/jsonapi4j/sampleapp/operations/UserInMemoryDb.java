package pro.api4.jsonapi4j.sampleapp.operations;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.util.CustomCollectors;
import pro.api4.jsonapi4j.response.pagination.LimitOffsetToCursorAdapter;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.country.CountryRef;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.UserDbEntity;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.RelativeRef;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.RelativeRef.RelationshipType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

public class UserInMemoryDb implements UserDb {

    private static AtomicInteger ID_COUNTER;

    private final Map<String, UserDbEntity> users = new ConcurrentHashMap<>();
    private final Map<String, List<CountryRef>> userCitizenships = new ConcurrentHashMap<>();
    private final Map<String, CountryRef> userPlaceOfBirth = new ConcurrentHashMap<>();
    private final Map<String, List<RelativeRef>> userRalatives = new ConcurrentHashMap<>();

    {
        users.put("1", new UserDbEntity("1", "John", "Doe", "john@doe.com", "123456789"));
        userCitizenships.put("1", List.of(new CountryRef("NO"), new CountryRef("FI"), new CountryRef("US")));
        userPlaceOfBirth.put("1", new CountryRef("US"));
        userRalatives.put(
                "1",
                List.of(
                        new RelativeRef("2", RelationshipType.HUSBAND),
                        new RelativeRef("3", RelationshipType.BROTHER)
                )
        );

        users.put("2", new UserDbEntity("2", "Jane", "Doe", "jane@doe.com", "222456789"));
        userCitizenships.put("2", List.of(new CountryRef("US")));
        userPlaceOfBirth.put("2", new CountryRef("FI"));
        userRalatives.put(
                "2",
                List.of(
                        new RelativeRef("1", RelationshipType.WIFE),
                        new RelativeRef("4", RelationshipType.SON)
                )
        );

        users.put("3", new UserDbEntity("3", "Jack", "Doe", "jack@doe.com", "333456789"));
        userCitizenships.put("3", List.of(new CountryRef("US"), new CountryRef("FI")));
        userPlaceOfBirth.put("3", new CountryRef("NO"));
        userRalatives.put("3", Collections.emptyList());

        users.put("4", new UserDbEntity("4", "Jessy", "Doe", "jessy@doe.com", "444456789"));
        userCitizenships.put("4", List.of(new CountryRef("NO"), new CountryRef("US")));
        userPlaceOfBirth.put("4", new CountryRef("US"));
        userRalatives.put(
                "4",
                List.of(
                        new RelativeRef("1", RelationshipType.FATHER),
                        new RelativeRef("2", RelationshipType.MOTHER)
                )
        );

        users.put("5", new UserDbEntity("5", "Jared", "Doe", "jared@doe.com", "555456789"));
        userCitizenships.put("5", List.of(new CountryRef("US")));
        userPlaceOfBirth.put("5", new CountryRef("NO"));
        userRalatives.put(
                "5",
                List.of(
                        new RelativeRef("1", RelationshipType.BROTHER),
                        new RelativeRef("2", RelationshipType.DAUGHTER),
                        new RelativeRef("3", RelationshipType.FATHER),
                        new RelativeRef("4", RelationshipType.BROTHER)
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
    public List<CountryRef> getUserCitizenships(String userId) {
        return userCitizenships.get(userId);
    }

    @Override
    public List<RelativeRef> getUserRelatives(String userId) {
        return userRalatives.get(userId);
    }

    @Override
    public void updateUserCitizenships(String userId, List<CountryRef> citizenships) {
        userCitizenships.put(userId, citizenships);
    }

    @Override
    public void addUserCitizenships(String userId, List<CountryRef> citizenships) {
        List<CountryRef> existing = new ArrayList<>(emptyIfNull(userCitizenships.get(userId)));
        citizenships.stream()
                .filter(citizenship -> !existing.contains(citizenship))
                .forEach(existing::add);
        userCitizenships.put(userId, existing);
    }

    @Override
    public void removeUserCitizenships(String userId, List<CountryRef> citizenships) {
        List<CountryRef> existing = new ArrayList<>(emptyIfNull(userCitizenships.get(userId)));
        existing.removeAll(citizenships);
        userCitizenships.put(userId, existing);
    }

    @Override
    public Map<String, List<CountryRef>> getUsersCitizenships(Set<String> userIds) {
        return userIds.stream().collect(
                Collectors.toMap(
                        userId -> userId,
                        userId -> emptyIfNull(userCitizenships.get(userId))
                )
        );
    }

    @Override
    public Map<String, List<RelativeRef>> getUsersRelatives(Set<String> userIds) {
        return userIds.stream().collect(
                Collectors.toMap(
                        userId -> userId,
                        userId -> ListUtils.emptyIfNull(userRalatives.get(userId))
                )
        );
    }

    @Override
    public void updateUserRelatives(String userId, List<RelativeRef> relatives) {
        List<RelativeRef> relations = ListUtils.emptyIfNull(relatives)
                .stream()
                .filter(r -> StringUtils.isNotBlank(r.getRelativeUserId()))
                .filter(r -> r.getRelationshipType() != null)
                .toList();
        userRalatives.put(userId, relations);
    }

    @Override
    public void addUserRelatives(String userId, List<RelativeRef> relatives) {
        List<RelativeRef> existing = new ArrayList<>(ListUtils.emptyIfNull(userRalatives.get(userId)));
        Set<String> existingIds = existing.stream().map(RelativeRef::getRelativeUserId).collect(Collectors.toSet());
        ListUtils.emptyIfNull(relatives).stream()
                .filter(r -> StringUtils.isNotBlank(r.getRelativeUserId()) && r.getRelationshipType() != null)
                .filter(r -> !existingIds.contains(r.getRelativeUserId()))
                .forEach(existing::add);
        userRalatives.put(userId, existing);
    }

    @Override
    public void removeUserRelatives(String userId, Set<String> relativeIds) {
        List<RelativeRef> existing = new ArrayList<>(ListUtils.emptyIfNull(userRalatives.get(userId)));
        existing.removeIf(info -> relativeIds.contains(info.getRelativeUserId()));
        userRalatives.put(userId, existing);
    }

    @Override
    public void updateUserPlaceOfBirth(String userId, CountryRef placeOfBirth) {
        userPlaceOfBirth.put(userId, placeOfBirth);
    }

    @Override
    public CountryRef getUserPlaceOfBirth(String userId) {
        return userPlaceOfBirth.get(userId);
    }

    @Override
    public Map<String, CountryRef> getUsersPlaceOfBirth(Set<String> userIds) {
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
