package com.moneymanager.ui.controller;

import com.moneymanager.model.Note;
import com.moneymanager.model.User;
import com.moneymanager.repository.DataAccessException;
import com.moneymanager.service.NoteService;
import com.moneymanager.ui.util.AlertHelper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class NoteController {

    @FXML private Button deleteButton;
    @FXML private Label noteCountLabel;
    @FXML private ListView<Note> noteListView;

    @FXML private Label detailPlaceholder;
    @FXML private VBox  detailContent;
    @FXML private Label noteTitleLabel;
    @FXML private Label noteCreatedLabel;
    @FXML private TextArea noteContentArea;

    private NoteService noteService;
    private User currentUser;

    private static final DateTimeFormatter DT_FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");

    @FXML
    public void initialize() {
        noteListView.setCellFactory(lv -> new NoteCell());
        noteListView.setPlaceholder(
                new Label("No notes yet. Click '+ New Note' to create one."));

        deleteButton.disableProperty().bind(
                noteListView.getSelectionModel().selectedItemProperty().isNull());

        noteListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, selected) -> {
                    if (selected != null) showDetail(selected);
                    else clearDetail();
                });
    }

    public void init(NoteService noteService, User user) {
        this.noteService  = noteService;
        this.currentUser  = user;
        loadNotes(0);
    }

    @FXML
    private void handleAdd() {
        showAddDialog().ifPresent(note -> {
            try {
                Note saved = noteService.addNote(
                        currentUser.getUserId(), note.getTitle(), note.getContent());
                java.util.logging.Logger.getLogger("com.moneymanager")
                        .info("user=" + currentUser.getUsername()
                              + " action=note_created details=title="
                              + saved.getTitle());
                loadNotes(saved.getNoteId());
            } catch (IllegalArgumentException e) {
                AlertHelper.showError(getStage(), "Validation Error", e.getMessage());
            } catch (DataAccessException e) {
                AlertHelper.showError(getStage(), "Error", "Could not save note.");
                e.printStackTrace();
            }
        });
    }

    @FXML
    private void handleDelete() {
        Note selected = noteListView.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        if (!AlertHelper.showConfirm(getStage(), "Delete Note",
                "Delete \"" + selected.getTitle() + "\"?\nThis cannot be undone.")) return;

        try {
            noteService.deleteNote(selected.getNoteId());
            java.util.logging.Logger.getLogger("com.moneymanager")
                    .info("user=" + currentUser.getUsername()
                          + " action=note_deleted details=title="
                          + selected.getTitle());
            loadNotes(0);
            clearDetail();
        } catch (DataAccessException e) {
            AlertHelper.showError(getStage(), "Error", "Could not delete note.");
            e.printStackTrace();
        }
    }

    private void loadNotes(long selectId) {
        if (noteService == null) return;

        Note previousSelection = (selectId == 0)
                ? noteListView.getSelectionModel().getSelectedItem()
                : null;

        List<Note> notes = noteService.getNotes(currentUser.getUserId());
        noteListView.setItems(FXCollections.observableArrayList(notes));

        int count = notes.size();
        noteCountLabel.setText(count + " note" + (count == 1 ? "" : "s"));

        if (selectId > 0) {
            notes.stream()
                    .filter(n -> n.getNoteId() == selectId)
                    .findFirst()
                    .ifPresent(n -> noteListView.getSelectionModel().select(n));
        } else if (previousSelection != null) {
            notes.stream()
                    .filter(n -> n.getNoteId() == previousSelection.getNoteId())
                    .findFirst()
                    .ifPresent(n -> noteListView.getSelectionModel().select(n));
        } else if (!notes.isEmpty()) {
            noteListView.getSelectionModel().selectFirst();
        }
    }

    private void showDetail(Note note) {
        detailPlaceholder.setVisible(false);
        detailPlaceholder.setManaged(false);
        detailContent.setVisible(true);
        detailContent.setManaged(true);

        noteTitleLabel.setText(note.getTitle());
        noteCreatedLabel.setText(note.getCreatedAt() != null
                ? "Created " + note.getCreatedAt().format(DT_FMT)
                : "");
        noteContentArea.setText(note.getContent() != null ? note.getContent() : "");
    }

    private void clearDetail() {
        detailContent.setVisible(false);
        detailContent.setManaged(false);
        detailPlaceholder.setVisible(true);
        detailPlaceholder.setManaged(true);
        noteContentArea.clear();
    }

    private Stage getStage() {
        return (Stage) noteListView.getScene().getWindow();
    }

    private Optional<Note> showAddDialog() {
        Dialog<Note> dialog = new Dialog<>();
        dialog.setTitle("New Note");
        dialog.setHeaderText(null);
        dialog.initOwner(getStage());
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/css/style.css").toExternalForm());

        ButtonType saveBtn = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveBtn, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(12);
        grid.setPadding(new Insets(20, 28, 10, 28));
        grid.setMinWidth(460);

        TextField titleField = new TextField();
        titleField.setPromptText("Note title");
        titleField.setPrefWidth(300);
        titleField.getStyleClass().add("form-field");

        TextArea contentArea = new TextArea();
        contentArea.setPromptText("Write your note here…");
        contentArea.setPrefRowCount(8);
        contentArea.setPrefWidth(300);
        contentArea.setWrapText(true);
        contentArea.getStyleClass().add("form-field");

        Label titleLbl = new Label("Title *");
        titleLbl.getStyleClass().add("field-label");
        Label contentLbl = new Label("Content");
        contentLbl.getStyleClass().add("field-label");

        grid.add(titleLbl,   0, 0);  grid.add(titleField,   1, 0);
        grid.add(contentLbl, 0, 1);  grid.add(contentArea,  1, 1);
        dialog.getDialogPane().setContent(grid);

        Node okNode = dialog.getDialogPane().lookupButton(saveBtn);
        okNode.setDisable(true);
        titleField.textProperty().addListener((o, a, b) ->
                okNode.setDisable(b.trim().isEmpty()));

        dialog.setResultConverter(bt -> {
            if (bt != saveBtn) return null;
            var n = new Note();
            n.setTitle(titleField.getText().trim());
            n.setContent(contentArea.getText().trim());
            return n;
        });

        dialog.setOnShown(e -> titleField.requestFocus());

        return dialog.showAndWait().filter(n -> n != null);
    }

    private static final class NoteCell extends ListCell<Note> {

        private static final DateTimeFormatter DATE_FMT =
                DateTimeFormatter.ofPattern("MMM d, yyyy");

        @Override
        protected void updateItem(Note item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) { setGraphic(null); setText(null); return; }

            Label titleLabel = new Label(item.getTitle());
            titleLabel.getStyleClass().add("note-cell-title");
            titleLabel.setMaxWidth(Double.MAX_VALUE);
            titleLabel.setWrapText(false);

            String date = item.getCreatedAt() != null
                    ? item.getCreatedAt().format(DATE_FMT) : "";
            Label dateLabel = new Label(date);
            dateLabel.getStyleClass().add("note-cell-date");

            String preview = "";
            if (item.getContent() != null && !item.getContent().isBlank()) {
                preview = item.getContent().lines()
                        .filter(l -> !l.isBlank())
                        .findFirst().orElse("");
                if (preview.length() > 60) preview = preview.substring(0, 57) + "…";
            }
            Label previewLabel = new Label(preview);
            previewLabel.getStyleClass().add("note-cell-preview");
            previewLabel.setMaxWidth(Double.MAX_VALUE);

            var box = new javafx.scene.layout.VBox(2, titleLabel, dateLabel, previewLabel);
            box.setPadding(new Insets(8, 12, 8, 12));

            setGraphic(box);
            setText(null);
        }
    }
}
