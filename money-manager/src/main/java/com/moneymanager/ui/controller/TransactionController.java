package com.moneymanager.ui.controller;

import com.moneymanager.dto.TransactionDTO;
import com.moneymanager.model.User;
import com.moneymanager.repository.DataAccessException;
import com.moneymanager.service.BudgetService;
import com.moneymanager.service.TransactionService;
import com.moneymanager.ui.util.AlertHelper;
import com.moneymanager.ui.util.Categories;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class TransactionController {

    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private DatePicker fromDatePicker;
    @FXML private DatePicker toDatePicker;
    @FXML private ComboBox<String> categoryFilter;
    @FXML private TableView<TransactionDTO> transactionTable;
    @FXML private TableColumn<TransactionDTO, String> dateCol;
    @FXML private TableColumn<TransactionDTO, String> nameCol;
    @FXML private TableColumn<TransactionDTO, String> categoryCol;
    @FXML private TableColumn<TransactionDTO, String> typeCol;
    @FXML private TableColumn<TransactionDTO, String> amountCol;
    @FXML private Label summaryLabel;

    private TransactionService transactionService;
    private BudgetService budgetService;
    private User currentUser;
    private boolean suppressFilter = false;
    private Runnable onDataChanged;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    @FXML
    public void initialize() {
        setupColumns();
        setupTable();

        editButton.disableProperty().bind(
                transactionTable.getSelectionModel().selectedItemProperty().isNull());
        deleteButton.disableProperty().bind(
                transactionTable.getSelectionModel().selectedItemProperty().isNull());

        categoryFilter.getItems().add("All");
        categoryFilter.getItems().addAll(Categories.ALL);
        categoryFilter.setValue("All");

        fromDatePicker.valueProperty().addListener((obs, o, n) -> applyFilter());
        toDatePicker.valueProperty().addListener((obs, o, n) -> applyFilter());
        categoryFilter.valueProperty().addListener((obs, o, n) -> applyFilter());
    }

    public void init(TransactionService txService, BudgetService budgetService, User user) {
        this.transactionService = txService;
        this.budgetService      = budgetService;
        this.currentUser        = user;
        loadData();
    }

    public void setOnDataChanged(Runnable callback) {
        this.onDataChanged = callback;
    }

    public void refresh() {
        loadData();
    }

    @FXML
    private void handleAdd() {
        showFormDialog("New Transaction", null).ifPresent(dto -> {
            if ("EXPENSE".equals(dto.txType())) {
                String catErr = budgetService.checkCategoryLimit(
                        currentUser.getUserId(), dto.category(), dto.amount(), dto.txDate(), 0L);
                if (catErr != null) {
                    AlertHelper.showError(getStage(), "Category Budget Exceeded", catErr);
                    return;
                }
                String balErr = budgetService.checkMonthlyBalanceLimit(
                        currentUser.getUserId(), dto.amount(), dto.txDate(), 0L);
                if (balErr != null) {
                    AlertHelper.showError(getStage(), "Monthly Budget Exceeded", balErr);
                    return;
                }
            }
            try {
                transactionService.add(currentUser.getUserId(), dto);
                java.util.logging.Logger.getLogger("com.moneymanager")
                        .info("user=" + currentUser.getUsername()
                              + " action=transaction_added details=amount="
                              + dto.amount().setScale(2, java.math.RoundingMode.HALF_UP)
                              + ", category=" + dto.category()
                              + ", month=" + dto.txDate().getMonthValue()
                              + ", year=" + dto.txDate().getYear());
                notifyDataChanged();
                loadData();
                if ("EXPENSE".equals(dto.txType())) {
                    String warning = budgetService.getCategoryWarning(
                            currentUser.getUserId(), dto.category(), dto.txDate());
                    if (warning != null)
                        AlertHelper.showInfo(getStage(), "Budget Warning", warning);
                }
            } catch (IllegalArgumentException e) {
                AlertHelper.showError(getStage(), "Validation Error", e.getMessage());
            } catch (DataAccessException e) {
                AlertHelper.showError(getStage(), "Error", "Could not save transaction.");
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void handleEdit() {
        TransactionDTO selected = transactionTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        showFormDialog("Edit Transaction", selected).ifPresent(dto -> {
            if ("EXPENSE".equals(dto.txType())) {
                String catErr = budgetService.checkCategoryLimit(
                        currentUser.getUserId(), dto.category(), dto.amount(),
                        dto.txDate(), selected.transactionId());
                if (catErr != null) {
                    AlertHelper.showError(getStage(), "Category Budget Exceeded", catErr);
                    return;
                }
                String balErr = budgetService.checkMonthlyBalanceLimit(
                        currentUser.getUserId(), dto.amount(),
                        dto.txDate(), selected.transactionId());
                if (balErr != null) {
                    AlertHelper.showError(getStage(), "Monthly Budget Exceeded", balErr);
                    return;
                }
            }
            try {
                transactionService.update(selected.transactionId(), currentUser.getUserId(), dto);
                notifyDataChanged();
                loadData();
                if ("EXPENSE".equals(dto.txType())) {
                    String warning = budgetService.getCategoryWarning(
                            currentUser.getUserId(), dto.category(), dto.txDate());
                    if (warning != null)
                        AlertHelper.showInfo(getStage(), "Budget Warning", warning);
                }
            } catch (IllegalArgumentException e) {
                AlertHelper.showError(getStage(), "Validation Error", e.getMessage());
            } catch (DataAccessException e) {
                AlertHelper.showError(getStage(), "Error", "Could not update transaction.");
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void handleDelete() {
        TransactionDTO selected = transactionTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        if (!AlertHelper.showConfirm(getStage(), "Delete Transaction",
                "Delete \"" + selected.name() + "\"?\nThis cannot be undone.")) return;
        try {
            transactionService.delete(selected.transactionId());
            java.util.logging.Logger.getLogger("com.moneymanager")
                    .info("user=" + currentUser.getUsername()
                          + " action=transaction_deleted details=amount="
                          + selected.amount().setScale(2, java.math.RoundingMode.HALF_UP)
                          + ", category=" + selected.category()
                          + ", month=" + selected.txDate().getMonthValue()
                          + ", year=" + selected.txDate().getYear());
            notifyDataChanged();
            loadData();
        } catch (DataAccessException e) {
            AlertHelper.showError(getStage(), "Error", "Could not delete transaction.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleClearFilter() {
        suppressFilter = true;
        fromDatePicker.setValue(null);
        toDatePicker.setValue(null);
        suppressFilter = false;
        categoryFilter.setValue("All");
    }

    private void loadData() { applyFilter(); }

    private void applyFilter() {
        if (transactionService == null || suppressFilter) return;
        LocalDate from = fromDatePicker.getValue();
        LocalDate to   = toDatePicker.getValue();
        String cat = "All".equals(categoryFilter.getValue()) ? null : categoryFilter.getValue();
        List<TransactionDTO> rows = transactionService.getFiltered(
                currentUser.getUserId(), from, to, cat);
        transactionTable.setItems(FXCollections.observableArrayList(rows));
        updateSummary(rows);
    }

    private void notifyDataChanged() {
        if (onDataChanged != null) onDataChanged.run();
    }

    private void setupColumns() {
        dateCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().txDate().format(DATE_FMT)));
        nameCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().name()));
        categoryCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().category()));

        typeCol.setCellValueFactory(c ->
                new SimpleStringProperty(c.getValue().txType()));
        typeCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); setStyle(""); return; }
                setText(item);
                setStyle("INCOME".equals(item)
                        ? "-fx-text-fill: #16a34a; -fx-font-weight: bold;"
                        : "-fx-text-fill: #dc2626; -fx-font-weight: bold;");
            }
        });

        amountCol.setCellValueFactory(c -> {
            TransactionDTO tx = c.getValue();
            return new SimpleStringProperty(
                    "$" + tx.amount().setScale(2, RoundingMode.HALF_UP).toPlainString());
        });
        amountCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || getIndex() >= getTableView().getItems().size()) {
                    setText(null); setStyle(""); return;
                }
                setText(item);
                TransactionDTO tx = getTableView().getItems().get(getIndex());
                setStyle("INCOME".equals(tx.txType())
                        ? "-fx-text-fill: #16a34a; -fx-font-weight: bold;"
                        : "-fx-text-fill: #dc2626; -fx-font-weight: bold;");
            }
        });
    }

    private void setupTable() {
        transactionTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        transactionTable.setPlaceholder(
                new Label("No transactions yet. Click '+ Add' to create one."));
        transactionTable.setRowFactory(tv -> {
            TableRow<TransactionDTO> row = new TableRow<>();
            row.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && !row.isEmpty()) handleEdit();
            });
            return row;
        });
    }

    private void updateSummary(List<TransactionDTO> rows) {
        BigDecimal income = rows.stream().filter(t -> "INCOME".equals(t.txType()))
                .map(TransactionDTO::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expenses = rows.stream().filter(t -> "EXPENSE".equals(t.txType()))
                .map(TransactionDTO::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal net = income.subtract(expenses);
        summaryLabel.setText(String.format(
                "%d transaction(s)   |   Income: $%s   |   Expenses: $%s   |   Net: %s$%s",
                rows.size(),
                income.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                expenses.setScale(2, RoundingMode.HALF_UP).toPlainString(),
                net.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "",
                net.setScale(2, RoundingMode.HALF_UP).toPlainString()));
    }

    private Optional<TransactionDTO> showFormDialog(String title, TransactionDTO prefill) {
        Dialog<TransactionDTO> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.initOwner(getStage());
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/style.css").toExternalForm());

        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(12);
        grid.setPadding(new Insets(20, 28, 10, 28));
        grid.setMinWidth(420);

        TextField nameField = new TextField();
        nameField.setPromptText("e.g. Grocery shopping");
        nameField.setPrefWidth(240);
        nameField.getStyleClass().add("form-field");

        TextField amountField = new TextField();
        amountField.setPromptText("0.00");
        amountField.setPrefWidth(240);
        amountField.getStyleClass().add("form-field");

        ComboBox<String> categoryCombo = new ComboBox<>();
        categoryCombo.getItems().addAll(Categories.ALL);
        categoryCombo.setPromptText("Select category");
        categoryCombo.setPrefWidth(240);

        ToggleGroup typeGroup = new ToggleGroup();
        RadioButton expenseRb = new RadioButton("Expense");
        RadioButton incomeRb  = new RadioButton("Income");
        expenseRb.setToggleGroup(typeGroup);
        incomeRb.setToggleGroup(typeGroup);
        HBox typeBox = new HBox(20, expenseRb, incomeRb);

        DatePicker datePicker = new DatePicker(LocalDate.now());
        datePicker.setPrefWidth(240);

        grid.add(makeLabel("Name *"),     0, 0);  grid.add(nameField,     1, 0);
        grid.add(makeLabel("Amount *"),   0, 1);  grid.add(amountField,   1, 1);
        grid.add(makeLabel("Category *"), 0, 2);  grid.add(categoryCombo, 1, 2);
        grid.add(makeLabel("Type *"),     0, 3);  grid.add(typeBox,       1, 3);
        grid.add(makeLabel("Date *"),     0, 4);  grid.add(datePicker,    1, 4);
        dialog.getDialogPane().setContent(grid);

        if (prefill != null) {
            nameField.setText(prefill.name());
            amountField.setText(prefill.amount().toPlainString());
            categoryCombo.setValue(prefill.category());
            datePicker.setValue(prefill.txDate());
            if ("INCOME".equals(prefill.txType())) incomeRb.setSelected(true);
            else expenseRb.setSelected(true);
        } else {
            expenseRb.setSelected(true);
        }

        Node okNode = dialog.getDialogPane().lookupButton(saveBtn);
        okNode.setDisable(true);
        Runnable validate = () -> okNode.setDisable(
                nameField.getText().trim().isEmpty()
                || !isPositiveDecimal(amountField.getText().trim())
                || categoryCombo.getValue() == null
                || datePicker.getValue() == null);
        nameField.textProperty().addListener((o, a, b) -> validate.run());
        amountField.textProperty().addListener((o, a, b) -> validate.run());
        categoryCombo.valueProperty().addListener((o, a, b) -> validate.run());
        datePicker.valueProperty().addListener((o, a, b) -> validate.run());
        if (prefill != null) validate.run();

        dialog.setResultConverter(bt -> {
            if (bt != saveBtn) return null;
            try {
                long id = prefill != null ? prefill.transactionId() : 0L;
                return new TransactionDTO(id, nameField.getText().trim(),
                        new BigDecimal(amountField.getText().trim()),
                        categoryCombo.getValue(),
                        incomeRb.isSelected() ? "INCOME" : "EXPENSE",
                        datePicker.getValue());
            } catch (NumberFormatException e) { return null; }
        });
        return dialog.showAndWait().filter(dto -> dto != null);
    }

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

    private Stage getStage() {
        return (Stage) transactionTable.getScene().getWindow();
    }
}
