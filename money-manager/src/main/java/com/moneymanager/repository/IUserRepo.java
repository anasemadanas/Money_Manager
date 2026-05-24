package com.moneymanager.repository;

import com.moneymanager.model.User;

import java.util.Optional;

public interface IUserRepo {

    User save(User user);

    Optional<User> findByUsername(String username);

    void updatePasswordHash(long userId, String passwordHash);
}
