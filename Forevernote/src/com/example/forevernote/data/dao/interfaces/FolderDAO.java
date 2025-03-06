package com.example.forevernote.data.dao.interfaces;

import java.util.List;

import com.example.forevernote.data.models.Note;
import com.example.forevernote.data.models.Folder;

/**
 * This interface provides methods to interact with notebooks in a data access layer.
 */
public interface FolderDAO {

	public int createFolder(Folder folder);
	
	public void updateFolder(Folder folder);
	
	public void deleteFolder(int id);
	
	public Folder getFolderById(int id);
	
	public Folder getFolderByNoteId(int noteId);
	
	public List<Folder> fetchAllFoldersAsList();
	
	public Folder fetchAllFoldersAsTree();
	
	//public void addNote(int folderId, int noteId);
	
	public void addNote(Folder folder, Note note);
	
	//public void removeNote(int folderId, int noteId);
	
	public void removeNote(Folder folder, Note note);
	
	public void loadNotes(Folder folder);
	
	//public void addSubFolder(int parentId, int subFolderId);
	
	public void addSubFolder(Folder parent, Folder subFolder);
	
	//public void removeSubFolder(int parentId, int subFolderId);
	
	public void removeSubFolder(Folder parentFolder, Folder subFolder);
	
	public void loadSubFolders(Folder folder); 
	
	public void loadSubFolders(Folder folder, int maxDepth);
	
	public void loadParentFolders(Folder folder, int maxDepth); 
	
	public void loadParentFolders(Folder folder);
	
	public void loadParentFolder(Folder folder);
	
	public Folder getParentFolder(int folderId);
	
	public Folder getParentFolder(Folder folder);
	
	public String getPathFolder(int idFolder);
	
	public boolean existsByTitle(String title);

}