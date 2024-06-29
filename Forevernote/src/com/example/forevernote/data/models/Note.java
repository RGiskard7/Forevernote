package com.example.forevernote.data.models;

import java.io.Serializable;
import java.util.*;

public class Note extends BaseModel implements Serializable {    
	private static final long serialVersionUID = 1L;
	private String content;
	private Notebook notebook;
	private List<Tag> tags; //Set

    public Note(int id, String title, String content, Notebook notebook, List<Tag> tags, String createdDate, String modifiedDate) {
        super(id, title, createdDate, modifiedDate);
        
        this.content = content;
        this.notebook = notebook;
        this.tags = new ArrayList<>(tags); //HashSet
    }
    
    public Note(int id, String title, String content, Notebook notebook, String createdDate, String modifiedDate) {
    	super(id, title, createdDate, modifiedDate);
    	
        this.content = content;
        this.notebook = notebook;
        this.tags = new ArrayList<>();
    }
    
    public Note(int id, String title, String content, List<Tag> tags, String createdDate, String modifiedDate) {
    	super(id, title, createdDate, modifiedDate);
    	
        this.content = content;
        notebook = null;
        this.tags = new ArrayList<>(tags);
    }
    
    public Note(int id, String title, String content, String createdDate, String modifiedDate) {
    	super(id, title, createdDate, modifiedDate);
    	
        this.content = content;
        notebook = null;
        this.tags = new ArrayList<>();
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Notebook getNotebook() {
        return notebook;
    }

    public void setNotebook(Notebook notebook) {
        this.notebook = notebook;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Note note = (Note) o;
        return getTitle().equals(note.getTitle());
    }

    @Override
    public String toString() {
        /*return "Note{" +
                "title='" + title + '\'' +
                '}';*/
    	return "Note{" +
    	"id='" + getId() + '\'' +
        "title='" + getTitle() + '\'' +
        "content='" + content + '\'' +
        '}';
    }
}
