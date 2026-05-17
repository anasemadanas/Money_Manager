package com.moneymanager.ui.controller;

import com.moneymanager.model.User;
import com.moneymanager.service.BudgetService;
import com.moneymanager.service.DashboardService;
import com.moneymanager.service.GoalService;
import com.moneymanager.service.MonthlyIncomeService;
import com.moneymanager.service.NoteService;
import com.moneymanager.service.TransactionService;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TabPane;

public class MainController {

    @FXML private Label usernameLabel;
    @FXML private TabPane mainTabPane;
    @FXML private DashboardController dashboardController;
    @FXML private TransactionController transactionsController;
    @FXML private BudgetController budgetsController;
    @FXML private GoalController goalsController;
    @FXML private NoteController notesController;

    /** Called by LoginController after loading main.fxml. */
    public void init(User user, TransactionService txService,
                     BudgetService budgetService, GoalService goalService,
                     NoteService noteService, DashboardService dashboardService,
                     MonthlyIncomeService monthlyIncomeService) {

        usernameLabel.setText("Logged in as: " + user.getUsername());

        // Apply saved monthly income for the current month (idempotent)
        monthlyIncomeService.applyForCurrentMonth(user.getUserId());

        // Initialise all tab controllers
        dashboardController.init(dashboardService, monthlyIncomeService, user);
        budgetsController.init(budgetService, user);
        transactionsController.init(txService, budgetService, user);
        goalsController.init(goalService, user);
        notesController.init(noteService, user);

        // Cross-tab refresh wiring
        transactionsController.setOnDataChanged(() -> {
            dashboardController.refresh();
            budgetsController.refresh();
        });

        dashboardController.setOnSettingChanged(() -> {
            transactionsController.refresh();
            budgetsController.refresh();
        });

        // Tab-switch refresh
        mainTabPane.getSelectionModel().selectedIndexProperty().addListener(
                (obs, oldIdx, newIdx) -> {
                    switch (newIdx.intValue()) {
                        case 0 -> dashboardController.refresh();
                        case 2 -> budgetsController.refresh();
                    }
                });
    }
}
