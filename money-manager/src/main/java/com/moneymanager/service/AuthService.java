package com.moneymanager.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.moneymanager.model.User;
import com.moneymanager.repository.IUserRepo;

import java.util.Optional;

public class AuthService {

    private final IUserRepo userRepo;

    public AuthService(IUserRepo userRepo) {
        this.userRepo = userRepo;
    }

    public User register(String username, String password, String securityQuestion, String securityAnswer) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        if (password == null || password.length() < 4) {
            throw new IllegalArgumentException("Password must be at least 4 characters");
        }
        if (securityQuestion == null || securityQuestion.isBlank()) {
            throw new IllegalArgumentException("Security question is required");
        }
        if (securityAnswer == null || securityAnswer.isBlank()) {
            throw new IllegalArgumentException("Security answer is required");
        }
        if (userRepo.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already taken");
        }

        String hash = BCrypt.withDefaults().hashToString(12, password.toCharArray());
        String answerHash = BCrypt.withDefaults().hashToString(12, securityAnswer.toCharArray());
        var user = new User();
        user.setUsername(username);
        user.setPasswordHash(hash);
        user.setSecurityQuestion(securityQuestion.trim());
        user.setSecurityAnswerHash(answerHash);
        return userRepo.save(user);
    }

    public Optional<User> login(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return Optional.empty();
        }
        return userRepo.findByUsername(username)
                .filter(user -> {
                    var result = BCrypt.verifyer().verify(password.toCharArray(), user.getPasswordHash());
                    return result.verified;
                });
    }

    public Optional<String> getSecurityQuestion(String username) {
        if (username == null || username.isBlank()) return Optional.empty();
        return userRepo.findByUsername(username).map(User::getSecurityQuestion);
    }

    public void resetPassword(String username, String securityAnswer, String newPassword) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required.");
        }
        if (newPassword == null || newPassword.length() < 4) {
            throw new IllegalArgumentException("Password must be at least 4 characters");
        }
        if (securityAnswer == null || securityAnswer.isBlank()) {
            throw new IllegalArgumentException("Security answer is required.");
        }

        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        if (user.getSecurityAnswerHash() == null || user.getSecurityAnswerHash().isBlank()) {
            throw new IllegalArgumentException("Password reset is not available for this user.");
        }
        var result = BCrypt.verifyer().verify(securityAnswer.toCharArray(), user.getSecurityAnswerHash());
        if (!result.verified) {
            throw new IllegalArgumentException("Invalid security answer.");
        }

        String newHash = BCrypt.withDefaults().hashToString(12, newPassword.toCharArray());
        userRepo.updatePasswordHash(user.getUserId(), newHash);
    }
}
