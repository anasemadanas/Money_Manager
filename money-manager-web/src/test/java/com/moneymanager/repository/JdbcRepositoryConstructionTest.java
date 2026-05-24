package com.moneymanager.repository;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class JdbcRepositoryConstructionTest {

    @Test
    void repositoriesDoNotAccessTheDatabaseDuringConstruction() {
        assertDoesNotThrow(JdbcMonthlyBalanceRepo::new);
        assertDoesNotThrow(JdbcUserSettingsRepo::new);
    }
}
