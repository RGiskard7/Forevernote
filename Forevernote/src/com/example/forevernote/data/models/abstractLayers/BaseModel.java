package com.example.forevernote.data.models.abstractLayers;

import java.util.Objects;

public abstract class BaseModel {
	private Integer id;
    private String title;
    private String createdDate;
    private String modifiedDate;
      
	public BaseModel(Integer id, String title, String createdDate, String modifiedDate) {
		this.id = id;
		this.title = title;
		this.createdDate = createdDate;
		this.modifiedDate = modifiedDate;
	}
	
	public BaseModel(String title, String createdDate, String modifiedDate) {
		this(null, title, createdDate, modifiedDate);
	}

	public Integer getId() {
		return id;
	}
	public void setId(Integer id) {
		this.id = id;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getCreatedDate() {
		return createdDate;
	}
	public void setCreatedDate(String createdDate) {
		this.createdDate = createdDate;
	}
	public String getModifiedDate() {
		return modifiedDate;
	}
	public void setModifiedDate(String modifiedDate) {
		this.modifiedDate = modifiedDate;
	}
	
    @Override
	public boolean equals(Object obj) { // Revisar
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BaseModel other = (BaseModel) obj;
		return Objects.equals(createdDate, other.createdDate) && Objects.equals(id, other.id)
				&& Objects.equals(modifiedDate, other.modifiedDate) && Objects.equals(title, other.title);
	}

	@Override
	public int hashCode() {
		return Objects.hash(title);
	}
    
    @Override
    public String toString() {
        return "Model{" +
                "id=" + id +
                ", title='" + title + '\'' +
                '}';
    }
}
