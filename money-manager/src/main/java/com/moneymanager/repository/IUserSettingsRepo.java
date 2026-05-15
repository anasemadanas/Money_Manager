package com.moneymanager.repository;

import java.math.BigDecimal;
import java.util.Optional;

public interface IUserSettingsRepo {

    /** Return the saved monthly income for this user, or empty if never set. */
    Optional<BigDecimal> getMonthlyIncome(long userId);

    /** Persist (insert or replace) the monthly income for this user. */
    void setMonthlyIncome(long userId, BigDecimal amount);
}
