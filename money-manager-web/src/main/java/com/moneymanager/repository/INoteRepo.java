package com.moneymanager.repository;

import com.moneymanager.model.Note;

import java.util.List;

public interface INoteRepo {

    /** Persist a new note and return it with the generated ID and created_at filled. */
    Note save(Note note);

    /** Return all notes for a user, newest first. */
    List<Note> findByUser(long userId);

    /** Delete a note by its primary key. */
    void delete(long noteId);
}
