package com.example.forevernote.sync;

import com.example.forevernote.data.models.Note;
import com.example.forevernote.data.models.Folder;
import com.example.forevernote.data.models.Tag;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for synchronization services.
 * 
 * <p>This interface defines the contract for sync providers (cloud services,
 * local network sync, etc.). Implementations can be swapped at runtime.</p>
 * 
 * <p>Synchronization is designed to be:</p>
 * <ul>
 *   <li>Asynchronous - all operations return CompletableFuture</li>
 *   <li>Conflict-aware - provides mechanisms for conflict resolution</li>
 *   <li>Incremental - supports delta sync for efficiency</li>
 *   <li>Pluggable - different providers can be implemented</li>
 * </ul>
 * 
 * @author Edu Díaz (RGiskard7)
 * @since 1.4.0
 */
public interface SyncService {
    
    /**
     * Synchronization status.
     */
    enum SyncStatus {
        /** Not connected to any sync service */
        DISCONNECTED,
        /** Connected and idle */
        IDLE,
        /** Currently synchronizing */
        SYNCING,
        /** Sync completed successfully */
        SYNCED,
        /** Sync error occurred */
        ERROR,
        /** Conflicts need resolution */
        CONFLICT
    }
    
    /**
     * Sync conflict information.
     */
    record SyncConflict(
        String itemId,
        String itemType,
        String localVersion,
        String remoteVersion,
        long localTimestamp,
        long remoteTimestamp
    ) {}
    
    /**
     * Sync result containing details about the sync operation.
     */
    record SyncResult(
        boolean success,
        int itemsPushed,
        int itemsPulled,
        int conflictsFound,
        List<SyncConflict> conflicts,
        String errorMessage
    ) {}
    
    // ==================== Connection Management ====================
    
    /**
     * Initialize the sync service with configuration.
     * 
     * @param config Sync configuration
     * @return Future that completes when initialization is done
     */
    CompletableFuture<Boolean> initialize(SyncConfig config);
    
    /**
     * Check if the sync service is configured and ready.
     * 
     * @return true if ready to sync
     */
    boolean isConfigured();
    
    /**
     * Get the current sync status.
     * 
     * @return Current status
     */
    SyncStatus getStatus();
    
    /**
     * Disconnect from the sync service.
     * 
     * @return Future that completes when disconnected
     */
    CompletableFuture<Void> disconnect();
    
    // ==================== Full Sync ====================
    
    /**
     * Perform a full bidirectional sync.
     * 
     * @return Future with sync result
     */
    CompletableFuture<SyncResult> syncAll();
    
    /**
     * Pull all remote changes to local.
     * 
     * @return Future with sync result
     */
    CompletableFuture<SyncResult> pullAll();
    
    /**
     * Push all local changes to remote.
     * 
     * @return Future with sync result
     */
    CompletableFuture<SyncResult> pushAll();
    
    // ==================== Incremental Sync ====================
    
    /**
     * Sync changes since the last sync.
     * 
     * @return Future with sync result
     */
    CompletableFuture<SyncResult> syncChanges();
    
    /**
     * Sync a specific note.
     * 
     * @param note The note to sync
     * @return Future with success status
     */
    CompletableFuture<Boolean> syncNote(Note note);
    
    /**
     * Sync a specific folder.
     * 
     * @param folder The folder to sync
     * @return Future with success status
     */
    CompletableFuture<Boolean> syncFolder(Folder folder);
    
    /**
     * Sync a specific tag.
     * 
     * @param tag The tag to sync
     * @return Future with success status
     */
    CompletableFuture<Boolean> syncTag(Tag tag);
    
    // ==================== Conflict Resolution ====================
    
    /**
     * Get unresolved conflicts.
     * 
     * @return List of conflicts
     */
    List<SyncConflict> getConflicts();
    
    /**
     * Resolve a conflict by keeping the local version.
     * 
     * @param conflict The conflict to resolve
     * @return Future with success status
     */
    CompletableFuture<Boolean> resolveKeepLocal(SyncConflict conflict);
    
    /**
     * Resolve a conflict by keeping the remote version.
     * 
     * @param conflict The conflict to resolve
     * @return Future with success status
     */
    CompletableFuture<Boolean> resolveKeepRemote(SyncConflict conflict);
    
    /**
     * Resolve a conflict by merging both versions.
     * 
     * @param conflict The conflict to resolve
     * @param mergedContent The merged content
     * @return Future with success status
     */
    CompletableFuture<Boolean> resolveMerge(SyncConflict conflict, String mergedContent);
    
    // ==================== Event Listeners ====================
    
    /**
     * Add a listener for sync status changes.
     * 
     * @param listener The listener to add
     */
    void addStatusListener(SyncStatusListener listener);
    
    /**
     * Remove a sync status listener.
     * 
     * @param listener The listener to remove
     */
    void removeStatusListener(SyncStatusListener listener);
    
    /**
     * Listener interface for sync status changes.
     */
    @FunctionalInterface
    interface SyncStatusListener {
        void onStatusChanged(SyncStatus oldStatus, SyncStatus newStatus, String message);
    }
}
