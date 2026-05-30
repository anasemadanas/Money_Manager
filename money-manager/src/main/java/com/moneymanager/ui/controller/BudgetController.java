package com.moneymanager.ui.controller;

import com.moneymanager.dto.BudgetDTO;
import com.moneymanager.model.User;
import com.moneymanager.repository.DataAccessException;
import com.moneymanager.service.BudgetService;
import com.moneymanager.ui.util.AlertHelper;
import com.moneymanager.ui.util.Categories;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BudgetController {

    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private ComboBox<String>  monthCombo;
    @FXML private ComboBox<Integer> yearCombo;
    @FXML private ListView<BudgetDTO> budgetListView;
    @FXML private Label summaryLabel;

    @FXML private Button setBalanceButton;
    @FXML private Label  balancePlaceholderLabel;
    @FXML private VBox   balanceProgressPane;
    @FXML private Label  balanceSpentLabel;
    @FXML private Label  balanceRemainingLabel;
    @FXML private ProgressBar monthlyBalanceBar;

    private BudgetService budgetService;
    private User currentUser;

    private static final String[] MONTH_NAMES = {
        "January","February","March","April","May","June",
        "July","August","September","October","November","December"
    };
    private static final Logger LOGGER = Logger.getLogger(BudgetController.class.getName());
    private static final NumberFormat CF = NumberFormat.getCurrencyInstance(Locale.US);

    @FXML
    public void initialize() {
        monthCombo.getItems().addAll(MONTH_NAMES);
        monthCombo.getSelectionModel().select(LocalDate.now().getMonthValue() - 1);

        int currentYear = LocalDate.now().getYear();
        for (int y = 2020; y <= currentYear + 2; y++) yearCombo.getItems().add(y);
        yearCombo.setValue(currentYear);

        editButton.disableProperty().bind(
                budgetListView.getSelectionModel().selectedItemProperty().isNull());
        deleteButton.disableProperty().bind(
                budgetListView.getSelectionModel().selectedItemProperty().isNull());

        budgetListView.setCellFactory(lv -> new BudgetCell());
        budgetListView.setPlaceholder(
                new Label("No budgets for this month. Click '+ Add Budget' to create one."));

        monthCombo.valueProperty().addListener((obs, o, n) -> loadData());
        yearCombo.valueProperty().addListener((obs, o, n) -> loadData());
    }

    public void init(BudgetService budgetService, User user) {
        this.budgetService = budgetService;
        this.currentUser   = user;
        loadData();
    }

    public void refresh() {
        loadData();
    }

    @FXML
    private void handleAdd() {
        int month = getSelectedMonth();
        int year  = getSelectedYear();
        showAddDialog(MONTH_NAMES[month - 1] + " " + year).ifPresent(dto -> {
            try {
                budgetService.add(currentUser.getUserId(),
                        dto.category(), dto.amountCap(), month, year);
                Logger.getLogger("com.moneymanager")
                        .info("user=" + currentUser.getUsername()
                              + " action=budget_created details=amount="
                              + dto.amountCap().setScale(2, java.math.RoundingMode.HALF_UP)
                              + ", month=" + month
                              + ", year=" + year);
                loadData();
            } catch (IllegalArgumentException e) {
                AlertHelper.showError(getStage(), "Validation Error", e.getMessage());
            } catch (DataAccessException e) {
                AlertHelper.showError(getStage(), "Error", "Could not save budget.");
                LOGGER.log(Level.SEVERE, "Could not save budget", e);
            }
        });
    }

    @FXML
    private void handleEdit() {
        BudgetDTO selected = budgetListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        showEditCapDialog(selected).ifPresent(newCap -> {
            try {
                budgetService.updateCap(selected.budgetId(), currentUser.getUserId(), newCap);
                Logger.getLogger("com.moneymanager")
                        .info("user=" + currentUser.getUsername()
                              + " action=budget_updated details=amount="
                              + newCap.setScale(2, java.math.RoundingMode.HALF_UP)
                              + ", month=" + selected.month()
                              + ", year=" + selected.year());
                loadData();
            } catch (IllegalArgumentException e) {
                AlertHelper.showError(getStage(), "Validation Error", e.getMessage());
            } catch (DataAccessException e) {
                AlertHelper.showError(getStage(), "Error", "Could not update budget.");
                LOGGER.log(Level.SEVERE, "Could not update budget", e);
            }
        });
    }

    @FXML
    private void handleDelete() {
        BudgetDTO selected = budgetListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        if (!AlertHelper.showConfirm(getStage(), "Delete Budget",
                "Delete the budget for \"" + selected.category() + "\"?\nThis cannot be undone."))
            return;
        try {
            budgetService.delete(selected.budgetId(), currentUser.getUserId());
            Logger.getLogger("com.moneymanager")
                    .info("user=" + currentUser.getUsername()
                          + " action=budget_deleted details=amount="
                          + selected.amountCap().setScale(2, java.math.RoundingMode.HALF_UP)
                          + ", month=" + selected.month()
                          + ", year=" + selected.year());
            loadData();
        } catch (DataAccessException e) {
            AlertHelper.showError(getStage(), "Error", "Could not delete budget.");
            LOGGER.log(Level.SEVERE, "Could not delete budget", e);
        }
    }

    @FXML
    private void handleSetBalance() {
        int month = getSelectedMonth();
        int year  = getSelectedYear();
        Optional<BigDecimal> current = budgetService.getMonthlyBalance(
                currentUser.getUserId(), month, year);

        showSetBalanceDialog(MONTH_NAMES[month - 1] + " " + year, current.orElse(null))
                .ifPresent(amount -> {
                    try {
                        budgetService.setMonthlyBalance(currentUser.getUserId(), amount, month, year);
                        loadData();
                    } catch (IllegalArgumentException e) {
                        AlertHelper.showError(getStage(), "Validation Error", e.getMessage());
                    } catch (DataAccessException e) {
                        AlertHelper.showError(getStage(), "Error", "Could not save monthly budget.");
                        LOGGER.log(Level.SEVERE, "Could not save monthly budget", e);
                    }
                });
    }

    private void loadData() {
        if (budgetService == null) return;
        int month = getSelectedMonth();
        int year  = getSelectedYear();

        updateBalanceCard(month, year);

        List<BudgetDTO> budgets = budgetService.getBudgets(
                currentUser.getUserId(), month, year);
        budgetListView.setItems(FXCollections.observableArrayList(budgets));
        updateSummary(budgets, month, year);
    }

    private void updateBalanceCard(int month, int year) {
        Optional<BigDecimal> total = budgetService.getMonthlyBalance(
                currentUser.getUserId(), month, year);

        if (total.isEmpty()) {
            balancePlaceholderLabel.setVisible(true);
            balancePlaceholderLabel.setManaged(true);
            balanceProgressPane.setVisible(false);
            balanceProgressPane.setManaged(false);
            setBalanceButton.setText("Set Monthly Budget");
            return;
        }

        BigDecimal cap   = total.get();
        BigDecimal spent = budgetService.getTotalMonthlyExpenses(currentUser.getUserId(), month, year);
        BigDecimal remaining = cap.subtract(spent).max(BigDecimal.ZERO);
        double pct = cap.compareTo(BigDecimal.ZERO) == 0 ? 0.0
                : spent.divide(cap, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue();

        balanceSpentLabel.setText(CF.format(spent) + " spent of " + CF.format(cap));
        balanceRemainingLabel.setText(CF.format(remaining) + " remaining");

        monthlyBalanceBar.setProgress(Math.min(pct / 100.0, 1.0));
        monthlyBalanceBar.getStyleClass().removeAll("amber", "red");
        if (pct >= 100)      monthlyBalanceBar.getStyleClass().add("red");
        else if (pct >= 80)  monthlyBalanceBar.getStyleClass().add("amber");

        balanceRemainingLabel.getStyleClass().removeAll("pct-green", "pct-amber", "pct-red");
        if (pct >= 100)      balanceRemainingLabel.getStyleClass().add("pct-red");
        else if (pct >= 80)  balanceRemainingLabel.getStyleClass().add("pct-amber");
        else                 balanceRemainingLabel.getStyleClass().add("pct-green");

        setBalanceButton.setText("Edit Monthly Budget");
        balancePlaceholderLabel.setVisible(false);
        balancePlaceholderLabel.setManaged(false);
        balanceProgressPane.setVisible(true);
        balanceProgressPane.setManaged(true);
    }

    private void updateSummary(List<BudgetDTO> budgets, int month, int year) {
        if (budgets.isEmpty()) {
            summaryLabel.setText("No category budgets set for " +
                    MONTH_NAMES[month - 1] + " " + year + ".");
            return;
        }
        BigDecimal totalCap   = budgets.stream().map(BudgetDTO::amountCap)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSpent = budgets.stream().map(BudgetDTO::spent)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long overCount = budgets.stream()
                .filter(b -> b.spent().compareTo(b.amountCap()) >= 0).count();
        String overText = overCount == 0 ? "none over budget" : overCount + " over budget";
        summaryLabel.setText(String.format(
                "%d budget(s)   |   Total cap: %s   |   Total spent: %s   |   %s",
                budgets.size(), CF.format(totalCap), CF.format(totalSpent), overText));
    }

    private Optional<BudgetDTO> showAddDialog(String periodLabel) {
        Dialog<BudgetDTO> dialog = new Dialog<>();
        dialog.setTitle("New Budget — " + periodLabel);
        dialog.setHeaderText(null);
        dialog.initOwner(getStage());
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/style.css").toExternalForm());

        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(12);
        grid.setPadding(new Insets(20, 28, 10, 28));
        grid.setMinWidth(380);

        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll(Categories.ALL);
        categoryCombo.setPromptText("Select category");
        categoryCombo.setPrefWidth(220);

        TextField capField = new TextField();
        capField.setPromptText("0.00");
        capField.setPrefWidth(220);
        capField.getStyleClass().add("form-field");

        grid.add(makeLabel("Category *"),   0, 0);  grid.add(categoryCombo, 1, 0);
        grid.add(makeLabel("Budget Cap *"), 0, 1);  grid.add(capField,      1, 1);
        dialog.getDialogPane().setContent(grid);

        Node okNode = dialog.getDialogPane().lookupButton(saveBtn);
        okNode.setDisable(true);
        Runnable validate = () -> okNode.setDisable(
                categoryCombo.getValue() == null || !isPositiveDecimal(capField.getText().trim()));
        categoryCombo.valueProperty().addListener((o, a, b) -> validate.run());
        capField.textProperty().addListener((o, a, b) -> validate.run());

        dialog.setResultConverter(bt -> {
            if (bt != saveBtn) return null;
            try {
                return new BudgetDTO(0, categoryCombo.getValue(),
                        new BigDecimal(capField.getText().trim()), BigDecimal.ZERO,
                        getSelectedMonth(), getSelectedYear());
            } catch (NumberFormatException e) { return null; }
        });
        return dialog.showAndWait().filter(d -> d != null);
    }

    private Optional<BigDecimal> showEditCapDialog(BudgetDTO budget) {
        Dialog<BigDecimal> dialog = new Dialog<>();
        dialog.setTitle("Edit Budget — " + budget.category());
        dialog.setHeaderText(null);
        dialog.initOwner(getStage());
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/style.css").toExternalForm());

        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(12);
        grid.setPadding(new Insets(20, 28, 10, 28));
        grid.setMinWidth(360);

        Label catLabel = new Label(budget.category());
        catLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        TextField capField = new TextField(budget.amountCap().toPlainString());
        capField.setPrefWidth(200);
        capField.getStyleClass().add("form-field");

        grid.add(makeLabel("Category"),  0, 0);  grid.add(catLabel, 1, 0);
        grid.add(makeLabel("New Cap *"), 0, 1);  grid.add(capField, 1, 1);
        dialog.getDialogPane().setContent(grid);

        Node okNode = dialog.getDialogPane().lookupButton(saveBtn);
        capField.textProperty().addListener((o, a, b) ->
                okNode.setDisable(!isPositiveDecimal(b.trim())));

        dialog.setResultConverter(bt -> {
            if (bt != saveBtn) return null;
            try { return new BigDecimal(capField.getText().trim()); }
            catch (NumberFormatException e) { return null; }
        });
        return dialog.showAndWait().filter(d -> d != null);
    }

    private Optional<BigDecimal> showSetBalanceDialog(String periodLabel, BigDecimal current) {
        Dialog<BigDecimal> dialog = new Dialog<>();
        dialog.setTitle("Monthly Budget — " + periodLabel);
        dialog.setHeaderText(null);
        dialog.initOwner(getStage());
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/style.css").toExternalForm());

        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(12);
        grid.setPadding(new Insets(20, 28, 10, 28));
        grid.setMinWidth(360);

        TextField amtField = new TextField(current != null ? current.toPlainString() : "");
        amtField.setPromptText("e.g. 3000.00");
        amtField.setPrefWidth(220);
        amtField.getStyleClass().add("form-field");

        Label hint = new Label("Expense transactions this month will be deducted\nfrom this balance.");
        hint.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");

        grid.add(makeLabel("Total Budget *"), 0, 0);  grid.add(amtField, 1, 0);
        grid.add(new Label(),                 0, 1);  grid.add(hint,     1, 1);
        dialog.getDialogPane().setContent(grid);

        Node okNode = dialog.getDialogPane().lookupButton(saveBtn);
        okNode.setDisable(!isPositiveDecimal(amtField.getText().trim()));
        amtField.textProperty().addListener((o, a, b) ->
                okNode.setDisable(!isPositiveDecimal(b.trim())));

        dialog.setResultConverter(bt -> {
            if (bt != saveBtn) return null;
            try { return new BigDecimal(amtField.getText().trim()); }
            catch (NumberFormatException e) { return null; }
        });
        return dialog.showAndWait().filter(d -> d != null);
    }

    private int getSelectedMonth() {
        int idx = monthCombo.getSelectionModel().getSelectedIndex();
        return idx < 0 ? LocalDate.now().getMonthValue() : idx + 1;
    }

    private int getSelectedYear() {
        Integer y = yearCombo.getValue();
        return y == null ? LocalDate.now().getYear() : y;
    }

    private Stage getStage() { return (Stage) budgetListView.getScene().getWindow(); }

    private static Label makeLabel(String text) {
        var lbl = new Label(text);
        lbl.getStyleClass().add("field-label");
        return lbl;
    }

    private static boolean isPositiveDecimal(String s) {
        if (s == null || s.isBlank()) return false;
        try { return new BigDecimal(s).compareTo(BigDecimal.ZERO) > 0; }
        catch (NumberFormatException e) { return false; }
    }

    private static final class BudgetCell extends ListCell<BudgetDTO> {
        @Override
        protected void updateItem(BudgetDTO item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) { setGraphic(null); setText(null); return; }

            double pct = item.amountCap().compareTo(BigDecimal.ZERO) == 0 ? 0.0
                    : item.spent().divide(item.amountCap(), 4, RoundingMode.HALF_UP)
                           .multiply(BigDecimal.valueOf(100)).doubleValue();

            Label catLabel = new Label(item.category());
            catLabel.getStyleClass().add("budget-category");

            Label amtLabel = new Label(CF.format(item.spent()) + " spent of " + CF.format(item.amountCap()));
            amtLabel.getStyleClass().add("budget-amount");

            VBox textBox = new VBox(3, catLabel, amtLabel);
            HBox.setHgrow(textBox, Priority.ALWAYS);

            ProgressBar bar = new ProgressBar(Math.min(pct / 100.0, 1.0));
            bar.setPrefWidth(220); bar.setPrefHeight(12);
            if (pct >= 100)     bar.getStyleClass().add("red");
            else if (pct >= 80) bar.getStyleClass().add("amber");

            Label pctLabel = new Label(String.format("%.0f%%", pct));
            pctLabel.setMinWidth(48);
            pctLabel.setAlignment(Pos.CENTER_RIGHT);
            if (pct >= 100)     pctLabel.getStyleClass().add("pct-red");
            else if (pct >= 80) pctLabel.getStyleClass().add("pct-amber");
            else                pctLabel.getStyleClass().add("pct-green");

            HBox row = new HBox(16, textBox, bar, pctLabel);
            row.setAlignment(Pos.CENTER_LEFT);
            row.setPadding(new Insets(14, 16, 14, 16));
            row.getStyleClass().add("budget-row");

            setGraphic(row); setText(null);
        }

        private static final NumberFormat CF = NumberFormat.getCurrencyInstance(Locale.US);
    }
}
