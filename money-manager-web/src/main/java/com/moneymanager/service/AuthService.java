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

    /**
     * Register a new user. Returns the saved user on success.
     *
     * @throws IllegalArgumentException if username/password is blank or username is taken
     */
    public User register(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        if (password == null || password.length() < 4) {
            throw new IllegalArgumentException("Password must be at least 4 characters");
        }
        if (userRepo.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already taken");
        }

        String hash = BCrypt.withDefaults().hashToString(12, password.toCharArray());
        var user = new User();
        user.setUsername(username);
        user.setPasswordHash(hash);
        return userRepo.save(user);
    }

    /**
     * Authenticate a user by username and password.
     *
     * @return the User if credentials are valid, or empty if not
     */
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
}
