package com.moneymanager.repository;

import java.math.BigDecimal;
import java.util.Optional;

public interface IUserSettingsRepo {
    Optional<BigDecimal> getMonthlyIncome(long userId);
    void setMonthlyIncome(long userId, BigDecimal amount);
}
