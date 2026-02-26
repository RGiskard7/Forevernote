package com.example.forevernote.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.input.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import java.util.*;
import java.io.File;
import java.util.prefs.Preferences;
import java.util.logging.Logger;
import org.kordamp.ikonli.javafx.FontIcon;

import com.example.forevernote.config.LoggerConfig;
import com.example.forevernote.data.dao.interfaces.FolderDAO;
import com.example.forevernote.data.dao.interfaces.NoteDAO;
import com.example.forevernote.data.dao.interfaces.TagDAO;
import com.example.forevernote.data.models.Folder;
import com.example.forevernote.data.models.Note;
import com.example.forevernote.data.models.Tag;
import com.example.forevernote.data.models.interfaces.Component;
import com.example.forevernote.event.EventBus;
import com.example.forevernote.event.events.*;
import com.example.forevernote.service.FolderService;
import com.example.forevernote.service.NoteService;
import com.example.forevernote.service.TagService;

/**
 * Controller for the Sidebar view.
 * Restored literally from the original MainController codebase.
 * Selection handlers use events that MainController already listens for.
 */
public class SidebarController {

    private static final Logger logger = LoggerConfig.getLogger(SidebarController.class);

    @FXML
    private VBox sidebarPane;
    @FXML
    private TabPane navigationTabPane;

    // Folders
    @FXML
    private TreeView<Folder> folderTreeView;
    @FXML
    private TextField filterFoldersField;
    private TreeItem<Folder> vaultRootItem;
    private TreeItem<Folder> allNotesItem;
    private boolean folderSortAscending = true;

    // Tags
    @FXML
    private ListView<String> tagListView;
    @FXML
    private TextField filterTagsField;
    private final javafx.collections.ObservableList<String> masterTagsList = javafx.collections.FXCollections
            .observableArrayList();
    private boolean tagSortAscending = true;

    // Recent
    @FXML
    private ListView<String> recentNotesListView;
    @FXML
    private TextField filterRecentField;
    private final javafx.collections.ObservableList<String> masterRecentList = javafx.collections.FXCollections
            .observableArrayList();
    private boolean recentSortAscending = true;
    private List<Note> cachedRecentNotes = new ArrayList<>();

    // Favorites
    @FXML
    private ListView<String> favoritesListView;
    @FXML
    private TextField filterFavoritesField;
    private final javafx.collections.ObservableList<String> masterFavoritesList = javafx.collections.FXCollections
            .observableArrayList();
    private boolean favoritesSortAscending = true;
    private List<Note> cachedFavoriteNotes = new ArrayList<>();

    // Trash
    @FXML
    private TreeView<Component> trashTreeView;
    @FXML
    private TextField filterTrashField;
    private boolean trashSortAscending = true;

    // Services
    private NoteService noteService;
    private FolderService folderService;
    private TagService tagService;
    private FolderDAO folderDAO;
    private NoteDAO noteDAO;
    private TagDAO tagDAO;
    private EventBus eventBus;
    private ResourceBundle bundle;

    // Setters for MainController
    public void setEventBus(EventBus eb) {
        this.eventBus = eb;
    }

    public void setNoteService(NoteService ns) {
        this.noteService = ns;
    }

    public void setTagService(TagService ts) {
        this.tagService = ts;
    }

    public void setFolderService(FolderService fs) {
        this.folderService = fs;
    }

    public void setBundle(ResourceBundle b) {
        this.bundle = b;
    }

    public void setFolderDAO(FolderDAO fd) {
        this.folderDAO = fd;
    }

    public void setNoteDAO(NoteDAO nd) {
        this.noteDAO = nd;
    }

    public void setTagDAO(TagDAO td) {
        this.tagDAO = td;
    }

    @FXML
    public void initialize() {
        // Core initialization of tree structures (invisible roots)
        initializeFolderTree();
        initializeTrashTree();

        // Setup filter propagation
        setupFilteredList(tagListView, masterTagsList, filterTagsField);
        setupFilteredList(recentNotesListView, masterRecentList, filterRecentField);
        setupFilteredList(favoritesListView, masterFavoritesList, filterFavoritesField);

        // Selection listeners - Matching MainController patterns
        folderTreeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue() != null) {
                Folder f = newVal.getValue();
                if ("ALL_NOTES_VIRTUAL".equals(f.getId())) {
                    eventBus.publish(new FolderEvents.FolderSelectedEvent(f));
                } else if (!"INVISIBLE_ROOT".equals(f.getTitle())) {
                    eventBus.publish(new FolderEvents.FolderSelectedEvent(f));
                }
            }
        });

        tagListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                tagService.getAllTags().stream().filter(t -> t.getTitle().equals(newVal)).findFirst().ifPresent(t -> {
                    eventBus.publish(new TagEvents.TagSelectedEvent(t));
                });
            }
        });

        recentNotesListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                cachedRecentNotes.stream().filter(n -> n.getTitle().equals(newVal)).findFirst().ifPresent(n -> {
                    eventBus.publish(new NoteEvents.NoteOpenRequestEvent(n));
                });
            }
        });

        favoritesListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                cachedFavoriteNotes.stream().filter(n -> n.getTitle().equals(newVal)).findFirst().ifPresent(n -> {
                    eventBus.publish(new NoteEvents.NoteOpenRequestEvent(n));
                });
            }
        });

        trashTreeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue() != null) {
                eventBus.publish(new NoteEvents.TrashItemSelectedEvent(newVal.getValue()));
            }
        });

        filterFoldersField.textProperty().addListener((o, ov, nv) -> loadFolders());
        filterTrashField.textProperty().addListener((o, ov, nv) -> loadTrashTree());

        setupCellFactories();
        setupTrashContextMenu();
    }

    private void initializeFolderTree() {
        Folder invisibleRoot = new Folder("INVISIBLE_ROOT", null, null);
        TreeItem<Folder> rootContainer = new TreeItem<>(invisibleRoot);
        folderTreeView.setRoot(rootContainer);
        folderTreeView.setShowRoot(false);

        // All Notes Virtual Folder
        Folder allNotesFolder = new Folder(getString("app.all_notes"), null, null);
        allNotesFolder.setId("ALL_NOTES_VIRTUAL");
        allNotesItem = new TreeItem<>(allNotesFolder);
        folderTreeView.getRoot().getChildren().add(allNotesItem);

        // Vault Root Folder logic literal
        String vaultName = "My Vault";
        try {
            Preferences prefs = Preferences.userNodeForPackage(MainController.class);
            String path = prefs.get("filesystem_path", "");
            if (!path.isEmpty()) {
                File f = new File(path);
                if (f.exists())
                    vaultName = f.getName();
            } else if ("sqlite".equals(prefs.get("storage_type", "sqlite"))) {
                vaultName = getString("app.my_notes");
            }
        } catch (Exception e) {
        }

        Folder vaultFolder = new Folder(vaultName, null, null);
        vaultFolder.setId("ROOT");
        vaultRootItem = new TreeItem<>(vaultFolder);
        vaultRootItem.setExpanded(true);
        folderTreeView.getRoot().getChildren().add(vaultRootItem);
    }

    private void initializeTrashTree() {
        Folder trashRoot = new Folder("INVISIBLE_ROOT", null, null);
        TreeItem<Component> trashRootItem = new TreeItem<>(trashRoot);
        trashTreeView.setRoot(trashRootItem);
        trashTreeView.setShowRoot(false);
    }

    private void setupCellFactories() {
        folderTreeView.setCellFactory(tv -> new TreeCell<Folder>() {
            @Override
            protected void updateItem(Folder folder, boolean empty) {
                super.updateItem(folder, empty);
                if (empty || folder == null) {
                    setText(null);
                    setGraphic(null);
                    setContextMenu(null);
                } else {
                    Label iconLabel = new Label("");
                    iconLabel.getStyleClass().setAll("folder-cell-icon");
                    boolean isAllNotes = "ALL_NOTES_VIRTUAL".equals(folder.getId());
                    if (isAllNotes) {
                        iconLabel.setText("[=]");
                        iconLabel.getStyleClass().add("folder-all-notes");
                    } else {
                        TreeItem<Folder> ti = getTreeItem();
                        boolean isExp = ti != null && ti.isExpanded();
                        iconLabel.setText(isExp ? "[/]" : "[+]");
                        iconLabel.getStyleClass().add(isExp ? "folder-expanded" : "folder-collapsed");
                    }
                    int count = 0;
                    try {
                        if (isAllNotes)
                            count = noteService.getAllNotes().size();
                        else
                            count = getNoteCountForFolder(folder);
                    } catch (Exception e) {
                    }
                    HBox container = new HBox(6);
                    container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    Label nameLabel = new Label(folder.getTitle());
                    nameLabel.getStyleClass().add("folder-cell-name");
                    if (count > 0 || isAllNotes) {
                        Label countLabel = new Label("(" + count + ")");
                        countLabel.getStyleClass().add("folder-cell-count");
                        container.getChildren().addAll(iconLabel, nameLabel, countLabel);
                    } else {
                        container.getChildren().addAll(iconLabel, nameLabel);
                    }
                    setGraphic(container);
                    setText(null);
                    if (!isAllNotes)
                        setContextMenu(createFolderContextMenu(folder));
                    else
                        setContextMenu(null);
                }
            }
        });

        trashTreeView.setCellFactory(tv -> new TreeCell<Component>() {
            @Override
            protected void updateItem(Component item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox container = new HBox(6);
                    container.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                    if (item instanceof Folder) {
                        Label iconLabel = new Label("");
                        iconLabel.getStyleClass().setAll("folder-cell-icon");
                        TreeItem<Component> ti = getTreeItem();
                        boolean isExp = ti != null && ti.isExpanded();
                        iconLabel.setText(isExp ? "[/]" : "[+]");
                        String title = item.getTitle();
                        if (title != null && title.equals(".trash"))
                            title = getString("tab.trash");
                        else if (title != null) {
                            int idx = title.lastIndexOf('/');
                            if (idx != -1)
                                title = title.substring(idx + 1);
                        }
                        Label nameLabel = new Label(title);
                        nameLabel.getStyleClass().add("folder-cell-name");
                        container.getChildren().addAll(iconLabel, nameLabel);
                    } else {
                        FontIcon noteIcon = new FontIcon("fth-file-text");
                        noteIcon.getStyleClass().add("feather-icon");
                        Label nameLabel = new Label(item.getTitle());
                        nameLabel.getStyleClass().add("folder-cell-name");
                        container.getChildren().addAll(noteIcon, nameLabel);
                    }
                    setGraphic(container);
                    setText(null);
                }
            }
        });
    }

    private void setupTrashContextMenu() {
        ContextMenu trashMenu = new ContextMenu();
        MenuItem restoreItem = new MenuItem(getString("action.restore"));
        restoreItem.setOnAction(e -> handleRestoreTrashItem());
        MenuItem deleteItem = new MenuItem(getString("action.delete_permanently"));
        deleteItem.setOnAction(e -> handleDeleteTrashItem());
        trashMenu.getItems().addAll(restoreItem, deleteItem);
        trashTreeView.setContextMenu(trashMenu);
    }

    private void setupFilteredList(ListView<String> listView, javafx.collections.ObservableList<String> masterList,
            TextField filterField) {
        javafx.collections.transformation.FilteredList<String> filteredList = new javafx.collections.transformation.FilteredList<>(
                masterList, p -> true);
        listView.setItems(filteredList);
        if (filterField != null) {
            filterField.textProperty().addListener((obs, oldVal, newVal) -> {
                filteredList.setPredicate(item -> {
                    if (newVal == null || newVal.isEmpty())
                        return true;
                    return item.toLowerCase().contains(newVal.toLowerCase());
                });
            });
        }
    }

    public void loadFolders() {
        try {
            if (vaultRootItem == null)
                return;
            vaultRootItem.getChildren().clear();
            List<Folder> folders = folderService.getAllFolders();
            String filter = filterFoldersField.getText() == null ? ""
                    : filterFoldersField.getText().toLowerCase().trim();
            Set<String> visibleIds = new HashSet<>();
            if (!filter.isEmpty()) {
                for (Folder f : folders) {
                    if (f.getTitle().toLowerCase().contains(filter)) {
                        visibleIds.add(f.getId());
                        Folder parent = f;
                        int s = 0;
                        while (s++ < 100) {
                            Optional<Folder> op = folderService.getParentFolder(parent);
                            if (op.isEmpty() || "ROOT".equals(op.get().getId()))
                                break;
                            parent = op.get();
                            visibleIds.add(parent.getId());
                        }
                    }
                }
            }
            List<Folder> roots = new ArrayList<>();
            for (Folder f : folders) {
                if (!filter.isEmpty() && !visibleIds.contains(f.getId()))
                    continue;
                Optional<Folder> op = folderService.getParentFolder(f);
                if (op.isEmpty() || "ROOT".equals(op.get().getId()))
                    roots.add(f);
            }
            Comparator<Folder> comp = (f1, f2) -> f1.getTitle().compareToIgnoreCase(f2.getTitle());
            if (!folderSortAscending)
                comp = comp.reversed();
            Collections.sort(roots, comp);
            for (Folder f : roots) {
                TreeItem<Folder> item = new TreeItem<>(f);
                vaultRootItem.getChildren().add(item);
                loadSubFolders(item, f, visibleIds, comp, !filter.isEmpty());
            }
            if (!filter.isEmpty())
                expandCollapseRecursive(vaultRootItem, true);
            vaultRootItem.setExpanded(true);
        } catch (Exception e) {
            logger.severe("Failed to load folders: " + e.getMessage());
        }
    }

    private void loadSubFolders(TreeItem<Folder> parentItem, Folder parentFolder, Set<String> visibleIds,
            Comparator<Folder> comp, boolean active) {
        try {
            folderService.loadSubfolders(parentFolder, 1);
            List<Folder> children = new ArrayList<>();
            for (Component c : parentFolder.getChildren())
                if (c instanceof Folder)
                    if (!active || visibleIds.contains(c.getId()))
                        children.add((Folder) c);
            Collections.sort(children, comp);
            for (Folder f : children) {
                TreeItem<Folder> item = new TreeItem<>(f);
                parentItem.getChildren().add(item);
                loadSubFolders(item, f, visibleIds, comp, active);
            }
        } catch (Exception e) {
        }
    }

    public void loadTrashTree() {
        try {
            Folder trashRoot = folderService.getTrashFolders();
            List<Note> allNotes = noteService.getTrashNotes();
            Map<String, Folder> folderMap = new HashMap<>();
            mapTrashFolders(trashRoot, folderMap);
            List<Note> rootNotes = new ArrayList<>();
            for (Note n : allNotes) {
                String id = n.getId().replace("\\", "/");
                String pId = null;
                if (n.getParent() != null && n.getParent().getId() != null)
                    pId = n.getParent().getId();
                else {
                    int i = id.lastIndexOf('/');
                    if (i != -1)
                        pId = id.substring(0, i);
                }
                boolean added = false;
                if (pId != null) {
                    String norm = pId.replace("\\", "/");
                    Folder p = folderMap.get(norm);
                    if (p == null) {
                        if (norm.equals(".trash") || norm.equals("trash"))
                            p = trashRoot;
                        else if (norm.startsWith("trash/"))
                            p = folderMap.get("." + norm);
                        else if (!norm.startsWith(".trash/") && !norm.startsWith("."))
                            p = folderMap.get(".trash/" + norm);
                    }
                    if (p != null) {
                        p.add(n);
                        n.setParent(p);
                        added = true;
                    }
                }
                if (!added)
                    rootNotes.add(n);
            }
            for (Note rn : rootNotes) {
                trashRoot.add(rn);
                rn.setParent(trashRoot);
            }
            String filter = filterTrashField.getText() == null ? "" : filterTrashField.getText().toLowerCase().trim();
            Set<String> vIds = new HashSet<>();
            if (!filter.isEmpty())
                buildTrashVisibleIdsRec(trashRoot, filter, vIds);
            TreeItem<Component> rootItem = new TreeItem<>(trashRoot);
            buildTrashTreeRecursive(rootItem, vIds, !filter.isEmpty());
            if (!filter.isEmpty() && !rootItem.getChildren().isEmpty())
                expandCollapseRecursive(rootItem, true);
            trashTreeView.setRoot(rootItem);
            trashTreeView.setShowRoot(false);
        } catch (Exception e) {
            logger.severe("Failed to load trash: " + e.getMessage());
        }
    }

    private void mapTrashFolders(Folder f, Map<String, Folder> map) {
        if (f.getId() != null)
            map.put(f.getId(), f);
        for (Component c : f.getChildren())
            if (c instanceof Folder)
                mapTrashFolders((Folder) c, map);
    }

    private boolean buildTrashVisibleIdsRec(Component c, String filter, Set<String> vIds) {
        boolean vis = (c.getTitle() != null && c.getTitle().toLowerCase().contains(filter));
        if (c instanceof Folder) {
            for (Component child : ((Folder) c).getChildren())
                if (buildTrashVisibleIdsRec(child, filter, vIds))
                    vis = true;
        }
        if (vis)
            vIds.add(c.getId());
        return vis;
    }

    private void buildTrashTreeRecursive(TreeItem<Component> parentItem, Set<String> vIds, boolean filtering) {
        if (parentItem.getValue() instanceof Folder) {
            Folder f = (Folder) parentItem.getValue();
            List<Component> sorted = new ArrayList<>(f.getChildren());
            sorted.sort((c1, c2) -> {
                boolean f1 = c1 instanceof Folder;
                boolean f2 = c2 instanceof Folder;
                if (f1 && !f2)
                    return -1;
                if (!f1 && f2)
                    return 1;
                String t1 = c1.getTitle() == null ? "" : c1.getTitle().toLowerCase();
                String t2 = c2.getTitle() == null ? "" : c2.getTitle().toLowerCase();
                return t1.compareTo(t2);
            });
            for (Component c : sorted) {
                if (filtering && c.getId() != null && !vIds.contains(c.getId()))
                    continue;
                TreeItem<Component> item = new TreeItem<>(c);
                parentItem.getChildren().add(item);
                if (c instanceof Folder)
                    buildTrashTreeRecursive(item, vIds, filtering);
            }
        }
    }

    public void loadTags() {
        try {
            List<Tag> tags = tagService.getAllTags();
            masterTagsList.clear();
            for (Tag t : tags)
                masterTagsList.add(t.getTitle());
            tagListView.setCellFactory(lv -> new ListCell<String>() {
                @Override
                protected void updateItem(String item, boolean e) {
                    super.updateItem(item, e);
                    if (e || item == null) {
                        setText(null);
                        setContextMenu(null);
                    } else {
                        setText("# " + item);
                        setContextMenu(createTagContextMenu(item));
                    }
                }
            });
        } catch (Exception e) {
        }
    }

    public void loadRecentNotes() {
        try {
            cachedRecentNotes = noteService.getAllNotes();
            cachedRecentNotes.sort((a, b) -> {
                String d1 = a.getModifiedDate() != null ? a.getModifiedDate()
                        : (a.getCreatedDate() != null ? a.getCreatedDate() : "");
                String d2 = b.getModifiedDate() != null ? b.getModifiedDate()
                        : (b.getCreatedDate() != null ? b.getCreatedDate() : "");
                return recentSortAscending ? d2.compareTo(d1) : d1.compareTo(d2);
            });
            masterRecentList.clear();
            for (int i = 0; i < Math.min(10, cachedRecentNotes.size()); i++)
                masterRecentList.add(cachedRecentNotes.get(i).getTitle());
        } catch (Exception e) {
        }
    }

    public void loadFavorites() {
        try {
            cachedFavoriteNotes = noteService.getAllNotes().stream().filter(Note::isFavorite).toList();
            masterFavoritesList.clear();
            for (Note n : cachedFavoriteNotes)
                masterFavoritesList.add(n.getTitle());
        } catch (Exception e) {
        }
    }

    @FXML
    public void handleSortFolders(ActionEvent e) {
        folderSortAscending = !folderSortAscending;
        loadFolders();
    }

    @FXML
    public void handleExpandAllFolders(ActionEvent e) {
        expandCollapseRecursive(vaultRootItem, true);
    }

    @FXML
    public void handleCollapseAllFolders(ActionEvent e) {
        expandCollapseRecursive(vaultRootItem, false);
    }

    @FXML
    public void handleSortTags(ActionEvent e) {
        tagSortAscending = !tagSortAscending;
        loadTags();
    }

    @FXML
    public void handleSortRecent(ActionEvent e) {
        recentSortAscending = !recentSortAscending;
        loadRecentNotes();
    }

    @FXML
    public void handleSortFavorites(ActionEvent e) {
        favoritesSortAscending = !favoritesSortAscending;
        loadFavorites();
    }

    @FXML
    public void handleSortTrash(ActionEvent e) {
        trashSortAscending = !trashSortAscending;
        loadTrashTree();
    }

    @FXML
    public void handleEmptyTrash(ActionEvent e) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, getString("action.empty_trash") + "?", ButtonType.OK,
                ButtonType.CANCEL);
        alert.showAndWait().filter(r -> r == ButtonType.OK).ifPresent(r -> {
            try {
                for (Note n : noteService.getTrashNotes())
                    noteService.permanentlyDeleteNote(n.getId());
                Folder root = folderService.getTrashFolders();
                if (root != null)
                    for (Component c : root.getChildren())
                        if (c instanceof Folder)
                            folderService.permanentlyDeleteFolder(c.getId());
                loadTrashTree();
                publishStatusUpdate("Trash emptied");
            } catch (Exception ex) {
            }
        });
    }

    private void handleRestoreTrashItem() {
        TreeItem<Component> sel = trashTreeView.getSelectionModel().getSelectedItem();
        if (sel != null) {
            try {
                Component c = sel.getValue();
                if (c instanceof Folder)
                    folderService.restoreFolder(c.getId());
                else if (c instanceof Note)
                    noteService.restoreNote(c.getId());
                if (folderDAO != null)
                    folderDAO.refreshCache();
                loadFolders();
                loadTrashTree();
                publishStatusUpdate("Item restored");
            } catch (Exception e) {
            }
        }
    }

    private void handleDeleteTrashItem() {
        TreeItem<Component> sel = trashTreeView.getSelectionModel().getSelectedItem();
        if (sel != null) {
            Component c = sel.getValue();
            Alert a = new Alert(Alert.AlertType.CONFIRMATION,
                    getString("action.delete") + " " + c.getTitle() + " " + getString("app.permanently") + "?",
                    ButtonType.OK, ButtonType.CANCEL);
            if (a.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                try {
                    if (c instanceof Folder)
                        folderService.permanentlyDeleteFolder(c.getId());
                    else if (c instanceof Note)
                        noteService.permanentlyDeleteNote(c.getId());
                    loadTrashTree();
                    publishStatusUpdate("Item deleted");
                } catch (Exception e) {
                }
            }
        }
    }

    private ContextMenu createFolderContextMenu(Folder f) {
        ContextMenu m = new ContextMenu();
        MenuItem r = new MenuItem(getString("action.rename"));
        r.setOnAction(e -> handleRenameFolder(f));
        MenuItem d = new MenuItem(getString("action.delete"));
        d.setOnAction(e -> handleDeleteFolder(f));
        m.getItems().addAll(r, d);
        return m;
    }

    private void handleRenameFolder(Folder f) {
        TextInputDialog d = new TextInputDialog(f.getTitle());
        d.showAndWait().ifPresent(name -> {
            try {
                folderService.renameFolder(f, name);
                loadFolders();
            } catch (Exception e) {
            }
        });
    }

    private void handleDeleteFolder(Folder f) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, getString("action.delete") + "?", ButtonType.OK,
                ButtonType.CANCEL);
        a.showAndWait().filter(r -> r == ButtonType.OK).ifPresent(r -> {
            try {
                folderService.deleteFolder(f.getId());
                loadFolders();
            } catch (Exception e) {
            }
        });
    }

    private ContextMenu createTagContextMenu(String tagName) {
        ContextMenu m = new ContextMenu();
        MenuItem d = new MenuItem(getString("action.delete"));
        d.setOnAction(e -> handleDeleteTag(tagName));
        m.getItems().add(d);
        return m;
    }

    private void handleDeleteTag(String tagName) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, getString("action.delete") + " tag #" + tagName + "?",
                ButtonType.OK, ButtonType.CANCEL);
        a.showAndWait().filter(r -> r == ButtonType.OK).ifPresent(r -> {
            tagService.getAllTags().stream().filter(t -> t.getTitle().equals(tagName)).findFirst().ifPresent(t -> {
                try {
                    tagService.deleteTag(t.getId());
                    loadTags();
                } catch (Exception ex) {
                }
            });
        });
    }

    private void expandCollapseRecursive(TreeItem<?> item, boolean expand) {
        if (item != null) {
            item.setExpanded(expand);
            for (TreeItem<?> child : item.getChildren())
                expandCollapseRecursive(child, expand);
        }
    }

    private int getNoteCountForFolder(Folder f) {
        try {
            if (f == null)
                return 0;
            String id = f.getId();
            if (id == null || "ALL_NOTES_VIRTUAL".equals(id))
                return noteService.getAllNotes().size();
            Optional<Folder> op = folderService.getFolderById(id);
            return op.map(folder -> noteService.getNotesByFolder(folder).size()).orElse(0);
        } catch (Exception e) {
            return 0;
        }
    }

    private String getString(String k) {
        return bundle != null && bundle.containsKey(k) ? bundle.getString(k) : k;
    }

    private void publishStatusUpdate(String m) {
        if (eventBus != null)
            eventBus.publish(new UIEvents.StatusUpdateEvent(m));
    }

    public VBox getSidebarPane() {
        return sidebarPane;
    }

    public TabPane getNavigationTabPane() {
        return navigationTabPane;
    }

    public TreeView<Folder> getFolderTreeView() {
        return folderTreeView;
    }

    public TreeView<Component> getTrashTreeView() {
        return trashTreeView;
    }

    public TextField getFilterFoldersField() {
        return filterFoldersField;
    }

    public ListView<String> getTagListView() {
        return tagListView;
    }

    public TextField getFilterTrashField() {
        return filterTrashField;
    }
}
