package com.example.forevernote.data.models;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Note implements Serializable {    
	private static final long serialVersionUID = 1L;
	private int id;
	private String title;
	private String content;
	private Notebook notebook;
	private Set<Tag> tags;
	private String creationDate;
	private String updateDate;
	
    public Note(int id, String title, String content, Notebook notebook, List<Tag> tags, String creationDate) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.notebook = notebook;
        this.creationDate = creationDate;
        this.tags = new HashSet<>(tags);
        updateDate = null;
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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

    public Notebook getNotebook() {
        return notebook;
    }

    public void setNotebook(Notebook notebook) {
        this.notebook = notebook;
    }

    public Set<Tag> getTags() {
        return tags;
    }

    public void setTags(Set<Tag> tags) {
        this.tags = tags;
    }

    @Override
    public String toString() {
        /*return "Note{" +
                "title='" + title + '\'' +
                '}';*/
    	return "Note{" +
    	"id='" + id + '\'' +
        "title='" + title + '\'' +
        "content='" + content + '\'' +
        '}';
    }
}
