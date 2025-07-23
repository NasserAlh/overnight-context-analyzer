package com.tradingtools.overnight.models;

import java.util.TreeMap;
import java.util.Map;

/**
 * Volume profile data structure with TPO analysis
 */
public class VolumeProfile {
    private final TreeMap<Double, Long> priceLevels;
    private double poc;
    private double vah;
    private double val;
    private double totalVolume;
    private final double priceTickSize;
    
    public VolumeProfile(double tickSize) {
        this.priceLevels = new TreeMap<>();
        this.priceTickSize = tickSize;
        this.totalVolume = 0;
    }
    
    /**
     * Add volume at specific price level
     */
    public void addVolume(double price, long volume) {
        double roundedPrice = roundToTickSize(price);
        priceLevels.merge(roundedPrice, volume, Long::sum);
        totalVolume += volume;
    }
    
    /**
     * Round price to nearest tick
     */
    private double roundToTickSize(double price) {
        return Math.round(price / priceTickSize) * priceTickSize;
    }
    
    /**
     * Calculate POC, VAH, VAL after all data added
     */
    public void calculateValueArea() {
        if (priceLevels.isEmpty()) return;
        
        // Find POC (Point of Control)
        poc = priceLevels.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(0.0);
        
        // Calculate 70% of total volume for value area
        double valueAreaVolume = totalVolume * 0.7;
        double accumulatedVolume = 0;
        
        // Start from POC and expand outward
        TreeMap<Double, Long> valueArea = new TreeMap<>();
        valueArea.put(poc, priceLevels.get(poc));
        accumulatedVolume = priceLevels.get(poc);
        
        // Expand value area until 70% volume reached
        Double upperBound = poc;
        Double lowerBound = poc;
        
        while (accumulatedVolume < valueAreaVolume) {
            Double nextUpper = priceLevels.higherKey(upperBound);
            Double nextLower = priceLevels.lowerKey(lowerBound);
            
            long upperVolume = (nextUpper != null) ? priceLevels.get(nextUpper) : 0;
            long lowerVolume = (nextLower != null) ? priceLevels.get(nextLower) : 0;
            
            if (upperVolume >= lowerVolume && nextUpper != null) {
                valueArea.put(nextUpper, upperVolume);
                accumulatedVolume += upperVolume;
                upperBound = nextUpper;
            } else if (nextLower != null) {
                valueArea.put(nextLower, lowerVolume);
                accumulatedVolume += lowerVolume;
                lowerBound = nextLower;
            } else {
                break;
            }
        }
        
        vah = valueArea.lastKey();
        val = valueArea.firstKey();
    }
    
    /**
     * Calculate volume balance (upper half vs lower half)
     */
    public double getVolumeBalance() {
        if (priceLevels.isEmpty()) return 0;
        
        double midPoint = (priceLevels.firstKey() + priceLevels.lastKey()) / 2;
        long upperVolume = 0;
        long lowerVolume = 0;
        
        for (Map.Entry<Double, Long> entry : priceLevels.entrySet()) {
            if (entry.getKey() > midPoint) {
                upperVolume += entry.getValue();
            } else {
                lowerVolume += entry.getValue();
            }
        }
        
        return (upperVolume - lowerVolume) / (double) totalVolume;
    }
    
    // Getters
    public double getPoc() { return poc; }
    public double getVah() { return vah; }
    public double getVal() { return val; }
    public double getTotalVolume() { return totalVolume; }
    public TreeMap<Double, Long> getPriceLevels() { return new TreeMap<>(priceLevels); }
    public double getPriceTickSize() { return priceTickSize; }
}