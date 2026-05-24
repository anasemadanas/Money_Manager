package com.moneymanager.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.moneymanager.model.User;
import com.moneymanager.repository.IUserRepo;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class AuthService {

    private static final int MIN_SECRET_LENGTH = 8;
    private static final int MAX_BCRYPT_BYTES = 72;

    private final IUserRepo userRepo;

    public AuthService(IUserRepo userRepo) {
        this.userRepo = userRepo;
    }
    public User register(String username, String password, String confirmPassword, String recoveryCode) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        String normalizedUsername = username.trim();
        if (normalizedUsername.length() > 50) {
            throw new IllegalArgumentException("Username must be 50 characters or fewer");
        }
        validateNewPassword(password);
        if (!password.equals(confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        validateRecoveryCode(recoveryCode);
        if (userRepo.findByUsername(normalizedUsername).isPresent()) {
            throw new IllegalArgumentException("Username already taken");
        }

        var user = new User();
        user.setUsername(normalizedUsername);
        user.setPasswordHash(hash(password));
        user.setRecoveryCodeHash(hash(recoveryCode));
        return userRepo.save(user);
    }
    public Optional<User> login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Optional.empty();
        }
        return userRepo.findByUsername(username.trim())
                .filter(user -> {
                    var result = BCrypt.verifyer().verify(password.toCharArray(), user.getPasswordHash());
                    return result.verified;
                });
    }
    public void resetPassword(String username, String recoveryCode, String newPassword, String confirmPassword) {
        validateNewPassword(newPassword);
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("New passwords do not match");
        }
        if (username == null || username.isBlank() || recoveryCode == null || recoveryCode.isBlank()) {
            throw invalidRecoveryDetails();
        }

        User user = userRepo.findByUsername(username.trim()).orElseThrow(this::invalidRecoveryDetails);
        if (user.getRecoveryCodeHash() == null
                || !BCrypt.verifyer().verify(recoveryCode.toCharArray(), user.getRecoveryCodeHash()).verified) {
            throw invalidRecoveryDetails();
        }

        userRepo.updatePassword(user.getUserId(), hash(newPassword));
    }

    private String hash(String value) {
        return BCrypt.withDefaults().hashToString(12, value.toCharArray());
    }

    private void validateNewPassword(String password) {
        validateSecret(password, "Password");
    }

    private void validateRecoveryCode(String recoveryCode) {
        validateSecret(recoveryCode, "Recovery code");
    }

    private void validateSecret(String value, String name) {
        if (value == null || value.length() < MIN_SECRET_LENGTH) {
            throw new IllegalArgumentException(name + " must be at least " + MIN_SECRET_LENGTH + " characters");
        }
        if (value.getBytes(StandardCharsets.UTF_8).length > MAX_BCRYPT_BYTES) {
            throw new IllegalArgumentException(name + " must be at most " + MAX_BCRYPT_BYTES + " bytes");
        }
    }

    private IllegalArgumentException invalidRecoveryDetails() {
        return new IllegalArgumentException("Unable to reset password. Check your username and recovery code.");
    }
}
