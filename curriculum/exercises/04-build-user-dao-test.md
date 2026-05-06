# Exercise 04 — Write Unit Tests for UserService

**Covers:** Modules 04, 06
**Difficulty:** Intermediate
**Estimated time:** 60–90 minutes

---

## What you are doing

The old `UserServiceTest.java` tested `UserService` with a mocked `EventUserRepository`.
That repository no longer exists. The test file is now out of date. You will rewrite the
unit tests for `UserService` using a mocked `UserDao`.

This is a unit test — you are not touching DynamoDB. You mock `UserDao` and verify that
`UserService` calls it correctly and builds the right responses.

---

## Before you start

Open `Application/src/test/java/com/kenzie/appserver/service/UserServiceTest.java`.
Read through the existing tests. Note:
- What they mock
- What they verify
- Which tests are still valid in concept (even if the mocked class changed)
- Which tests are testing behavior that no longer exists

---

## What to test

Write tests that cover:

| Scenario | Method | Expected behavior |
|---|---|---|
| User found | `getUserById` | Returns a `UserResponse` with correct fields |
| User not found | `getUserById` | Returns `null` |
| Valid create | `createUser` | Saves a `UserRecord`; returns `UserResponse` with a generated UUID |
| Null name | `createUser` | Throws `ResponseStatusException` with status 400 |
| Null email | `createUser` | Throws `ResponseStatusException` with status 400 |
| User found for update | `updateUser` | Saves updated record; returns updated `UserResponse` |
| User not found for update | `updateUser` | Throws `ResponseStatusException` with status 404 |
| Empty ID for delete | `deleteUser` | Throws `ResponseStatusException` with status 400 |
| User not found for delete | `deleteUser` | Throws `ResponseStatusException` with status 404 |
| User found for delete | `deleteUser` | Calls `userDao.deleteById` |

---

## Structure

```java
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserDao userDao;

    @InjectMocks
    private UserService userService;

    @Test
    void getUserById_userExists_returnsResponse() {
        // Arrange
        UserRecord record = new UserRecord("user-1", "Stacey", "stacey@example.com");
        when(userDao.findById("user-1")).thenReturn(Optional.of(record));

        // Act
        UserResponse response = userService.getUserById("user-1");

        // Assert
        assertNotNull(response);
        assertEquals("user-1", response.getId());
        assertEquals("Stacey", response.getName());
    }

    // Write the rest
}
```

---

## Run your tests

```bash
JAVA_HOME=/opt/homebrew/Cellar/openjdk@21/21.0.8/libexec/openjdk.jdk/Contents/Home \
  ./gradlew :Application:test
```

All tests should pass. Commit:

```
test: rewrite UserServiceTest for UserDao; add coverage for all CRUD paths
```

---

## What you should be able to explain after this

- Why do we mock `UserDao` rather than use a real one in unit tests?
- `@InjectMocks` creates a real `UserService` with mocked dependencies injected. What is
  the alternative — constructing `UserService` manually in a `@BeforeEach`?
- The `createUser` method calls `UUID.randomUUID()`. How would you verify that the ID set
  on the saved `UserRecord` is a valid UUID?
