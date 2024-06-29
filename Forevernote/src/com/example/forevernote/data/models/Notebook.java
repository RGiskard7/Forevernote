package com.example.forevernote.data.models;

import java.io.Serializable;
import java.util.*;

public class Notebook extends BaseModel implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<Note> notes;

    public Notebook(int id, String title, List<Note> notes, String createdDate, String modifiedDate) {
    	super(id, title, createdDate, modifiedDate);
        this.notes = new ArrayList<Note>(notes);
    }

    public Notebook(int id, String title, String createdDate, String modifiedDate) {
    	super(id, title, createdDate, modifiedDate);
        notes = new ArrayList<Note>();
    }

    public List<Note> getNotes() {
        return notes;
    }

    public void setNotes(List<Note> notes) {
        this.notes = notes;
    }
    
    public void addNote(Note note) {
    	if (note == null) {
    		return;
    	}
    	notes.add(note);
    }

    public Boolean isEmpty() {
        if (notes.isEmpty()) {
            return true;
        }
        return false;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Notebook notebook = (Notebook) o;
        return getTitle().equals(notebook.getTitle());
    }

    @Override
    public String toString() {
        return "Notebook{" +
                "id=" + getId() +
                ", title='" + getTitle() + '\'' +
                '}';
    }
}
