package com.moneymanager.repository;

import com.moneymanager.model.Note;

import java.util.List;

public interface INoteRepo {

    Note save(Note note);

    List<Note> findByUser(long userId);

    void delete(long noteId, long userId);
}
