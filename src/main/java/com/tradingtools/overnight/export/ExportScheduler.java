package com.tradingtools.overnight.export;

import com.tradingtools.overnight.utils.TimeUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Manages export scheduling and tracking
 */
public class ExportScheduler {
    
    private LocalDate lastExportDate;
    private final int exportHour;
    private final int exportMinute;
    private boolean exportEnabled;
    
    public ExportScheduler(int hour, int minute) {
        this.exportHour = hour;
        this.exportMinute = minute;
        this.exportEnabled = true;
    }
    
    /**
     * Check if export should occur
     */
    public boolean shouldExport(long currentBarTime) {
        if (!exportEnabled) return false;
        
        LocalDateTime currentTime = TimeUtils.toET(currentBarTime);
        LocalDate marketDate = TimeUtils.getMarketDate(currentBarTime);
        
        // Check if already exported today
        if (lastExportDate != null && lastExportDate.equals(marketDate)) {
            return false;
        }
        
        // Check if it's export time
        return isExportTime(currentTime);
    }
    
    /**
     * Check if current time matches export schedule
     */
    private boolean isExportTime(LocalDateTime time) {
        return time.getHour() == exportHour && 
               time.getMinute() >= exportMinute &&
               time.getMinute() < exportMinute + 5; // 5-minute window
    }
    
    /**
     * Mark export as completed
     */
    public void markExported(LocalDate date) {
        this.lastExportDate = date;
    }
    
    /**
     * Reset export tracking
     */
    public void reset() {
        this.lastExportDate = null;
    }
    
    /**
     * Enable/disable exports
     */
    public void setEnabled(boolean enabled) {
        this.exportEnabled = enabled;
    }
    
    public boolean isEnabled() {
        return exportEnabled;
    }
    
    public LocalDate getLastExportDate() {
        return lastExportDate;
    }
}