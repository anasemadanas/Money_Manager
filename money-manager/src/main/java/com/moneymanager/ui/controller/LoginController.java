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
    @FXML private VBox securityQuestionBox;
    @FXML private TextField securityQuestionField;
    @FXML private VBox securityAnswerBox;
    @FXML private PasswordField securityAnswerField;
    @FXML private Label errorLabel;
    @FXML private Button actionButton;
    @FXML private Hyperlink toggleLink;
    @FXML private Hyperlink forgotLink;

    private AuthService authService;
    private TransactionService transactionService;
    private BudgetService budgetService;
    private GoalService goalService;
    private NoteService noteService;
    private DashboardService dashboardService;
    private MonthlyIncomeService monthlyIncomeService;
    private Stage stage;

    private enum Mode { LOGIN, REGISTER, RESET_PASSWORD }
    private Mode mode = Mode.LOGIN;

    @FXML
    private void initialize() {
        render();
    }

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
        switch (mode) {
            case LOGIN -> doLogin();
            case REGISTER -> doRegister();
            case RESET_PASSWORD -> doResetPassword();
        }
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
        String question = securityQuestionField.getText().trim();
        String answer   = securityAnswerField.getText();

        if (!password.equals(confirm)) {
            showError("Passwords do not match.");
            return;
        }
        try {
            switchToMain(authService.register(username, password, question, answer));
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (DataAccessException e) {
            showError("Database error. Check your connection.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleToggle() {
        mode = (mode == Mode.LOGIN) ? Mode.REGISTER : Mode.LOGIN;
        clearInputs();
        clearError();
        render();
    }

    @FXML
    private void handleForgot() {
        mode = Mode.RESET_PASSWORD;
        clearError();
        passwordField.clear();
        confirmPasswordField.clear();
        securityAnswerField.clear();
        render();

        String username = usernameField.getText().trim();
        if (!username.isBlank()) {
            try {
                securityQuestionField.setText(authService.getSecurityQuestion(username).orElse(""));
            } catch (DataAccessException e) {
                showError("Database error. Check your connection.");
                e.printStackTrace();
            }
        } else {
            securityQuestionField.setText("");
        }
    }

    private void doResetPassword() {
        String username = usernameField.getText().trim();
        String newPassword = passwordField.getText();
        String confirm = confirmPasswordField.getText();
        String answer = securityAnswerField.getText();

        if (!newPassword.equals(confirm)) {
            showError("Passwords do not match.");
            return;
        }
        try {
            authService.resetPassword(username, answer, newPassword);
            mode = Mode.LOGIN;
            clearInputs();
            render();
            showError("Password reset successful. Please login.");
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (DataAccessException e) {
            showError("Database error. Check your connection.");
            e.printStackTrace();
        }
    }

    private void render() {
        if (mode == Mode.LOGIN) {
            headerLabel.setText("Sign In");
            actionButton.setText("Login");
            toggleLink.setText("Don't have an account? Register");

            confirmPasswordBox.setVisible(false);
            confirmPasswordBox.setManaged(false);
            securityQuestionBox.setVisible(false);
            securityQuestionBox.setManaged(false);
            securityAnswerBox.setVisible(false);
            securityAnswerBox.setManaged(false);
            forgotLink.setVisible(true);
            forgotLink.setManaged(true);
            return;
        }

        if (mode == Mode.REGISTER) {
            headerLabel.setText("Create Account");
            actionButton.setText("Register");
            toggleLink.setText("Already have an account? Login");

            confirmPasswordBox.setVisible(true);
            confirmPasswordBox.setManaged(true);
            securityQuestionBox.setVisible(true);
            securityQuestionBox.setManaged(true);
            securityQuestionField.setEditable(true);
            securityAnswerBox.setVisible(true);
            securityAnswerBox.setManaged(true);
            forgotLink.setVisible(false);
            forgotLink.setManaged(false);
            return;
        }

        // RESET_PASSWORD
        headerLabel.setText("Reset Password");
        actionButton.setText("Reset");
        toggleLink.setText("Back to Login");

        confirmPasswordBox.setVisible(true);
        confirmPasswordBox.setManaged(true);
        securityQuestionBox.setVisible(true);
        securityQuestionBox.setManaged(true);
        securityQuestionField.setEditable(false);
        securityAnswerBox.setVisible(true);
        securityAnswerBox.setManaged(true);
        forgotLink.setVisible(false);
        forgotLink.setManaged(false);
    }

    private void clearInputs() {
        usernameField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
        securityQuestionField.clear();
        securityAnswerField.clear();
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
