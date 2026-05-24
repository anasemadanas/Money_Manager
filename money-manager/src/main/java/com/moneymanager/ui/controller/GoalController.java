package com.moneymanager.ui.controller;

import com.moneymanager.dto.ContributionDTO;
import com.moneymanager.dto.GoalDTO;
import com.moneymanager.model.User;
import com.moneymanager.repository.DataAccessException;
import com.moneymanager.service.GoalService;
import com.moneymanager.ui.util.AlertHelper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class GoalController {

    @FXML private Button editGoalButton;
    @FXML private Button deleteGoalButton;
    @FXML private ListView<GoalDTO> goalListView;
    @FXML private Label contributionTitleLabel;
    @FXML private TableView<ContributionDTO> contributionTable;
    @FXML private TableColumn<ContributionDTO, String> contribDateCol;
    @FXML private TableColumn<ContributionDTO, String> contribAmountCol;
    @FXML private TableColumn<ContributionDTO, String> contribNoteCol;
    @FXML private Label summaryLabel;

    private GoalService goalService;
    private User currentUser;

    private static final DateTimeFormatter CONTRIB_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy  HH:mm");
    private static final NumberFormat CF = NumberFormat.getCurrencyInstance(Locale.US);

    @FXML
    public void initialize() {
        contribDateCol.setCellValueFactory(c -> {
            OffsetDateTime ts = c.getValue().contributedAt();
            return new SimpleStringProperty(ts != null ? ts.format(CONTRIB_FMT) : "");
        });
        contribAmountCol.setCellValueFactory(c ->
                new SimpleStringProperty(CF.format(c.getValue().amount())));
        contribNoteCol.setCellValueFactory(c -> {
            String note = c.getValue().note();
            return new SimpleStringProperty(note != null ? note : "");
        });
        contributionTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        contributionTable.setPlaceholder(
                new Label("No contributions yet for this goal."));

        goalListView.setCellFactory(lv -> new GoalCell());
        goalListView.setPlaceholder(
                new Label("No goals yet. Click '+ New Goal' to create one."));

        editGoalButton.disableProperty().bind(
                goalListView.getSelectionModel().selectedItemProperty().isNull());
        deleteGoalButton.disableProperty().bind(
                goalListView.getSelectionModel().selectedItemProperty().isNull());

        goalListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, selected) -> {
                    if (selected != null) {
                        loadContributions(selected);
                    } else {
                        contributionTable.getItems().clear();
                        contributionTitleLabel.setText(
                                "Select a goal to view contribution history");
                    }
                });
    }

    public void init(GoalService goalService, User user) {
        this.goalService = goalService;
        this.currentUser = user;
        loadGoals();
    }

    @FXML
    private void handleNewGoal() {
        showGoalDialog("New Savings Goal", null).ifPresent(dto -> {
            try {
                goalService.addGoal(currentUser.getUserId(),
                        dto.name(), dto.targetAmount(), dto.deadline());
                java.util.logging.Logger.getLogger("com.moneymanager")
                        .info("user=" + currentUser.getUsername()
                              + " action=goal_created details=name="
                              + dto.name()
                              + ", target_amount="
                              + dto.targetAmount().setScale(2, java.math.RoundingMode.HALF_UP));
                loadGoals();
            } catch (IllegalArgumentException e) {
                AlertHelper.showError(getStage(), "Validation Error", e.getMessage());
            } catch (DataAccessException e) {
                AlertHelper.showError(getStage(), "Error", "Could not save goal.");
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void handleEditGoal() {
        GoalDTO selected = goalListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        showGoalDialog("Edit Goal", selected).ifPresent(dto -> {
            try {
                goalService.updateGoal(selected.goalId(),
                        dto.name(), dto.targetAmount(), dto.deadline());
                java.util.logging.Logger.getLogger("com.moneymanager")
                        .info("user=" + currentUser.getUsername()
                              + " action=goal_updated details=name="
                              + dto.name()
                              + ", target_amount="
                              + dto.targetAmount().setScale(2, java.math.RoundingMode.HALF_UP));
                loadGoals();
            } catch (IllegalArgumentException e) {
                AlertHelper.showError(getStage(), "Validation Error", e.getMessage());
            } catch (DataAccessException e) {
                AlertHelper.showError(getStage(), "Error", "Could not update goal.");
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void handleDeleteGoal() {
        GoalDTO selected = goalListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (!AlertHelper.showConfirm(getStage(), "Delete Goal",
                "Delete goal \"" + selected.name() + "\" and all its contributions?\n" +
                "This cannot be undone.")) return;

        try {
            goalService.deleteGoal(selected.goalId());
            java.util.logging.Logger.getLogger("com.moneymanager")
                    .info("user=" + currentUser.getUsername()
                          + " action=goal_deleted details=name="
                          + selected.name()
                          + ", target_amount="
                          + selected.targetAmount().setScale(2, java.math.RoundingMode.HALF_UP));
            loadGoals();
        } catch (DataAccessException e) {
            AlertHelper.showError(getStage(), "Error", "Could not delete goal.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddContribution() {
        List<GoalDTO> goals = goalService.getGoals(currentUser.getUserId());
        if (goals.isEmpty()) {
            AlertHelper.showInfo(getStage(), "No Goals",
                    "Create a savings goal first before adding a contribution.");
            return;
        }
        GoalDTO preSelected = goalListView.getSelectionModel().getSelectedItem();
        showContributionDialog(goals, preSelected).ifPresent(result -> {
            try {
                goalService.addContribution(result.goalId(), result.amount(), result.note());
                GoalDTO matchedGoal = goals.stream()
                        .filter(g -> g.goalId() == result.goalId())
                        .findFirst()
                        .orElse(null);
                String goalName = (matchedGoal != null) ? matchedGoal.name() : "Unknown";
                java.util.logging.Logger.getLogger("com.moneymanager")
                        .info("user=" + currentUser.getUsername()
                              + " action=goal_contribution_added details=amount="
                              + result.amount().setScale(2, java.math.RoundingMode.HALF_UP)
                              + ", goal=" + goalName);
                loadGoals();
                GoalDTO nowSelected = goalListView.getSelectionModel().getSelectedItem();
                if (nowSelected != null && nowSelected.goalId() == result.goalId()) {
                    loadContributions(goalListView.getSelectionModel().getSelectedItem());
                }
            } catch (IllegalArgumentException e) {
                AlertHelper.showError(getStage(), "Validation Error", e.getMessage());
            } catch (DataAccessException e) {
                AlertHelper.showError(getStage(), "Error", "Could not save contribution.");
                e.printStackTrace();
            }
        });
    }

    private void loadGoals() {
        if (goalService == null) return;

        GoalDTO previousSelection = goalListView.getSelectionModel().getSelectedItem();
        List<GoalDTO> goals = goalService.getGoals(currentUser.getUserId());
        goalListView.setItems(FXCollections.observableArrayList(goals));
        updateSummary(goals);

        if (previousSelection != null) {
            goals.stream()
                    .filter(g -> g.goalId() == previousSelection.goalId())
                    .findFirst()
                    .ifPresentOrElse(
                            g -> goalListView.getSelectionModel().select(g),
                            () -> {
                                contributionTable.getItems().clear();
                                contributionTitleLabel.setText(
                                        "Select a goal to view contribution history");
                            });
        }
    }

    private void loadContributions(GoalDTO goal) {
        contributionTitleLabel.setText("Contributions — " + goal.name());
        List<ContributionDTO> list = goalService.getContributions(goal.goalId());
        contributionTable.setItems(FXCollections.observableArrayList(list));
    }

    private void updateSummary(List<GoalDTO> goals) {
        if (goals.isEmpty()) {
            summaryLabel.setText("No savings goals yet.");
            return;
        }
        BigDecimal totalTarget = goals.stream().map(GoalDTO::targetAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSaved = goals.stream().map(GoalDTO::savedAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long completed = goals.stream()
                .filter(g -> g.savedAmount().compareTo(g.targetAmount()) >= 0).count();
        summaryLabel.setText(String.format(
                "%d goal(s)   |   Total target: %s   |   Total saved: %s   |   %d completed",
                goals.size(), CF.format(totalTarget), CF.format(totalSaved), completed));
    }

    private Optional<GoalDTO> showGoalDialog(String title, GoalDTO prefill) {
        Dialog<GoalDTO> dialog = new Dialog<>();
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
        nameField.setPromptText("e.g. New Laptop");
        nameField.setPrefWidth(240);
        nameField.getStyleClass().add("form-field");

        TextField targetField = new TextField();
        targetField.setPromptText("0.00");
        targetField.setPrefWidth(240);
        targetField.getStyleClass().add("form-field");

        DatePicker deadlinePicker = new DatePicker();
        deadlinePicker.setPromptText("Optional");
        deadlinePicker.setPrefWidth(240);

        CheckBox noDeadlineCb = new CheckBox("No deadline");
        noDeadlineCb.setSelected(prefill == null || prefill.deadline() == null);
        deadlinePicker.setDisable(noDeadlineCb.isSelected());
        noDeadlineCb.selectedProperty().addListener((obs, o, checked) -> {
            deadlinePicker.setDisable(checked);
            if (checked) deadlinePicker.setValue(null);
        });

        grid.add(makeLabel("Goal Name *"),  0, 0);  grid.add(nameField,     1, 0);
        grid.add(makeLabel("Target ($) *"), 0, 1);  grid.add(targetField,   1, 1);
        grid.add(makeLabel("Deadline"),     0, 2);  grid.add(deadlinePicker, 1, 2);
        grid.add(new Label(),               0, 3);  grid.add(noDeadlineCb,  1, 3);
        dialog.getDialogPane().setContent(grid);

        if (prefill != null) {
            nameField.setText(prefill.name());
            targetField.setText(prefill.targetAmount().toPlainString());
            if (prefill.deadline() != null) {
                deadlinePicker.setValue(prefill.deadline());
                noDeadlineCb.setSelected(false);
                deadlinePicker.setDisable(false);
            }
        }

        Node okNode = dialog.getDialogPane().lookupButton(saveBtn);
        okNode.setDisable(true);
        Runnable validate = () -> okNode.setDisable(
                nameField.getText().trim().isEmpty()
                || !isPositiveDecimal(targetField.getText().trim()));
        nameField.textProperty().addListener((o, a, b) -> validate.run());
        targetField.textProperty().addListener((o, a, b) -> validate.run());
        if (prefill != null) validate.run();

        dialog.setResultConverter(bt -> {
            if (bt != saveBtn) return null;
            try {
                long id = prefill != null ? prefill.goalId() : 0L;
                LocalDate deadline = noDeadlineCb.isSelected() ? null : deadlinePicker.getValue();
                return new GoalDTO(id, nameField.getText().trim(),
                        new BigDecimal(targetField.getText().trim()),
                        BigDecimal.ZERO, deadline);
            } catch (NumberFormatException e) { return null; }
        });
        return dialog.showAndWait().filter(d -> d != null);
    }

    private Optional<ContributionDTO> showContributionDialog(List<GoalDTO> goals,
                                                              GoalDTO preSelected) {
        Dialog<ContributionDTO> dialog = new Dialog<>();
        dialog.setTitle("Add Contribution");
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

        ComboBox<GoalDTO> goalCombo = new ComboBox<>();
        goalCombo.getItems().addAll(goals);
        goalCombo.setConverter(new StringConverter<>() {
            @Override public String toString(GoalDTO g) {
                if (g == null) return "";
                return g.name() + " (" + CF.format(g.savedAmount()) + " / " +
                       CF.format(g.targetAmount()) + ")";
            }
            @Override public GoalDTO fromString(String s) { return null; }
        });
        goalCombo.setPrefWidth(280);
        if (preSelected != null) goalCombo.setValue(preSelected);

        TextField amountField = new TextField();
        amountField.setPromptText("0.00");
        amountField.setPrefWidth(280);
        amountField.getStyleClass().add("form-field");

        TextField memoField = new TextField();
        memoField.setPromptText("Optional note");
        memoField.setPrefWidth(280);
        memoField.getStyleClass().add("form-field");

        grid.add(makeLabel("Goal *"),   0, 0);  grid.add(goalCombo,   1, 0);
        grid.add(makeLabel("Amount *"), 0, 1);  grid.add(amountField, 1, 1);
        grid.add(makeLabel("Memo"),     0, 2);  grid.add(memoField,   1, 2);
        dialog.getDialogPane().setContent(grid);

        Node okNode = dialog.getDialogPane().lookupButton(saveBtn);
        okNode.setDisable(true);
        Runnable validate = () -> okNode.setDisable(
                goalCombo.getValue() == null
                || !isPositiveDecimal(amountField.getText().trim()));
        goalCombo.valueProperty().addListener((o, a, b) -> validate.run());
        amountField.textProperty().addListener((o, a, b) -> validate.run());
        if (preSelected != null) validate.run();

        dialog.setResultConverter(bt -> {
            if (bt != saveBtn) return null;
            try {
                GoalDTO goal = goalCombo.getValue();
                BigDecimal amt = new BigDecimal(amountField.getText().trim());
                String memo = memoField.getText().trim();
                return new ContributionDTO(0, goal.goalId(), amt,
                        memo.isEmpty() ? null : memo, null);
            } catch (NumberFormatException e) { return null; }
        });
        return dialog.showAndWait().filter(d -> d != null);
    }

    private Stage getStage() { return (Stage) goalListView.getScene().getWindow(); }

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

    private static final class GoalCell extends ListCell<GoalDTO> {

        private static final DateTimeFormatter DEADLINE_FMT =
                DateTimeFormatter.ofPattern("MMM d, yyyy");

        @Override
        protected void updateItem(GoalDTO item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) { setGraphic(null); setText(null); return; }

            double pct = item.targetAmount().compareTo(BigDecimal.ZERO) == 0 ? 0.0
                    : item.savedAmount()
                           .divide(item.targetAmount(), 4, RoundingMode.HALF_UP)
                           .multiply(BigDecimal.valueOf(100)).doubleValue();
            double progress = Math.min(pct / 100.0, 1.0);
            String color = determineColor(item, pct);

            Label nameLabel = new Label(item.name());
            nameLabel.getStyleClass().add("goal-name");

            Label deadlineLabel = new Label(formatDeadline(item.deadline()));
            deadlineLabel.getStyleClass().add("goal-deadline");
            if ("red".equals(color))   deadlineLabel.getStyleClass().add("goal-deadline-red");
            else if ("amber".equals(color)) deadlineLabel.getStyleClass().add("goal-deadline-amber");

            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox nameRow = new HBox(nameLabel, spacer, deadlineLabel);
            nameRow.setAlignment(Pos.CENTER_LEFT);

            ProgressBar bar = new ProgressBar(progress);
            bar.setMaxWidth(Double.MAX_VALUE);
            bar.setPrefHeight(12);
            if ("red".equals(color))        bar.getStyleClass().add("red");
            else if ("amber".equals(color)) bar.getStyleClass().add("amber");

            Label pctLabel = new Label(String.format("%.0f%%  —  %s / %s",
                    pct, CF.format(item.savedAmount()), CF.format(item.targetAmount())));
            pctLabel.getStyleClass().add("goal-pct");
            if ("red".equals(color))        pctLabel.getStyleClass().add("pct-red");
            else if ("amber".equals(color)) pctLabel.getStyleClass().add("pct-amber");
            else                            pctLabel.getStyleClass().add("pct-green");

            VBox card = new VBox(6, nameRow, bar, pctLabel);
            card.setPadding(new Insets(14, 16, 14, 16));
            card.getStyleClass().add("goal-card");

            setGraphic(card);
            setText(null);
        }

        private static String determineColor(GoalDTO goal, double pct) {
            if (pct >= 100) return "green";

            if (goal.deadline() == null) return "green";

            LocalDate today = LocalDate.now();
            if (goal.deadline().isBefore(today)) return "red";

            long daysLeft = ChronoUnit.DAYS.between(today, goal.deadline());
            if (daysLeft <= 30 && pct < 80) return "amber";

            return "green";
        }

        private static String formatDeadline(LocalDate deadline) {
            if (deadline == null) return "No deadline";
            LocalDate today = LocalDate.now();
            if (deadline.isBefore(today))
                return "Overdue (" + deadline.format(DEADLINE_FMT) + ")";
            long days = ChronoUnit.DAYS.between(today, deadline);
            if (days == 0) return "Due today!";
            if (days == 1) return "1 day left";
            if (days <= 30) return days + " days left";
            return "By " + deadline.format(DEADLINE_FMT);
        }

        private static final NumberFormat CF = NumberFormat.getCurrencyInstance(Locale.US);
    }
}
