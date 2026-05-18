package com.moneymanager.ui.controller;

import com.moneymanager.dto.DashboardSnapshot;
import com.moneymanager.dto.MonthlyTrend;
import com.moneymanager.model.User;
import com.moneymanager.repository.DataAccessException;
import com.moneymanager.service.DashboardService;
import com.moneymanager.service.MonthlyIncomeService;
import com.moneymanager.ui.util.AlertHelper;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Side;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class DashboardController {

    // ── FXML fields ─────────────────────────────────────────────────────────
    @FXML private Label monthLabel;
    @FXML private Label monthlyIncomeLabel;
    @FXML private Label incomeLabel;
    @FXML private Label expensesLabel;
    @FXML private Label netBalanceLabel;
    @FXML private Label goalSavingsLabel;
    @FXML private Label availableLabel;
    @FXML private PieChart categoryPieChart;
    @FXML private Label pieNoDataLabel;
    @FXML private BarChart<String, Number> monthlyBarChart;

    // ── State ────────────────────────────────────────────────────────────────
    private DashboardService dashboardService;
    private MonthlyIncomeService monthlyIncomeService;
    private User currentUser;

    /** Fired after monthly income is saved so other tabs (transactions, budgets) also refresh. */
    private Runnable onSettingChanged;

    /** Fired when the user clicks the Logout button. */
    private Runnable onLogout;

    private static final NumberFormat CF = NumberFormat.getCurrencyInstance(Locale.US);

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @FXML
    public void initialize() {
        monthlyBarChart.setAnimated(false);
        categoryPieChart.setAnimated(false);
        // Slice labels disabled — legend below the chart carries all the info
        categoryPieChart.setLabelsVisible(false);
        categoryPieChart.setLegendSide(Side.BOTTOM);
        categoryPieChart.setLegendVisible(true);
    }

    /** Called by MainController after FXML load. */
    public void init(DashboardService dashboardService,
                     MonthlyIncomeService monthlyIncomeService,
                     User user) {
        this.dashboardService      = dashboardService;
        this.monthlyIncomeService  = monthlyIncomeService;
        this.currentUser           = user;
        load();
    }

    /** Refresh all data (called after transactions change or on tab switch). */
    public void refresh() {
        if (dashboardService != null) load();
    }

    /**
     * Register a callback that is fired after the user saves a new monthly income.
     * MainController uses this to refresh the transactions list and budgets.
     */
    public void setOnSettingChanged(Runnable callback) {
        this.onSettingChanged = callback;
    }

    /** Register a callback for when the user logs out. */
    public void setOnLogout(Runnable callback) {
        this.onLogout = callback;
    }

    // ── FXML handlers ─────────────────────────────────────────────────────────

    @FXML
    private void handleSetMonthlyIncome() {
        Optional<BigDecimal> current =
                monthlyIncomeService.getMonthlyIncome(currentUser.getUserId());
        showIncomeDialog(current.orElse(null)).ifPresent(amount -> {
            try {
                monthlyIncomeService.setMonthlyIncome(currentUser.getUserId(), amount);
                monthlyIncomeService.applyForCurrentMonth(currentUser.getUserId());
                load(); // refresh this dashboard immediately
                if (onSettingChanged != null) onSettingChanged.run(); // refresh other tabs
            } catch (IllegalArgumentException e) {
                AlertHelper.showError(getStage(), "Validation Error", e.getMessage());
            } catch (DataAccessException e) {
                AlertHelper.showError(getStage(), "Error", "Could not save monthly income.");
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void handleLogout() {
        if (onLogout != null) {
            onLogout.run();
        }
    }

    // ── Data loading ──────────────────────────────────────────────────────────

    private void load() {
        try {
            DashboardSnapshot snap = dashboardService.getSnapshot(currentUser.getUserId());

            // Month heading
            String monthName = snap.month().getMonth()
                    .getDisplayName(TextStyle.FULL, Locale.ENGLISH);
            monthLabel.setText(monthName + " " + snap.month().getYear());

            // Monthly income setting label
            monthlyIncomeService.getMonthlyIncome(currentUser.getUserId())
                    .ifPresentOrElse(
                            amt -> monthlyIncomeLabel.setText("Monthly Income: " + CF.format(amt)),
                            () -> monthlyIncomeLabel.setText("Monthly Income: not set"));

            // KPI labels
            incomeLabel.setText(CF.format(snap.monthIncome()));
            expensesLabel.setText(CF.format(snap.monthExpenses()));
            goalSavingsLabel.setText(CF.format(snap.goalSavings()));
            applySignedLabel(netBalanceLabel, snap.netBalance());
            applySignedLabel(availableLabel,  snap.availableBalance());

            // Charts
            updatePieChart(snap.categoryBreakdown());
            updateBarChart(snap.monthlyTrend());

        } catch (DataAccessException e) {
            monthLabel.setText("Error loading dashboard");
            e.printStackTrace();
        }
    }

    private static void applySignedLabel(Label label, BigDecimal value) {
        boolean negative = value.compareTo(BigDecimal.ZERO) < 0;
        label.setText(CF.format(value.abs()) + (negative ? "  (deficit)" : ""));
        label.setStyle(negative
                ? "-fx-text-fill: #dc2626; -fx-font-size: 26px; -fx-font-weight: bold;"
                : "-fx-text-fill: #16a34a; -fx-font-size: 26px; -fx-font-weight: bold;");
    }

    // ── Pie chart ─────────────────────────────────────────────────────────────

    private void updatePieChart(Map<String, BigDecimal> breakdown) {
        if (breakdown.isEmpty()) {
            categoryPieChart.setVisible(false);
            categoryPieChart.setManaged(false);
            pieNoDataLabel.setVisible(true);
            pieNoDataLabel.setManaged(true);
            return;
        }
        pieNoDataLabel.setVisible(false);
        pieNoDataLabel.setManaged(false);
        categoryPieChart.setVisible(true);
        categoryPieChart.setManaged(true);

        ObservableList<PieChart.Data> slices = FXCollections.observableArrayList();
        for (var entry : breakdown.entrySet()) {
            // Legend label: single line "Food  ($120.00)" — no \n, no overlap
            String label = entry.getKey() + "  (" + CF.format(entry.getValue()) + ")";
            slices.add(new PieChart.Data(label, entry.getValue().doubleValue()));
        }
        categoryPieChart.setData(slices);

        // Install tooltips so users can hover each slice to see category + amount
        Platform.runLater(() -> {
            for (PieChart.Data d : categoryPieChart.getData()) {
                Tooltip tip = new Tooltip(d.getName());
                tip.setStyle("-fx-font-size: 13px;");
                Tooltip.install(d.getNode(), tip);
            }
            fixLegendLayout();
        });
    }

    /**
     * Space out the legend FlowPane that JavaFX places below the chart.
     * Must run after CSS and layout have been applied (called from Platform.runLater).
     */
    private void fixLegendLayout() {
        categoryPieChart.applyCss();
        categoryPieChart.layout();
        Node legendNode = categoryPieChart.lookup(".chart-legend");
        if (legendNode instanceof FlowPane fp) {
            fp.setHgap(16);
            fp.setVgap(8);
            fp.setStyle("-fx-padding: 8 4 4 4;");
        } else if (legendNode != null) {
            // Fallback: apply via inline style if not a FlowPane (shouldn't happen in JFX 21)
            legendNode.setStyle("-fx-padding: 8 4 4 4;");
        }
    }

    // ── Bar chart ─────────────────────────────────────────────────────────────

    private void updateBarChart(List<MonthlyTrend> trend) {
        XYChart.Series<String, Number> incomeSeries  = new XYChart.Series<>();
        XYChart.Series<String, Number> expenseSeries = new XYChart.Series<>();
        incomeSeries.setName("Income");
        expenseSeries.setName("Expenses");

        for (MonthlyTrend t : trend) {
            incomeSeries.getData().add(new XYChart.Data<>(t.label(), t.income().doubleValue()));
            expenseSeries.getData().add(new XYChart.Data<>(t.label(), t.expenses().doubleValue()));
        }
        monthlyBarChart.getData().clear();
        monthlyBarChart.getData().addAll(incomeSeries, expenseSeries);
    }

    // ── Dialog ────────────────────────────────────────────────────────────────

    private Optional<BigDecimal> showIncomeDialog(BigDecimal current) {
        Dialog<BigDecimal> dialog = new Dialog<>();
        dialog.setTitle("Monthly Income Setting");
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

        TextField amtField = new TextField(current != null ? current.toPlainString() : "");
        amtField.setPromptText("e.g. 3000.00");
        amtField.setPrefWidth(220);
        amtField.getStyleClass().add("form-field");

        Label hint = new Label(
                "Each month, this amount will automatically be added as\n" +
                "an INCOME transaction and set as the Monthly Budget cap\n" +
                "(if neither is already recorded for that month).");
        hint.setStyle("-fx-text-fill: #64748b; -fx-font-size: 12px;");

        Label lbl = new Label("Monthly Amount *");
        lbl.getStyleClass().add("field-label");
        grid.add(lbl,      0, 0);  grid.add(amtField, 1, 0);
        grid.add(new Label(), 0, 1);  grid.add(hint,     1, 1);
        dialog.getDialogPane().setContent(grid);

        Node okNode = dialog.getDialogPane().lookupButton(saveBtn);
        okNode.setDisable(!isPositive(amtField.getText().trim()));
        amtField.textProperty().addListener((o, a, b) ->
                okNode.setDisable(!isPositive(b.trim())));

        dialog.setResultConverter(bt -> {
            if (bt != saveBtn) return null;
            try { return new BigDecimal(amtField.getText().trim()); }
            catch (NumberFormatException e) { return null; }
        });
        return dialog.showAndWait().filter(d -> d != null);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static boolean isPositive(String s) {
        if (s == null || s.isBlank()) return false;
        try { return new BigDecimal(s).compareTo(BigDecimal.ZERO) > 0; }
        catch (NumberFormatException e) { return false; }
    }

    private Stage getStage() {
        return (Stage) monthLabel.getScene().getWindow();
    }
}
