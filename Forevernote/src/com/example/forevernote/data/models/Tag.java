package com.example.forevernote.data.models;

import java.io.Serializable;

import com.example.forevernote.data.models.abstractLayers.BaseModel;

public class Tag extends BaseModel implements Serializable {
    private static final long serialVersionUID = 1L;
    
    public Tag(Integer id, String title) {
    	super(id, title, null, null);
    }
    
    public Tag(String title) {
    	super(title, null, null);
    }
    
    public Tag(String title, String createdDate, String modifiedDate) {
    	super(title, createdDate, modifiedDate);
    }

    public Tag(Integer id, String title, String createdDate, String modifiedDate) {
    	super(id, title, createdDate, modifiedDate);
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Tag tag = (Tag) o;
        return getTitle().equals(tag.getTitle());
    }
    
    @Override
    public String toString() {
        return "Tag{" +
                "id=" + getId() +
                ", title='" + getTitle() + '\'' +
                '}';
    }
}
