package com.example.forevernote.data.models.abstractLayers;

import java.util.List;

import com.example.forevernote.data.models.interfaces.Component;

public abstract class LeafModel extends BaseModel implements Component {
	private Component parent = null;

	public LeafModel(int id, String title, String createdDate, String modifiedDate) {
		super(id, title, createdDate, modifiedDate);
	}
	
	public LeafModel(String title, String createdDate, String modifiedDate) {
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
		throw new UnsupportedOperationException();

	}
	
	@Override
	public void addAll(List<Component> components) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void setChildren(List<Component> components) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void remove(Component component) {
		throw new UnsupportedOperationException();

	}

	@Override
	public List<Component> getChildren() {
		throw new UnsupportedOperationException();
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
