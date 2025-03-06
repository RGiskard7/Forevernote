package com.example.forevernote.data.models;

import java.io.Serializable;

import com.example.forevernote.data.models.abstractLayers.CompositeModel;

public class Folder extends CompositeModel implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public Folder(Integer id, String title) {
    	super(id, title, null, null);
    }
    
    public Folder(String title) {
    	super(title, null, null);
    }

    public Folder(Integer id, String title, String createdDate, String modifiedDate) {
    	super(id, title, createdDate, modifiedDate);
    }
    
    public Folder(String title, String createdDate, String modifiedDate) {
    	super(title, createdDate, modifiedDate);
    }

    public Boolean isEmpty() {
        return getChildren().isEmpty();
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Folder folder = (Folder) o;
        return getTitle().equals(folder.getTitle());
    }

    @Override
    public String toString() {
        return "Folder{" +
                "id=" + getId() +
                ", title='" + getTitle() + '\'' +
                ", children=" + getChildren() + "}";
    }
}
