package com.tradingtools.overnight.components;

import com.tradingtools.overnight.models.MarketContext;
import com.tradingtools.overnight.models.VolumeProfile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Scoring engine for market context analysis
 */
public class ContextScorer {
    
    public enum ScoringMethod {
        BALANCED(0.25, 0.25, 0.25, 0.25),
        VWAP_HEAVY(0.4, 0.2, 0.2, 0.2),
        VOLUME_HEAVY(0.2, 0.3, 0.3, 0.2);
        
        private final double vwapWeight;
        private final double vaWeight;
        private final double pocWeight;
        private final double volumeWeight;
        
        ScoringMethod(double vwap, double va, double poc, double volume) {
            this.vwapWeight = vwap;
            this.vaWeight = va;
            this.pocWeight = poc;
            this.volumeWeight = volume;
        }
    }
    
    private final ScoringMethod method;
    private final double atrMultiplier;
    
    public ContextScorer(ScoringMethod method, double atrMultiplier) {
        this.method = method;
        this.atrMultiplier = atrMultiplier;
    }
    
    /**
     * Calculate complete market context
     */
    public MarketContext calculateContext(
            LocalDateTime timestamp,
            String symbol,
            double currentPrice,
            double vwap,
            VolumeProfile profile,
            double atr) {
        
        // Calculate individual scores
        int vwapScore = scoreVWAPPosition(currentPrice, vwap, atr);
        int vaScore = scoreValueAreaPosition(currentPrice, profile.getVah(), profile.getVal());
        int pocScore = scorePOCProximity(currentPrice, profile.getPoc(), atr);
        int volumeScore = scoreVolumeBalance(profile);
        
        // Store component scores
        Map<String, Double> components = new HashMap<>();
        components.put("vwap_score", (double) vwapScore);
        components.put("va_score", (double) vaScore);
        components.put("poc_score", (double) pocScore);
        components.put("volume_score", (double) volumeScore);
        
        // Calculate weighted composite score
        double weightedScore = 
            vwapScore * method.vwapWeight +
            vaScore * method.vaWeight +
            pocScore * method.pocWeight +
            volumeScore * method.volumeWeight;
        
        int finalScore = (int) Math.round(weightedScore);
        
        // Build metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("scoring_method", method.name());
        metadata.put("atr", atr);
        metadata.put("value_area_width", profile.getVah() - profile.getVal());
        
        return new MarketContext.Builder()
            .timestamp(timestamp)
            .symbol(symbol)
            .currentPrice(currentPrice)
            .overnightVWAP(vwap)
            .volumeProfile(profile.getPoc(), profile.getVah(), profile.getVal())
            .volumeBalance(profile.getVolumeBalance())
            .contextScore(finalScore)
            .scoreComponents(components)
            .metadata(metadata)
            .build();
    }
    
    /**
     * Score VWAP position (-10 to +10)
     */
    public int scoreVWAPPosition(double price, double vwap, double atr) {
        if (vwap == 0 || atr == 0) return 0;
        
        double deviation = price - vwap;
        double atrUnits = deviation / atr;
        
        // Scale based on ATR units
        if (atrUnits > 2) return 10;        // Strong bullish
        if (atrUnits > 1) return 7;         // Bullish
        if (atrUnits > 0.5) return 4;       // Mild bullish
        if (atrUnits > -0.5) return 0;      // Neutral
        if (atrUnits > -1) return -4;       // Mild bearish
        if (atrUnits > -2) return -7;       // Bearish
        return -10;                          // Strong bearish
    }
    
    /**
     * Score value area position (-10 to +10)
     */
    public int scoreValueAreaPosition(double price, double vah, double val) {
        if (val == 0 || vah == 0) return 0;
        
        double vaRange = vah - val;
        double vaMid = (vah + val) / 2;
        
        if (price > vah) {
            // Above value area
            double distance = (price - vah) / vaRange;
            return Math.min(10, 5 + (int)(distance * 10));
        } else if (price < val) {
            // Below value area
            double distance = (val - price) / vaRange;
            return Math.max(-10, -5 - (int)(distance * 10));
        } else {
            // Within value area
            double position = (price - vaMid) / (vaRange / 2);
            return (int)(position * 4); // -4 to +4
        }
    }
    
    /**
     * Score POC proximity (-10 to +10)
     */
    public int scorePOCProximity(double price, double poc, double atr) {
        if (poc == 0 || atr == 0) return 0;
        
        double distance = Math.abs(price - poc);
        double atrUnits = distance / atr;
        
        // Near POC is neutral, far is trending
        if (atrUnits < 0.5) return 0;       // At POC - neutral
        if (atrUnits < 1) return 3;         // Near POC - mild trend
        if (atrUnits < 2) return 6;         // Away from POC - trending
        
        // Direction matters for extreme distances
        return price > poc ? 9 : -9;         // Strong trend
    }
    
    /**
     * Score volume balance (-10 to +10)
     */
    public int scoreVolumeBalance(VolumeProfile profile) {
        double balance = profile.getVolumeBalance();
        
        // Balance ranges from -1 (all volume at lows) to +1 (all volume at highs)
        // Negative balance is bullish (accumulation at lows)
        // Positive balance is bearish (distribution at highs)
        
        return (int)(-balance * 10); // Invert for intuitive scoring
    }
    
    /**
     * Get score interpretation
     */
    public static String interpretScore(int score) {
        if (score >= 8) return "STRONG_BULLISH";
        if (score >= 4) return "BULLISH";
        if (score >= -3) return "NEUTRAL";
        if (score >= -7) return "BEARISH";
        return "STRONG_BEARISH";
    }
}