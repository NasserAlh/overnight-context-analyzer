package com.tradingtools.overnight.utils;

import java.time.*;
import java.time.format.DateTimeFormatter;

/**
 * Time utilities for trading calculations
 */
public class TimeUtils {
    
    private static final ZoneId ET_ZONE = ZoneId.of("America/New_York");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATETIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Check if given time is at specific hour and minute
     */
    public static boolean isTime(long epochMillis, int hour, int minute) {
        LocalDateTime dt = toET(epochMillis);
        return dt.getHour() == hour && dt.getMinute() == minute;
    }
    
    /**
     * Convert epoch to ET LocalDateTime
     */
    public static LocalDateTime toET(long epochMillis) {
        return Instant.ofEpochMilli(epochMillis)
            .atZone(ET_ZONE)
            .toLocalDateTime();
    }
    
    /**
     * Format timestamp for display
     */
    public static String formatDateTime(long epochMillis) {
        return DATETIME_FORMAT.format(toET(epochMillis));
    }
    
    /**
     * Format date only
     */
    public static String formatDate(long epochMillis) {
        return DATE_FORMAT.format(toET(epochMillis));
    }
    
    /**
     * Format time only
     */
    public static String formatTime(long epochMillis) {
        return TIME_FORMAT.format(toET(epochMillis));
    }
    
    /**
     * Get market date (considering overnight session)
     */
    public static LocalDate getMarketDate(long epochMillis) {
        LocalDateTime dt = toET(epochMillis);
        
        // If before 6 PM, it belongs to current date
        // If after 6 PM, it belongs to next trading date
        if (dt.toLocalTime().isAfter(LocalTime.of(18, 0))) {
            return dt.toLocalDate().plusDays(1);
        }
        
        return dt.toLocalDate();
    }
}