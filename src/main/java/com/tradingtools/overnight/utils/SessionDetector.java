package com.tradingtools.overnight.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

import com.motivewave.platform.sdk.common.DataSeries;
import com.tradingtools.overnight.models.SessionBoundary;
import com.tradingtools.overnight.models.SessionBoundary.SessionType;

/**
 * Detects and manages market session boundaries
 */
public class SessionDetector {
    
    // Market hours in ET (Eastern Time)
    private static final LocalTime RTH_START = LocalTime.of(9, 30);
    private static final LocalTime RTH_END = LocalTime.of(16, 0);
    private static final LocalTime OVERNIGHT_START = LocalTime.of(18, 0);
    
    private static final ZoneId ET_ZONE = ZoneId.of("America/New_York");
    
    /**
     * Get overnight session for current bar
     */
    public SessionBoundary getOvernightSession(DataSeries series, int currentIndex) {
        long currentTime = series.getStartTime(currentIndex);
        LocalDateTime currentDT = toMarketTime(currentTime);
        
        // Determine overnight session start
        LocalDateTime sessionStart;
        if (currentDT.toLocalTime().isBefore(RTH_START)) {
            // We're in the morning part of overnight session
            sessionStart = currentDT.toLocalDate().minusDays(1).atTime(OVERNIGHT_START);
        } else if (currentDT.toLocalTime().isAfter(OVERNIGHT_START)) {
            // We're in the evening part of overnight session
            sessionStart = currentDT.toLocalDate().atTime(OVERNIGHT_START);
        } else {
            // We're in RTH, get previous overnight
            sessionStart = currentDT.toLocalDate().minusDays(1).atTime(OVERNIGHT_START);
        }
        
        LocalDateTime sessionEnd = sessionStart.plusDays(1).withHour(9).withMinute(30);
        
        // Find indices
        int startIndex = findTimeIndex(series, currentIndex, sessionStart);
        int endIndex = findTimeIndex(series, currentIndex, sessionEnd);
        
        return new SessionBoundary(sessionStart, sessionEnd, startIndex, endIndex, SessionType.OVERNIGHT);
    }
    
    /**
     * Check if timestamp is in overnight session
     */
    public boolean isOvernightSession(long timestamp) {
        LocalDateTime dt = toMarketTime(timestamp);
        LocalTime time = dt.toLocalTime();
        
        return time.isAfter(OVERNIGHT_START) || time.isBefore(RTH_START);
    }
    
    /**
     * Check if timestamp is in RTH session
     */
    public boolean isRTHSession(long timestamp) {
        LocalDateTime dt = toMarketTime(timestamp);
        LocalTime time = dt.toLocalTime();
        
        return !time.isBefore(RTH_START) && !time.isAfter(RTH_END);
    }
    
    /**
     * Check if we're at a session boundary
     */
    public boolean isSessionBoundary(DataSeries series, int index) {
        if (index == 0) return true;
        
        long currentTime = series.getStartTime(index);
        long previousTime = series.getStartTime(index - 1);
        
        boolean currentIsOvernight = isOvernightSession(currentTime);
        boolean previousIsOvernight = isOvernightSession(previousTime);
        
        return currentIsOvernight != previousIsOvernight;
    }
    
    /**
     * Convert epoch millis to market time (ET)
     */
    private LocalDateTime toMarketTime(long epochMillis) {
        return Instant.ofEpochMilli(epochMillis)
            .atZone(ET_ZONE)
            .toLocalDateTime();
    }
    
    /**
     * Find bar index for specific time
     */
    private int findTimeIndex(DataSeries series, int startFrom, LocalDateTime targetTime) {
        long targetMillis = targetTime.atZone(ET_ZONE).toInstant().toEpochMilli();
        
        // Search backwards
        for (int i = startFrom; i >= 0; i--) {
            if (series.getStartTime(i) <= targetMillis) {
                return i;
            }
        }
        
        // Search forwards if not found backwards
        for (int i = startFrom + 1; i < series.size(); i++) {
            if (series.getStartTime(i) >= targetMillis) {
                return i;
            }
        }
        
        return startFrom; // Fallback
    }
    
    /**
     * Get next RTH open time from current timestamp
     */
    public LocalDateTime getNextRTHOpen(long timestamp) {
        LocalDateTime dt = toMarketTime(timestamp);
        LocalDateTime rthOpen = dt.toLocalDate().atTime(RTH_START);
        
        if (dt.toLocalTime().isBefore(RTH_START)) {
            return rthOpen;
        } else {
            return rthOpen.plusDays(1);
        }
    }
}