package com.example.forevernote.sync;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration for sync services.
 * 
 * <p>Supports various sync providers through a flexible key-value configuration.</p>
 * 
 * @author Edu Díaz (RGiskard7)
 * @since 1.4.0
 */
public class SyncConfig {
    
    /**
     * Supported sync providers.
     */
    public enum Provider {
        /** No sync (local only) */
        NONE,
        /** Dropbox sync */
        DROPBOX,
        /** Google Drive sync */
        GOOGLE_DRIVE,
        /** OneDrive sync */
        ONEDRIVE,
        /** WebDAV server */
        WEBDAV,
        /** Custom S3-compatible storage */
        S3,
        /** Local network sync (LAN) */
        LOCAL_NETWORK,
        /** Custom provider via plugin */
        CUSTOM
    }
    
    private Provider provider = Provider.NONE;
    private String endpoint;
    private String apiKey;
    private String apiSecret;
    private String accessToken;
    private String refreshToken;
    private String syncFolder = "Forevernote";
    private boolean autoSync = false;
    private int autoSyncIntervalMinutes = 15;
    private boolean syncOnStartup = true;
    private boolean syncOnChange = false;
    private Map<String, String> additionalConfig = new HashMap<>();
    
    // ==================== Getters and Setters ====================
    
    public Provider getProvider() {
        return provider;
    }
    
    public SyncConfig setProvider(Provider provider) {
        this.provider = provider;
        return this;
    }
    
    public String getEndpoint() {
        return endpoint;
    }
    
    public SyncConfig setEndpoint(String endpoint) {
        this.endpoint = endpoint;
        return this;
    }
    
    public String getApiKey() {
        return apiKey;
    }
    
    public SyncConfig setApiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }
    
    public String getApiSecret() {
        return apiSecret;
    }
    
    public SyncConfig setApiSecret(String apiSecret) {
        this.apiSecret = apiSecret;
        return this;
    }
    
    public String getAccessToken() {
        return accessToken;
    }
    
    public SyncConfig setAccessToken(String accessToken) {
        this.accessToken = accessToken;
        return this;
    }
    
    public String getRefreshToken() {
        return refreshToken;
    }
    
    public SyncConfig setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
        return this;
    }
    
    public String getSyncFolder() {
        return syncFolder;
    }
    
    public SyncConfig setSyncFolder(String syncFolder) {
        this.syncFolder = syncFolder;
        return this;
    }
    
    public boolean isAutoSync() {
        return autoSync;
    }
    
    public SyncConfig setAutoSync(boolean autoSync) {
        this.autoSync = autoSync;
        return this;
    }
    
    public int getAutoSyncIntervalMinutes() {
        return autoSyncIntervalMinutes;
    }
    
    public SyncConfig setAutoSyncIntervalMinutes(int minutes) {
        this.autoSyncIntervalMinutes = minutes;
        return this;
    }
    
    public boolean isSyncOnStartup() {
        return syncOnStartup;
    }
    
    public SyncConfig setSyncOnStartup(boolean syncOnStartup) {
        this.syncOnStartup = syncOnStartup;
        return this;
    }
    
    public boolean isSyncOnChange() {
        return syncOnChange;
    }
    
    public SyncConfig setSyncOnChange(boolean syncOnChange) {
        this.syncOnChange = syncOnChange;
        return this;
    }
    
    public Map<String, String> getAdditionalConfig() {
        return additionalConfig;
    }
    
    public SyncConfig setAdditionalConfig(Map<String, String> config) {
        this.additionalConfig = config;
        return this;
    }
    
    public SyncConfig addConfig(String key, String value) {
        this.additionalConfig.put(key, value);
        return this;
    }
    
    public String getConfig(String key) {
        return additionalConfig.get(key);
    }
    
    public String getConfig(String key, String defaultValue) {
        return additionalConfig.getOrDefault(key, defaultValue);
    }
    
    // ==================== Persistence ====================
    
    /**
     * Load configuration from preferences.
     * 
     * @param prefs Java Preferences node
     * @return Loaded configuration
     */
    public static SyncConfig loadFromPreferences(java.util.prefs.Preferences prefs) {
        SyncConfig config = new SyncConfig();
        
        String providerName = prefs.get("sync.provider", "NONE");
        try {
            config.provider = Provider.valueOf(providerName);
        } catch (IllegalArgumentException e) {
            config.provider = Provider.NONE;
        }
        
        config.endpoint = prefs.get("sync.endpoint", null);
        config.apiKey = prefs.get("sync.apiKey", null);
        config.apiSecret = prefs.get("sync.apiSecret", null);
        config.accessToken = prefs.get("sync.accessToken", null);
        config.refreshToken = prefs.get("sync.refreshToken", null);
        config.syncFolder = prefs.get("sync.folder", "Forevernote");
        config.autoSync = prefs.getBoolean("sync.autoSync", false);
        config.autoSyncIntervalMinutes = prefs.getInt("sync.autoSyncInterval", 15);
        config.syncOnStartup = prefs.getBoolean("sync.syncOnStartup", true);
        config.syncOnChange = prefs.getBoolean("sync.syncOnChange", false);
        
        return config;
    }
    
    /**
     * Save configuration to preferences.
     * 
     * @param prefs Java Preferences node
     */
    public void saveToPreferences(java.util.prefs.Preferences prefs) {
        prefs.put("sync.provider", provider.name());
        
        if (endpoint != null) prefs.put("sync.endpoint", endpoint);
        else prefs.remove("sync.endpoint");
        
        if (apiKey != null) prefs.put("sync.apiKey", apiKey);
        else prefs.remove("sync.apiKey");
        
        if (apiSecret != null) prefs.put("sync.apiSecret", apiSecret);
        else prefs.remove("sync.apiSecret");
        
        if (accessToken != null) prefs.put("sync.accessToken", accessToken);
        else prefs.remove("sync.accessToken");
        
        if (refreshToken != null) prefs.put("sync.refreshToken", refreshToken);
        else prefs.remove("sync.refreshToken");
        
        prefs.put("sync.folder", syncFolder);
        prefs.putBoolean("sync.autoSync", autoSync);
        prefs.putInt("sync.autoSyncInterval", autoSyncIntervalMinutes);
        prefs.putBoolean("sync.syncOnStartup", syncOnStartup);
        prefs.putBoolean("sync.syncOnChange", syncOnChange);
    }
}
