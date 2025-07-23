package com.tradingtools.overnight.models;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Immutable record containing complete market context analysis
 */
public record MarketContext(
    LocalDateTime timestamp,
    String symbol,
    double overnightVWAP,
    double currentPrice,
    double poc,
    double vah,
    double val,
    double volumeBalance,
    int contextScore,
    Map<String, Double> scoreComponents,
    Map<String, Object> metadata
) {
    
    /**
     * Builder pattern for complex construction
     */
    public static class Builder {
        private LocalDateTime timestamp;
        private String symbol;
        private double overnightVWAP;
        private double currentPrice;
        private double poc;
        private double vah;
        private double val;
        private double volumeBalance;
        private int contextScore;
        private Map<String, Double> scoreComponents;
        private Map<String, Object> metadata;
        
        public Builder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public Builder symbol(String symbol) {
            this.symbol = symbol;
            return this;
        }
        
        public Builder overnightVWAP(double vwap) {
            this.overnightVWAP = vwap;
            return this;
        }
        
        public Builder currentPrice(double price) {
            this.currentPrice = price;
            return this;
        }
        
        public Builder volumeProfile(double poc, double vah, double val) {
            this.poc = poc;
            this.vah = vah;
            this.val = val;
            return this;
        }
        
        public Builder volumeBalance(double balance) {
            this.volumeBalance = balance;
            return this;
        }
        
        public Builder contextScore(int score) {
            this.contextScore = score;
            return this;
        }
        
        public Builder scoreComponents(Map<String, Double> components) {
            this.scoreComponents = components;
            return this;
        }
        
        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }
        
        public MarketContext build() {
            return new MarketContext(
                timestamp, symbol, overnightVWAP, currentPrice,
                poc, vah, val, volumeBalance, contextScore,
                scoreComponents, metadata
            );
        }
    }
    
    /**
     * CSV export format
     */
    public String toCSV() {
        return String.format("%s,%s,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%d,%.2f,%.2f,%.2f,%.2f",
            timestamp.toLocalDate(),
            timestamp.toLocalTime(),
            overnightVWAP,
            poc,
            vah,
            val,
            currentPrice,
            volumeBalance,
            contextScore,
            scoreComponents.getOrDefault("vwap_score", 0.0),
            scoreComponents.getOrDefault("va_score", 0.0),
            scoreComponents.getOrDefault("poc_score", 0.0),
            scoreComponents.getOrDefault("volume_score", 0.0)
        );
    }
}