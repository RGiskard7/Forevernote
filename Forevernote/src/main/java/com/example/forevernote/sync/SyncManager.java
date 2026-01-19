package com.example.forevernote.sync;

import com.example.forevernote.config.LoggerConfig;
import com.example.forevernote.data.models.Note;
import com.example.forevernote.data.models.Folder;
import com.example.forevernote.data.models.Tag;
import com.example.forevernote.sync.SyncService.SyncStatus;
import com.example.forevernote.sync.SyncService.SyncResult;
import com.example.forevernote.sync.SyncService.SyncStatusListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.logging.Logger;

/**
 * Manager for synchronization operations.
 * 
 * <p>Provides a centralized point for managing sync services, scheduling
 * automatic syncs, and handling sync events.</p>
 * 
 * <p>Features:</p>
 * <ul>
 *   <li>Plugin-based sync providers</li>
 *   <li>Automatic background sync</li>
 *   <li>Sync queue for batching operations</li>
 *   <li>Conflict detection and resolution UI hooks</li>
 * </ul>
 * 
 * @author Edu Díaz (RGiskard7)
 * @since 1.4.0
 */
public class SyncManager {
    
    private static final Logger logger = LoggerConfig.getLogger(SyncManager.class);
    
    private static SyncManager instance;
    
    private SyncService currentService;
    private SyncConfig config;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> autoSyncTask;
    private final List<SyncStatusListener> listeners = new ArrayList<>();
    private SyncStatus lastStatus = SyncStatus.DISCONNECTED;
    
    // Sync queue for batching changes
    private final BlockingQueue<SyncItem> syncQueue = new LinkedBlockingQueue<>();
    private ExecutorService syncExecutor;
    private volatile boolean processingQueue = false;
    
    /**
     * Item in the sync queue.
     */
    private record SyncItem(
        String type,    // "note", "folder", "tag"
        Object item,
        String operation // "create", "update", "delete"
    ) {}
    
    private SyncManager() {
        // Private constructor for singleton
    }
    
    /**
     * Get the singleton instance.
     * 
     * @return SyncManager instance
     */
    public static synchronized SyncManager getInstance() {
        if (instance == null) {
            instance = new SyncManager();
        }
        return instance;
    }
    
    // ==================== Initialization ====================
    
    /**
     * Initialize the sync manager with configuration.
     * 
     * @param config Sync configuration
     * @return CompletableFuture that completes when initialized
     */
    public CompletableFuture<Boolean> initialize(SyncConfig config) {
        this.config = config;
        
        // If no provider configured, return immediately
        if (config.getProvider() == SyncConfig.Provider.NONE) {
            logger.info("Sync disabled (no provider configured)");
            return CompletableFuture.completedFuture(true);
        }
        
        // Create the appropriate sync service based on provider
        currentService = createSyncService(config.getProvider());
        if (currentService == null) {
            logger.warning("No sync service available for provider: " + config.getProvider());
            return CompletableFuture.completedFuture(false);
        }
        
        // Initialize the service
        return currentService.initialize(config)
            .thenApply(success -> {
                if (success) {
                    setupAutoSync();
                    setupSyncQueueProcessor();
                    
                    // Sync on startup if configured
                    if (config.isSyncOnStartup()) {
                        syncAll();
                    }
                    
                    logger.info("Sync manager initialized with provider: " + config.getProvider());
                }
                return success;
            });
    }
    
    /**
     * Create a sync service for the given provider.
     * 
     * @param provider The sync provider
     * @return SyncService instance or null
     */
    private SyncService createSyncService(SyncConfig.Provider provider) {
        // TODO: Implement provider-specific services
        // For now, return null (sync not implemented)
        // Future implementations:
        // case DROPBOX: return new DropboxSyncService();
        // case GOOGLE_DRIVE: return new GoogleDriveSyncService();
        // case WEBDAV: return new WebDAVSyncService();
        // case LOCAL_NETWORK: return new LocalNetworkSyncService();
        // case CUSTOM: return loadCustomProvider();
        
        logger.info("Sync service creation requested for: " + provider + " (not yet implemented)");
        return null;
    }
    
    /**
     * Setup automatic sync scheduler.
     */
    private void setupAutoSync() {
        if (!config.isAutoSync()) {
            return;
        }
        
        if (scheduler == null) {
            scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "SyncScheduler");
                t.setDaemon(true);
                return t;
            });
        }
        
        // Cancel existing task if any
        if (autoSyncTask != null) {
            autoSyncTask.cancel(false);
        }
        
        // Schedule periodic sync
        int interval = config.getAutoSyncIntervalMinutes();
        autoSyncTask = scheduler.scheduleAtFixedRate(
            () -> {
                if (currentService != null && currentService.getStatus() == SyncStatus.IDLE) {
                    syncChanges();
                }
            },
            interval,
            interval,
            TimeUnit.MINUTES
        );
        
        logger.info("Auto-sync scheduled every " + interval + " minutes");
    }
    
    /**
     * Setup the sync queue processor for batching sync operations.
     */
    private void setupSyncQueueProcessor() {
        if (syncExecutor == null) {
            syncExecutor = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "SyncQueueProcessor");
                t.setDaemon(true);
                return t;
            });
        }
        
        // Start queue processor
        syncExecutor.submit(this::processSyncQueue);
    }
    
    /**
     * Process items in the sync queue.
     */
    private void processSyncQueue() {
        processingQueue = true;
        
        while (processingQueue) {
            try {
                // Wait for items with a timeout
                SyncItem item = syncQueue.poll(5, TimeUnit.SECONDS);
                
                if (item != null && currentService != null) {
                    // Process the item
                    switch (item.type()) {
                        case "note" -> currentService.syncNote((Note) item.item());
                        case "folder" -> currentService.syncFolder((Folder) item.item());
                        case "tag" -> currentService.syncTag((Tag) item.item());
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                logger.warning("Error processing sync queue: " + e.getMessage());
            }
        }
    }
    
    // ==================== Sync Operations ====================
    
    /**
     * Perform a full sync.
     * 
     * @return CompletableFuture with sync result
     */
    public CompletableFuture<SyncResult> syncAll() {
        if (currentService == null) {
            return CompletableFuture.completedFuture(
                new SyncResult(false, 0, 0, 0, List.of(), "No sync service configured")
            );
        }
        
        notifyStatusChange(SyncStatus.SYNCING, "Starting full sync...");
        
        return currentService.syncAll()
            .thenApply(result -> {
                if (result.success()) {
                    notifyStatusChange(SyncStatus.SYNCED, "Sync completed successfully");
                } else {
                    notifyStatusChange(SyncStatus.ERROR, result.errorMessage());
                }
                return result;
            });
    }
    
    /**
     * Sync only changes since last sync.
     * 
     * @return CompletableFuture with sync result
     */
    public CompletableFuture<SyncResult> syncChanges() {
        if (currentService == null) {
            return CompletableFuture.completedFuture(
                new SyncResult(false, 0, 0, 0, List.of(), "No sync service configured")
            );
        }
        
        notifyStatusChange(SyncStatus.SYNCING, "Syncing changes...");
        
        return currentService.syncChanges()
            .thenApply(result -> {
                if (result.success()) {
                    notifyStatusChange(SyncStatus.SYNCED, "Changes synced");
                } else {
                    notifyStatusChange(SyncStatus.ERROR, result.errorMessage());
                }
                return result;
            });
    }
    
    /**
     * Queue a note for sync (used for sync-on-change).
     * 
     * @param note The note to sync
     * @param operation The operation type
     */
    public void queueNoteSync(Note note, String operation) {
        if (config != null && config.isSyncOnChange()) {
            syncQueue.offer(new SyncItem("note", note, operation));
        }
    }
    
    /**
     * Queue a folder for sync.
     * 
     * @param folder The folder to sync
     * @param operation The operation type
     */
    public void queueFolderSync(Folder folder, String operation) {
        if (config != null && config.isSyncOnChange()) {
            syncQueue.offer(new SyncItem("folder", folder, operation));
        }
    }
    
    /**
     * Queue a tag for sync.
     * 
     * @param tag The tag to sync
     * @param operation The operation type
     */
    public void queueTagSync(Tag tag, String operation) {
        if (config != null && config.isSyncOnChange()) {
            syncQueue.offer(new SyncItem("tag", tag, operation));
        }
    }
    
    // ==================== Status Management ====================
    
    /**
     * Get the current sync status.
     * 
     * @return Current status
     */
    public SyncStatus getStatus() {
        return currentService != null ? currentService.getStatus() : SyncStatus.DISCONNECTED;
    }
    
    /**
     * Check if sync is configured and available.
     * 
     * @return true if sync is available
     */
    public boolean isSyncAvailable() {
        return currentService != null && currentService.isConfigured();
    }
    
    /**
     * Add a status change listener.
     * 
     * @param listener The listener to add
     */
    public void addStatusListener(SyncStatusListener listener) {
        listeners.add(listener);
    }
    
    /**
     * Remove a status change listener.
     * 
     * @param listener The listener to remove
     */
    public void removeStatusListener(SyncStatusListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Notify listeners of a status change.
     */
    private void notifyStatusChange(SyncStatus newStatus, String message) {
        SyncStatus oldStatus = lastStatus;
        lastStatus = newStatus;
        
        for (SyncStatusListener listener : listeners) {
            try {
                listener.onStatusChanged(oldStatus, newStatus, message);
            } catch (Exception e) {
                logger.warning("Error notifying sync listener: " + e.getMessage());
            }
        }
    }
    
    // ==================== Shutdown ====================
    
    /**
     * Shutdown the sync manager.
     */
    public void shutdown() {
        processingQueue = false;
        
        if (autoSyncTask != null) {
            autoSyncTask.cancel(true);
        }
        
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        
        if (syncExecutor != null) {
            syncExecutor.shutdownNow();
        }
        
        if (currentService != null) {
            currentService.disconnect();
        }
        
        logger.info("Sync manager shutdown");
    }
}
