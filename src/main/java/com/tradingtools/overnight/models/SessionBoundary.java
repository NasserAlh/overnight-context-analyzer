package com.tradingtools.overnight.models;

import java.time.LocalDateTime;

/**
 * Represents market session boundaries
 */
public record SessionBoundary(
    LocalDateTime sessionStart,
    LocalDateTime sessionEnd,
    int startIndex,
    int endIndex,
    SessionType type
) {
    public enum SessionType {
        OVERNIGHT("Overnight Session"),
        RTH("Regular Trading Hours"),
        EXTENDED("Extended Hours");
        
        private final String description;
        
        SessionType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    public boolean isInSession(LocalDateTime time) {
        return !time.isBefore(sessionStart) && !time.isAfter(sessionEnd);
    }
    
    public int getBarCount() {
        return endIndex - startIndex + 1;
    }
}