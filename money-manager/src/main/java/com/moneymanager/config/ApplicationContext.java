package com.moneymanager.config;

import com.moneymanager.repository.JdbcBudgetRepo;
import com.moneymanager.repository.JdbcGoalRepo;
import com.moneymanager.repository.JdbcMonthlyBalanceRepo;
import com.moneymanager.repository.JdbcNoteRepo;
import com.moneymanager.repository.JdbcTransactionRepo;
import com.moneymanager.repository.JdbcUserRepo;
import com.moneymanager.repository.JdbcUserSettingsRepo;
import com.moneymanager.service.AuthService;
import com.moneymanager.service.BudgetService;
import com.moneymanager.service.DashboardService;
import com.moneymanager.service.GoalService;
import com.moneymanager.service.MonthlyIncomeService;
import com.moneymanager.service.NoteService;
import com.moneymanager.service.TransactionService;

public class ApplicationContext {

    private final AuthService authService;
    private final TransactionService transactionService;
    private final BudgetService budgetService;
    private final GoalService goalService;
    private final NoteService noteService;
    private final DashboardService dashboardService;
    private final MonthlyIncomeService monthlyIncomeService;

    public ApplicationContext() {
        var userRepo = new JdbcUserRepo();
        var txRepo = new JdbcTransactionRepo();
        var budgetRepo = new JdbcBudgetRepo();
        var balanceRepo = new JdbcMonthlyBalanceRepo();
        var goalRepo = new JdbcGoalRepo();
        var noteRepo = new JdbcNoteRepo();
        var settingsRepo = new JdbcUserSettingsRepo();

        authService = new AuthService(userRepo);
        transactionService = new TransactionService(txRepo);
        budgetService = new BudgetService(budgetRepo, balanceRepo);
        goalService = new GoalService(goalRepo);
        noteService = new NoteService(noteRepo);
        dashboardService = new DashboardService(txRepo, goalRepo, balanceRepo, settingsRepo);
        monthlyIncomeService = new MonthlyIncomeService(settingsRepo, txRepo, balanceRepo);
    }

    public AuthService authService() {
        return authService;
    }

    public TransactionService transactionService() {
        return transactionService;
    }

    public BudgetService budgetService() {
        return budgetService;
    }

    public GoalService goalService() {
        return goalService;
    }

    public NoteService noteService() {
        return noteService;
    }

    public DashboardService dashboardService() {
        return dashboardService;
    }

    public MonthlyIncomeService monthlyIncomeService() {
        return monthlyIncomeService;
    }
}
