package com.example.forevernote.data.models;

import java.io.Serializable;
import java.util.*;

public class Note implements Serializable {    
	private static final long serialVersionUID = 1L;
	private int id;
	private String title;
	private String content;
	private Notebook notebook;
	private List<Tag> tags; //Set
	private String creationDate;
	private String updateDate;
	
    public Note(int id, String title, String content, Notebook notebook, List<Tag> tags, String creationDate, String updateDate) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.notebook = notebook;
        this.creationDate = creationDate;
        this.tags = new ArrayList<>(tags); //HashSet
        this.updateDate = updateDate;
    }
    
    public Note(int id, String title, String content, Notebook notebook, String creationDate, String updateDate) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.notebook = notebook;
        this.creationDate = creationDate;
        this.tags = new ArrayList<>();
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

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
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
    
    @Override
    public int hashCode() {
        return Objects.hash(title);
    }
}
