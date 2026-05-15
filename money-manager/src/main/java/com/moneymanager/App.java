package com.moneymanager;

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
import com.moneymanager.ui.controller.LoginController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import com.moneymanager.config.DatabaseInitializer;

public class App extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        DatabaseInitializer.init();
        // ── Repositories ──────────────────────────────────────────────────────
        var userRepo     = new JdbcUserRepo();
        var txRepo       = new JdbcTransactionRepo();
        var budgetRepo   = new JdbcBudgetRepo();
        var balanceRepo  = new JdbcMonthlyBalanceRepo();
        var goalRepo     = new JdbcGoalRepo();
        var noteRepo     = new JdbcNoteRepo();
        var settingsRepo = new JdbcUserSettingsRepo();

        // ── Services ──────────────────────────────────────────────────────────
        var authService          = new AuthService(userRepo);
        var txService            = new TransactionService(txRepo);
        var budgetService        = new BudgetService(budgetRepo, balanceRepo);
        var goalService          = new GoalService(goalRepo);
        var noteService          = new NoteService(noteRepo);
        var dashboardService     = new DashboardService(txRepo, goalRepo);
        var monthlyIncomeService = new MonthlyIncomeService(settingsRepo, txRepo, balanceRepo);

        // ── Login screen ──────────────────────────────────────────────────────
        FXMLLoader loader = new FXMLLoader(App.class.getResource("/fxml/login.fxml"));
        Scene scene = new Scene(loader.load(), 900, 600);

        LoginController ctrl = loader.getController();
        ctrl.init(authService, txService, budgetService, goalService,
                  noteService, dashboardService, monthlyIncomeService, stage);

        stage.setTitle("Money Manager");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
