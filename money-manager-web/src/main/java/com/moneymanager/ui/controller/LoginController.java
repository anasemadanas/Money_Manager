package com.moneymanager.ui.controller;

import com.moneymanager.model.User;
import com.moneymanager.repository.DataAccessException;
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
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

public class LoginController {

    @FXML private Label headerLabel;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private VBox confirmPasswordBox;
    @FXML private PasswordField confirmPasswordField;
    @FXML private Label errorLabel;
    @FXML private Button actionButton;
    @FXML private Hyperlink toggleLink;

    private AuthService authService;
    private TransactionService transactionService;
    private BudgetService budgetService;
    private GoalService goalService;
    private NoteService noteService;
    private DashboardService dashboardService;
    private MonthlyIncomeService monthlyIncomeService;
    private Stage stage;
    private boolean loginMode = true;

    /** Called by App.java after FXML loading. */
    public void init(AuthService authService, TransactionService txService,
                     BudgetService budgetService, GoalService goalService,
                     NoteService noteService, DashboardService dashboardService,
                     MonthlyIncomeService monthlyIncomeService, Stage stage) {
        this.authService          = authService;
        this.transactionService   = txService;
        this.budgetService        = budgetService;
        this.goalService          = goalService;
        this.noteService          = noteService;
        this.dashboardService     = dashboardService;
        this.monthlyIncomeService = monthlyIncomeService;
        this.stage                = stage;
    }

    @FXML
    private void handleAction() {
        if (loginMode) doLogin(); else doRegister();
    }

    private void doLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isBlank() || password.isBlank()) {
            showError("Username and password are required.");
            return;
        }
        try {
            authService.login(username, password)
                    .ifPresentOrElse(this::switchToMain,
                            () -> showError("Invalid username or password."));
        } catch (DataAccessException e) {
            showError("Database error. Check your connection.");
            e.printStackTrace();
        }
    }

    private void doRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirm  = confirmPasswordField.getText();

        if (!password.equals(confirm)) {
            showError("Passwords do not match.");
            return;
        }
        try {
            switchToMain(authService.register(username, password));
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (DataAccessException e) {
            showError("Database error. Check your connection.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleToggle() {
        loginMode = !loginMode;
        clearError();
        usernameField.clear();
        passwordField.clear();
        confirmPasswordField.clear();

        if (loginMode) {
            headerLabel.setText("Sign In");
            actionButton.setText("Login");
            toggleLink.setText("Don't have an account? Register");
            confirmPasswordBox.setVisible(false);
            confirmPasswordBox.setManaged(false);
        } else {
            headerLabel.setText("Create Account");
            actionButton.setText("Register");
            toggleLink.setText("Already have an account? Login");
            confirmPasswordBox.setVisible(true);
            confirmPasswordBox.setManaged(true);
        }
    }

    private void switchToMain(User user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/main.fxml"));
            Scene scene = new Scene(loader.load(), 1100, 720);
            MainController ctrl = loader.getController();
            ctrl.init(user, transactionService, budgetService, goalService,
                      noteService, dashboardService, monthlyIncomeService);
            stage.setScene(scene);
            stage.setResizable(true);
            stage.setMinWidth(900);
            stage.setMinHeight(600);
            stage.centerOnScreen();
        } catch (IOException e) {
            AlertHelper.showError(stage, "Error", "Could not load main view: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorLabel.setVisible(true);
        errorLabel.setManaged(true);
    }

    private void clearError() {
        errorLabel.setVisible(false);
        errorLabel.setManaged(false);
    }
}
