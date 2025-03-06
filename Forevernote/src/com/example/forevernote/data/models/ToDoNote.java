package com.example.forevernote.data.models;

public class ToDoNote extends Note {
	private static final long serialVersionUID = 1L;
	private String toDoDue = null;
	private String toDoCompleted = null;

	public ToDoNote(int id, String title, String content, String createdDate, String modifiedDate, 
			String toDoDue, String toDoDueCompleted) {
		super(id, title, content, createdDate, modifiedDate);
		this.toDoDue = toDoDue;
		this.toDoCompleted = toDoDueCompleted;
	}
	
	public ToDoNote(int id, String title, String content, String createdDate, String modifiedDate) {
		super(id, title, content, createdDate, modifiedDate);
	}

	public String getToDoDue() {
		return toDoDue;
	}

	public void setToDoDue(String toDoDue) {
		this.toDoDue = toDoDue;
	}

	public String getToDoCompleted() {
		return toDoCompleted;
	}

	public void setToDoCompleted(String toDoCompleted) {
		this.toDoCompleted = toDoCompleted;
	}
}
