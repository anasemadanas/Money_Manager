package com.moneymanager.ui.controller;

import com.moneymanager.model.User;
import com.moneymanager.service.AuthService;
import com.moneymanager.service.BudgetService;
import com.moneymanager.service.DashboardService;
import com.moneymanager.service.GoalService;
import com.moneymanager.service.MonthlyIncomeService;
import com.moneymanager.service.NoteService;
import com.moneymanager.service.TransactionService;
import com.moneymanager.ui.util.AlertHelper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;

import java.util.logging.Level;
import java.util.logging.Logger;

public class MainController {

    @FXML private Label usernameLabel;
    @FXML private TabPane mainTabPane;
    @FXML private DashboardController dashboardController;
    @FXML private TransactionController transactionsController;
    @FXML private BudgetController budgetsController;
    @FXML private GoalController goalsController;
    @FXML private NoteController notesController;

    private User currentUser;
    private AuthService authService;
    private TransactionService transactionService;
    private BudgetService budgetService;
    private GoalService goalService;
    private NoteService noteService;
    private DashboardService dashboardService;
    private MonthlyIncomeService monthlyIncomeService;
    private static final Logger LOGGER = Logger.getLogger(MainController.class.getName());

    public void init(User user, AuthService authService, TransactionService txService,
                     BudgetService budgetService, GoalService goalService,
                     NoteService noteService, DashboardService dashboardService,
                     MonthlyIncomeService monthlyIncomeService) {

        this.currentUser = user;
        this.authService = authService;
        this.transactionService = txService;
        this.budgetService = budgetService;
        this.goalService = goalService;
        this.noteService = noteService;
        this.dashboardService = dashboardService;
        this.monthlyIncomeService = monthlyIncomeService;

        usernameLabel.setText("Logged in as: " + user.getUsername());

        monthlyIncomeService.applyForCurrentMonth(user.getUserId());

        dashboardController.init(dashboardService, monthlyIncomeService, user);
        dashboardController.setOnLogout(this::logout);

        budgetsController.init(budgetService, user);
        transactionsController.init(txService, budgetService, user);
        goalsController.init(goalService, user);
        notesController.init(noteService, user);

        transactionsController.setOnDataChanged(() -> {
            dashboardController.refresh();
            budgetsController.refresh();
        });

        dashboardController.setOnSettingChanged(() -> {
            transactionsController.refresh();
            budgetsController.refresh();
        });

        mainTabPane.getSelectionModel().selectedIndexProperty().addListener(
                (obs, oldIdx, newIdx) -> {
                    switch (newIdx.intValue()) {
                        case 0 -> dashboardController.refresh();
                        case 2 -> budgetsController.refresh();
                    }
                });
    }

    private void logout() {
        java.util.logging.Logger.getLogger("com.moneymanager")
                .info("user=" + currentUser.getUsername() + " action=exit");

        try {
            Stage stage = (Stage) usernameLabel.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/login.fxml"));
            javafx.scene.Scene scene = new javafx.scene.Scene(loader.load(), 900, 600);

            LoginController ctrl = loader.getController();
            ctrl.init(authService, transactionService, budgetService, goalService,
                      noteService, dashboardService, monthlyIncomeService, stage);

            stage.setScene(scene);
            stage.setResizable(false);
            stage.centerOnScreen();
        } catch (java.io.IOException e) {
            AlertHelper.showError((Stage) usernameLabel.getScene().getWindow(), "Error", "Could not load login view: " + e.getMessage());
            LOGGER.log(Level.SEVERE, "Could not load login view", e);
        }
    }
}
