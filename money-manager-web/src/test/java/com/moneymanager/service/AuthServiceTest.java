package com.moneymanager.service;

import com.moneymanager.model.User;
import com.moneymanager.repository.IUserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthServiceTest {

    private FakeUserRepo repo;
    private AuthService service;

    @BeforeEach
    void setUp() {
        repo = new FakeUserRepo();
        service = new AuthService(repo);
    }

    @Test
    void registrationHashesSecretsAndNormalizesUsernameForLogin() {
        User user = service.register("  maria  ", "pass1234", "pass1234", "rescue-key");

        assertNotEquals("pass1234", user.getPasswordHash());
        assertNotEquals("rescue-key", user.getRecoveryCodeHash());
        assertTrue(service.login(" maria ", "pass1234").isPresent());
    }

    @Test
    void resetPasswordRequiresRecoveryCodeAndReplacesPassword() {
        service.register("maria", "pass1234", "pass1234", "rescue-key");

        assertThrows(IllegalArgumentException.class,
                () -> service.resetPassword("maria", "wrong-key", "newpass1", "newpass1"));

        service.resetPassword("maria", "rescue-key", "newpass1", "newpass1");

        assertFalse(service.login("maria", "pass1234").isPresent());
        assertTrue(service.login("maria", "newpass1").isPresent());
    }

    @Test
    void registrationRejectsPasswordMismatchAndShortRecoveryCode() {
        assertThrows(IllegalArgumentException.class,
                () -> service.register("maria", "pass1234", "different", "rescue-key"));
        assertThrows(IllegalArgumentException.class,
                () -> service.register("maria", "pass1234", "pass1234", "tiny"));
        assertThrows(IllegalArgumentException.class,
                () -> service.register("maria", "short", "short", "rescue-key"));
    }

    @Test
    void registrationRejectsSecretsBeyondBcryptInputLimit() {
        String overlyLongSecret = "x".repeat(73);

        assertThrows(IllegalArgumentException.class,
                () -> service.register("maria", overlyLongSecret, overlyLongSecret, "rescue-key"));
        assertThrows(IllegalArgumentException.class,
                () -> service.register("maria", "pass1234", "pass1234", overlyLongSecret));
    }

    private static final class FakeUserRepo implements IUserRepo {
        private final Map<String, User> users = new HashMap<>();
        private long nextUserId = 1L;

        @Override
        public User save(User user) {
            user.setUserId(nextUserId++);
            users.put(user.getUsername(), user);
            return user;
        }

        @Override
        public Optional<User> findByUsername(String username) {
            return Optional.ofNullable(users.get(username));
        }

        @Override
        public void updatePassword(long userId, String passwordHash) {
            users.values().stream()
                    .filter(user -> user.getUserId() == userId)
                    .findFirst()
                    .orElseThrow()
                    .setPasswordHash(passwordHash);
        }
    }
}
