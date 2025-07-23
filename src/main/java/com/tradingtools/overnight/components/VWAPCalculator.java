package com.tradingtools.overnight.components;

import java.util.HashMap;
import java.util.Map;

import com.motivewave.platform.sdk.common.DataSeries;

/**
 * Volume Weighted Average Price calculator
 */
public class VWAPCalculator {
    
    // DataSeries storage keys
    public static final String OVERNIGHT_VWAP = "ON_VWAP";
    public static final String VWAP_UPPER = "VWAP_UP";
    public static final String VWAP_LOWER = "VWAP_LO";
    public static final String CUMULATIVE_TPV = "CUM_TPV";
    public static final String CUMULATIVE_VOL = "CUM_VOL";
    
    private final double bandMultiplier;
    
    public VWAPCalculator(double bandMultiplier) {
        this.bandMultiplier = bandMultiplier;
    }
    
    /**
     * Calculate VWAP for session range
     */
    public double calculateVWAP(DataSeries series, int startIndex, int endIndex) {
        if (startIndex < 0 || endIndex >= series.size() || startIndex > endIndex) {
            return Double.NaN;
        }
        
        double cumulativeTPV = 0; // Total Price * Volume
        long cumulativeVolume = 0; // Keep as long for precision
        
        for (int i = startIndex; i <= endIndex; i++) {
            if (!isValidBar(series, i)) {
                continue; // Skip invalid bars
            }
            
            double typicalPrice = getTypicalPrice(series, i);
            long volume = series.getVolume(i);
            
            cumulativeTPV += typicalPrice * volume;
            cumulativeVolume += volume;
            
            // Store intermediate values for incremental updates
            series.setDouble(i, CUMULATIVE_TPV, cumulativeTPV);
            series.setDouble(i, CUMULATIVE_VOL, (double)cumulativeVolume);
        }
        
        return cumulativeVolume > 0 ? cumulativeTPV / cumulativeVolume : Double.NaN;
    }
    
    /**
     * Update VWAP incrementally for new bar
     */
    public double updateVWAP(DataSeries series, int sessionStart, int currentIndex) {
        if (currentIndex <= sessionStart || currentIndex >= series.size()) {
            return Double.NaN;
        }
        
        if (currentIndex == sessionStart) {
            return calculateVWAP(series, sessionStart, currentIndex);
        }
        
        if (!isValidBar(series, currentIndex)) {
            // Return previous VWAP if current bar is invalid
            Double prevVWAP = series.getDouble(currentIndex - 1, OVERNIGHT_VWAP);
            return prevVWAP != null ? prevVWAP : Double.NaN;
        }
        
        // Get previous cumulative values
        Double prevTPV = series.getDouble(currentIndex - 1, CUMULATIVE_TPV);
        Double prevVolume = series.getDouble(currentIndex - 1, CUMULATIVE_VOL);
        
        if (prevTPV == null || prevVolume == null) {
            // Fallback to full calculation
            return calculateVWAP(series, sessionStart, currentIndex);
        }
        
        // Add current bar
        double typicalPrice = getTypicalPrice(series, currentIndex);
        long volume = series.getVolume(currentIndex);
        
        double newTPV = prevTPV + (typicalPrice * volume);
        double newVolume = prevVolume + volume;
        
        // Store new cumulative values
        series.setDouble(currentIndex, CUMULATIVE_TPV, newTPV);
        series.setDouble(currentIndex, CUMULATIVE_VOL, newVolume);
        
        return newVolume > 0 ? newTPV / newVolume : Double.NaN;
    }
    
    /**
     * Calculate VWAP bands based on standard deviation
     */
    public Map<String, Double> calculateVWAPBands(DataSeries series, int startIndex, int endIndex, double vwap) {
        Map<String, Double> bands = new HashMap<>();
        
        if (Double.isNaN(vwap) || startIndex < 0 || endIndex >= series.size() || startIndex > endIndex) {
            bands.put("upper", Double.NaN);
            bands.put("lower", Double.NaN);
            bands.put("stdDev", Double.NaN);
            return bands;
        }
        
        double sumSquaredDiff = 0;
        long totalVolume = 0;
        
        for (int i = startIndex; i <= endIndex; i++) {
            if (!isValidBar(series, i)) {
                continue;
            }
            
            double typicalPrice = getTypicalPrice(series, i);
            long volume = series.getVolume(i);
            
            sumSquaredDiff += Math.pow(typicalPrice - vwap, 2) * volume;
            totalVolume += volume;
        }
        
        double variance = totalVolume > 0 ? sumSquaredDiff / totalVolume : 0;
        double stdDev = Math.sqrt(variance);
        
        bands.put("upper", vwap + (bandMultiplier * stdDev));
        bands.put("lower", vwap - (bandMultiplier * stdDev));
        bands.put("stdDev", stdDev);
        
        return bands;
    }
    
    /**
     * Validate bar data
     */
    private boolean isValidBar(DataSeries series, int index) {
        if (index < 0 || index >= series.size()) {
            return false;
        }
        
        double high = series.getHigh(index);
        double low = series.getLow(index);
        double close = series.getClose(index);
        long volume = series.getVolume(index);
        
        return !Double.isNaN(high) && !Double.isNaN(low) && !Double.isNaN(close) &&
               volume > 0 && high >= low;
    }
    
    /**
     * Get typical price (HLC/3) with validation
     */
    private double getTypicalPrice(DataSeries series, int index) {
        double high = series.getHigh(index);
        double low = series.getLow(index);
        double close = series.getClose(index);
        
        // Validate all components
        if (Double.isNaN(high) || Double.isNaN(low) || Double.isNaN(close)) {
            return Double.NaN;
        }
        
        // Handle bad data where high < low
        if (high < low) {
            double temp = high;
            high = low;
            low = temp;
        }
        
        return (high + low + close) / 3.0;
    }
    
    /**
     * Calculate deviation from VWAP
     */
    public double getVWAPDeviation(double price, double vwap) {
        if (Double.isNaN(price) || Double.isNaN(vwap) || vwap == 0) {
            return Double.NaN;
        }
        return ((price - vwap) / vwap) * 100;
    }
    
    /**
     * Store VWAP values in DataSeries with validation
     */
    public void storeVWAPValues(DataSeries series, int index, double vwap, Map<String, Double> bands) {
        if (index < 0 || index >= series.size() || bands == null) {
            return;
        }
        
        series.setDouble(index, OVERNIGHT_VWAP, vwap);
        series.setDouble(index, VWAP_UPPER, bands.get("upper"));
        series.setDouble(index, VWAP_LOWER, bands.get("lower"));
    }
}