package com.example.forevernote.data.models;

import java.io.Serializable;
import java.util.*;

public class Tag implements Serializable {
    private static final long serialVersionUID = 1L;
	private int id;
    private String title;
    private List<Note> notes;
    private String creationDate;
    private String updateDate;

    public Tag(int id, String title, String creationDate, String updateDate) {
        this.id = id;
        this.title = title;
        notes = new ArrayList<Note>();
        this.creationDate = creationDate;
        this.updateDate = updateDate;
    }

    public Tag(int id, String title, List<Note> notes, String creationDate, String updateDate) {
        this.id = id;
        this.title = title;
        this.notes = new ArrayList<Note>(notes);
        this.creationDate = creationDate;
        this.updateDate = updateDate;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public List<Note> getNotes() {
        return notes;
    }

    public void setNotes(List<Note> notes) {
        this.notes = notes;
    }

    public String getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(String creationDate) {
        this.creationDate = creationDate;
    }
    
    public String getUpdateDate() {
        return updateDate;
    }

    public void setUpdateDate(String updateDate) {
        this.updateDate = updateDate;
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
        Tag label = (Tag) o;
        return title.equals(label.title);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title);
    }
}
