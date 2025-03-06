package com.example.forevernote.data.models;

import java.io.Serializable;
import java.util.*;

import com.example.forevernote.data.models.abstractLayers.LeafModel;

public class Note extends LeafModel implements Serializable {    
	private static final long serialVersionUID = 1L;
	private String content;
	//private List<Tag> tags = new ArrayList<>();
	
	private Set<Tag> tags = new HashSet<>();
	
	private Integer latitude = 0;
	private Integer longitude = 0;
	private String author = null;
	private String sourceUrl = null;
	private String source = null;
	private String sourceApplication = null;
	
    public Note(String title, String content) {
    	super(title, null, null);
        this.content = content;
    }
    
    public Note(Integer id, String title, String content) {
    	super(id, title, null, null);
        this.content = content;
    }
	
    public Note(String title, String content, String createdDate, String modifiedDate) {
    	super(title, createdDate, modifiedDate);
        this.content = content;
    }
    
    
    public Note(Integer id, String title, String content, String createdDate, String modifiedDate) {
    	super(id, title, createdDate, modifiedDate);
        this.content = content;
    }
    
    public Note(String title, String content, String createdDate, String modifiedDate, Integer latitude, Integer logitude,
    		String author, String source_url, String source, String source_application) {
    	super(title, createdDate, modifiedDate);
    	
        this.content = content;
        this.latitude = latitude;
        this.longitude = logitude;
        this.author = author;
        this.sourceUrl = source_url;
        this.source = source;
        this.sourceApplication = source_application;
    }
    
    public Note(Integer id, String title, String content, String createdDate, String modifiedDate, Integer latitude, Integer logitude,
    		String author, String source_url, String source, String source_application) {
    	this(title, content, createdDate, modifiedDate, latitude, logitude, author, source_url, source, source_application);
    	setId(id);
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    /*public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }*/
    

	public void addTag(Tag tag) {
    	if (tag != null) {
    		tags.add(tag);
    	}
	}

	public void addAllTags(List<Tag> tags) {
		if (tags != null && !tags.isEmpty()) {
			this.tags.addAll(tags);
		}
	}

	public void setTags(List<Tag> tags) {
		if (tags != null) {
			this.tags = new HashSet<>(tags);
		}
	}

	public void removeTag(Tag tag) {
		if (tag != null) {
			tags.remove(tag);
		}
	}
	
    public List<Tag> getTags() {
        return new ArrayList<>(tags);
    }
        
	public Integer getLatitude() {
		return latitude;
	}

	public void setLatitude(Integer latitude) {
		this.latitude = latitude;
	}

	public Integer getLongitude() {
		return longitude;
	}

	public void setLongitude(Integer longitude) {
		this.longitude = longitude;
	}

	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getSourceUrl() {
		return sourceUrl;
	}

	public void setSourceUrl(String sourceUrl) {
		this.sourceUrl = sourceUrl;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getSourceApplication() {
		return sourceApplication;
	}

	public void setSourceApplication(String sourceApplication) {
		this.sourceApplication = sourceApplication;
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
    	return "Note{"
    			+ "id='" + getId() + '\'' +
			        "title='" + getTitle() + '\'' +
			        "content='" + content + '\'' +
		        '}';
    }
}
