package com.moneymanager.repository;

import com.moneymanager.model.User;

import java.util.Optional;

public interface IUserRepo {

    /** Persist a new user and return it with the generated ID. */
    User save(User user);

    /** Find a user by username, or empty if not found. */
    Optional<User> findByUsername(String username);

    /** Update a user's password hash (for password reset). */
    void updatePasswordHash(long userId, String passwordHash);
}
