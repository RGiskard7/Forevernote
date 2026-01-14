package com.example.forevernote.event.events;

import com.example.forevernote.event.AppEvent;

/**
 * UI-related events for the application.
 * 
 * @author Edu Díaz (RGiskard7)
 * @since 1.1.0
 */
public final class UIEvents {
    
    private UIEvents() {
        // Prevent instantiation
    }
    
    /**
     * Event fired when the theme changes.
     */
    public static class ThemeChangedEvent extends AppEvent {
        private final String theme;
        
        public ThemeChangedEvent(String theme) {
            this.theme = theme;
        }
        
        public String getTheme() {
            return theme;
        }
    }
    
    /**
     * Event fired when the view mode changes (editor/split/preview).
     */
    public static class ViewModeChangedEvent extends AppEvent {
        private final String viewMode;
        
        public ViewModeChangedEvent(String viewMode) {
            this.viewMode = viewMode;
        }
        
        public String getViewMode() {
            return viewMode;
        }
    }
    
    /**
     * Event fired when the sidebar visibility changes.
     */
    public static class SidebarToggledEvent extends AppEvent {
        private final boolean visible;
        
        public SidebarToggledEvent(boolean visible) {
            this.visible = visible;
        }
        
        public boolean isVisible() {
            return visible;
        }
    }
    
    /**
     * Event fired when the status bar should be updated.
     */
    public static class StatusUpdateEvent extends AppEvent {
        private final String message;
        
        public StatusUpdateEvent(String message) {
            this.message = message;
        }
        
        public String getMessage() {
            return message;
        }
    }
    
    /**
     * Event fired when Command Palette should be shown.
     */
    public static class ShowCommandPaletteEvent extends AppEvent {
        public ShowCommandPaletteEvent() {
            super();
        }
    }
    
    /**
     * Event fired when Quick Switcher should be shown.
     */
    public static class ShowQuickSwitcherEvent extends AppEvent {
        public ShowQuickSwitcherEvent() {
            super();
        }
    }
    
    /**
     * Event fired when zoom level changes.
     */
    public static class ZoomChangedEvent extends AppEvent {
        private final double zoomLevel;
        
        public ZoomChangedEvent(double zoomLevel) {
            this.zoomLevel = zoomLevel;
        }
        
        public double getZoomLevel() {
            return zoomLevel;
        }
    }
    
    /**
     * Event fired when the info panel should be shown/hidden.
     */
    public static class InfoPanelToggledEvent extends AppEvent {
        private final boolean visible;
        
        public InfoPanelToggledEvent(boolean visible) {
            this.visible = visible;
        }
        
        public boolean isVisible() {
            return visible;
        }
    }
}
