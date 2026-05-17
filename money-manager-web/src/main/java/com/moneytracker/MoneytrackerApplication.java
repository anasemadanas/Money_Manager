package com.moneytracker;

import com.moneymanager.config.DatabaseInitializer;
import com.moneymanager.repository.JdbcBudgetRepo;
import com.moneymanager.repository.JdbcGoalRepo;
import com.moneymanager.repository.JdbcMonthlyBalanceRepo;
import com.moneymanager.repository.JdbcTransactionRepo;
import com.moneymanager.repository.JdbcUserRepo;
import com.moneymanager.service.AuthService;
import com.moneymanager.service.BudgetService;
import com.moneymanager.service.DashboardService;
import com.moneymanager.service.TransactionService;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class MoneytrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MoneytrackerApplication.class, args);
    }

    @Bean
    ApplicationRunner databaseInitializer() {
        return args -> DatabaseInitializer.init();
    }

    @Bean
    AuthService authService() {
        return new AuthService(new JdbcUserRepo());
    }

    @Bean
    TransactionService transactionService() {
        return new TransactionService(new JdbcTransactionRepo());
    }

    @Bean
    BudgetService budgetService() {
        return new BudgetService(new JdbcBudgetRepo(), new JdbcMonthlyBalanceRepo());
    }

    @Bean
    DashboardService dashboardService() {
        return new DashboardService(new JdbcTransactionRepo(), new JdbcGoalRepo());
    }
}
