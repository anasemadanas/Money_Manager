package com.moneymanager.service;

import com.moneymanager.model.Note;
import com.moneymanager.repository.INoteRepo;

import java.util.List;

public class NoteService {

    private final INoteRepo noteRepo;

    public NoteService(INoteRepo noteRepo) {
        this.noteRepo = noteRepo;
    }

    public List<Note> getNotes(long userId) {
        return noteRepo.findByUser(userId);
    }

    public Note addNote(long userId, String title, String content) {
        if (title == null || title.isBlank())
            throw new IllegalArgumentException("Title is required.");
        var note = new Note();
        note.setUserId(userId);
        note.setTitle(title.trim());
        note.setContent(content != null ? content.trim() : "");
        return noteRepo.save(note);
    }

    public void deleteNote(long noteId, long userId) {
        noteRepo.delete(noteId, userId);
    }
}
