package com.example.forevernote.data.models.abstractLayers;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.example.forevernote.data.models.interfaces.Component;

public abstract class CompositeModel extends BaseModel implements Component {
	private Component parent = null;
	private Set<Component> children = new HashSet<>();

	public CompositeModel(Integer id, String title, String createdDate, String modifiedDate) {
		super(id, title, createdDate, modifiedDate);
	}
	
    public CompositeModel(String title, String createdDate, String modifiedDate) {
        super(title, createdDate, modifiedDate);
    }
	
    @Override
	public Component getParent() {
		return parent;
	}
	
	@Override
    public void setParent(Component parent) {
    	this.parent = parent;
    }
	
    @Override
	public void add(Component component) {
    	if (component != null) {
    		children.add(component);
    	}
	}
    
	@Override
	public void addAll(List<Component> components) {
		if (components != null && !components.isEmpty()) {
			children.addAll(components);
		}
	}
	
	@Override
	public void setChildren(List<Component> components) {
		if (components != null) {
			this.children = new HashSet<>(components);
		}
	}

	@Override
	public void remove(Component component) {
		if (component != null) {
			children.remove(component);
		}
	}

	@Override
	public List<Component> getChildren() {
		return new ArrayList<>(children);
	}
	
	@Override
	public String getPath() {
	    Component parentFolder = getParent();
	    if (parentFolder == null) {
	        return "/" + getTitle();
	    } else {
	        return parentFolder.getPath() + "/" + getTitle();
	    }
	}

}
