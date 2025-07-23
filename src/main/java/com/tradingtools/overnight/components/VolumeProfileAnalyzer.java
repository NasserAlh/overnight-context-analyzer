package com.tradingtools.overnight.components;

import com.motivewave.platform.sdk.common.DataSeries;
import com.motivewave.platform.sdk.common.Instrument;
import com.tradingtools.overnight.models.VolumeProfile;

/**
 * Analyzes volume distribution and market profile
 */
public class VolumeProfileAnalyzer {
    
    // DataSeries storage keys
    public static final String POC = "VP_POC";
    public static final String VAH = "VP_VAH";
    public static final String VAL = "VP_VAL";
    public static final String VOLUME_BALANCE = "VP_BAL";
    
    private final double defaultTickSize = 0.25; // ES default
    
    /**
     * Build volume profile for session
     */
    public VolumeProfile buildProfile(DataSeries series, int startIndex, int endIndex) {
        // Get instrument once and handle null case
        Instrument instrument = series.getInstrument();
        double tickSize = getTickSize(instrument);
        VolumeProfile profile = new VolumeProfile(tickSize);
        
        // Validate indices
        if (startIndex < 0 || endIndex >= series.size() || startIndex > endIndex) {
            return profile; // Return empty profile for invalid range
        }
        
        // Aggregate volume at each price level
        for (int i = startIndex; i <= endIndex; i++) {
            if (!isValidBar(series, i)) {
                continue; // Skip invalid bars
            }
            
            double high = series.getHigh(i);
            double low = series.getLow(i);
            double close = series.getClose(i);
            long volume = series.getVolume(i);
            
            // Distribute volume across price range
            distributeVolume(profile, high, low, close, volume);
        }
        
        // Calculate value area
        profile.calculateValueArea();
        
        return profile;
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
     * Distribute bar volume across price levels
     */
    private void distributeVolume(VolumeProfile profile, double high, double low, double close, long volume) {
        if (volume <= 0) {
            return; // Skip bars with no volume
        }
        
        // Handle data errors
        if (high < low) {
            double temp = high;
            high = low;
            low = temp;
        }
        
        if (high == low) {
            // Single price point
            profile.addVolume(close, volume);
            return;
        }
        
        // Distribute using triangular distribution weighted toward close
        double tickSize = profile.getPriceTickSize();
        int priceLevels = (int) Math.round((high - low) / tickSize) + 1;
        
        // Limit price levels to prevent excessive computation
        if (priceLevels > 1000) {
            priceLevels = 1000;
            tickSize = (high - low) / 999.0;
        }
        
        double totalWeight = 0;
        double[] weights = new double[priceLevels];
        
        // Calculate weights first
        for (int i = 0; i < priceLevels; i++) {
            double price = low + (i * tickSize);
            if (price > high) price = high;
            
            weights[i] = calculateVolumeWeight(price, high, low, close);
            totalWeight += weights[i];
        }
        
        // Distribute volume proportionally
        long distributedVolume = 0;
        for (int i = 0; i < priceLevels; i++) {
            double price = low + (i * tickSize);
            if (price > high) price = high;
            
            long levelVolume;
            if (i == priceLevels - 1) {
                // Last level gets remaining volume
                levelVolume = volume - distributedVolume;
            } else {
                levelVolume = Math.round(volume * (weights[i] / totalWeight));
                distributedVolume += levelVolume;
            }
            
            if (levelVolume > 0) {
                profile.addVolume(price, levelVolume);
            }
        }
    }
    
    /**
     * Calculate volume weight for price level
     */
    private double calculateVolumeWeight(double price, double high, double low, double close) {
        double range = high - low;
        if (range == 0) return 1.0;
        
        // Use gaussian distribution centered at close
        double distance = Math.abs(price - close);
        double normalizedDistance = distance / range;
        
        // Gaussian weight with sigma = 0.3
        return Math.exp(-Math.pow(normalizedDistance, 2) / (2 * 0.09));
    }
    
    /**
     * Get tick size for instrument - handles null instrument
     */
    private double getTickSize(Instrument instrument) {
        if (instrument == null) {
            return defaultTickSize;
        }
        
        try {
            double tickSize = instrument.getTickSize();
            return tickSize > 0 ? tickSize : defaultTickSize;
        } catch (Exception e) {
            return defaultTickSize;
        }
    }
    
    /**
     * Store profile values in DataSeries
     */
    public void storeProfileValues(DataSeries series, int index, VolumeProfile profile) {
        if (index < 0 || index >= series.size() || profile == null) {
            return;
        }
        
        series.setDouble(index, POC, profile.getPoc());
        series.setDouble(index, VAH, profile.getVah());
        series.setDouble(index, VAL, profile.getVal());
        series.setDouble(index, VOLUME_BALANCE, profile.getVolumeBalance());
    }
    
    /**
     * Calculate POC migration (trend indicator)
     */
    public double calculatePOCMigration(DataSeries series, int currentIndex, int lookback) {
        if (currentIndex < lookback || currentIndex >= series.size()) {
            return 0;
        }
        
        Double currentPOC = series.getDouble(currentIndex, POC);
        Double previousPOC = series.getDouble(currentIndex - lookback, POC);
        
        if (currentPOC == null || previousPOC == null) {
            return 0;
        }
        
        return currentPOC - previousPOC;
    }
    
    /**
     * Check if price is in value area
     */
    public boolean isInValueArea(double price, double vah, double val) {
        return !Double.isNaN(price) && !Double.isNaN(vah) && !Double.isNaN(val) &&
               price >= val && price <= vah;
    }
    
    /**
     * Calculate value area width
     */
    public double getValueAreaWidth(double vah, double val) {
        if (Double.isNaN(vah) || Double.isNaN(val)) {
            return 0;
        }
        return Math.abs(vah - val);
    }
}