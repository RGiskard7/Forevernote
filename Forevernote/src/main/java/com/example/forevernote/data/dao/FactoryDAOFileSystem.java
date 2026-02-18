package com.example.forevernote.data.dao;

import com.example.forevernote.data.dao.abstractLayers.FactoryDAO;
import com.example.forevernote.data.dao.interfaces.FolderDAO;
import com.example.forevernote.data.dao.interfaces.NoteDAO;
import com.example.forevernote.data.dao.interfaces.TagDAO;
import com.example.forevernote.data.file.FolderDAOFileSystem;
import com.example.forevernote.data.file.NoteDAOFileSystem;
import com.example.forevernote.data.file.TagDAOFileSystem;

public class FactoryDAOFileSystem extends FactoryDAO {

    private NoteDAOFileSystem noteDAO;
    private FolderDAOFileSystem folderDAO;
    private TagDAOFileSystem tagDAO;

    public FactoryDAOFileSystem(String rootDirectory) {
        // Ideally we should cache instances to share caches
        this.noteDAO = new NoteDAOFileSystem(rootDirectory);
        this.folderDAO = new FolderDAOFileSystem(rootDirectory);
        this.tagDAO = new TagDAOFileSystem(this.noteDAO);
    }

    @Override
    public NoteDAO getNoteDAO() {
        return noteDAO;
    }

    @Override
    public FolderDAO getFolderDAO() {
        return folderDAO;
    }

    @Override
    public TagDAO getLabelDAO() {
        return tagDAO;
    }
}
