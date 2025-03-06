package com.example.forevernote.data.models.interfaces;

import java.util.List;

public interface Component { 
	
	public Integer getId();
	
	public void setId(Integer id);

	public String getTitle();
	
	public void setTitle(String title);
	
	public Component getParent();
	
    public void setParent(Component parent);
	
	public void add(Component component);
	
	public void addAll(List<Component> components);
	
	public void setChildren(List<Component> components);
    
    public void remove(Component component);
    
    public List<Component> getChildren();
    
    public String getPath();
}
