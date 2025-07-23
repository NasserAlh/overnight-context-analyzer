package com.tradingtools.overnight.export;

import com.tradingtools.overnight.models.MarketContext;
import com.tradingtools.overnight.components.ContextScorer;
import com.tradingtools.overnight.utils.TimeUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CSV export functionality
 */
public class CSVExporter {
    
    private static final String[] HEADERS = {
        "Date", "Time", "Symbol", "VWAP", "POC", "VAH", "VAL",
        "Current_Price", "Context_Score", "Score_Interpretation",
        "VWAP_Score", "VA_Score", "POC_Score", "Volume_Balance_Score",
        "Volume_Balance", "VA_Width", "VWAP_Deviation_%"
    };
    
    private static final DateTimeFormatter FILE_DATE_FORMAT = 
        DateTimeFormatter.ofPattern("yyyyMMdd");
    
    private final File baseDirectory;
    
    public CSVExporter(File baseDirectory) {
        this.baseDirectory = baseDirectory;
        ensureDirectoryExists();
    }
    
    /**
     * Export market context to CSV
     */
    public void exportContext(MarketContext context) throws IOException {
        File exportFile = getExportFile(context.timestamp().toLocalDate());
        boolean fileExists = exportFile.exists();
        
        // Create parent directories if needed
        exportFile.getParentFile().mkdirs();
        
        try (PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(
                    new FileOutputStream(exportFile, true),
                    StandardCharsets.UTF_8))) {
            
            // Write headers if new file
            if (!fileExists) {
                writer.println(String.join(",", HEADERS));
            }
            
            // Write data row
            writer.println(formatCSVRow(context));
            writer.flush();
        }
    }
    
    /**
     * Export multiple contexts
     */
    public void exportContexts(List<MarketContext> contexts) throws IOException {
        if (contexts.isEmpty()) return;
        
        // Group by date
        Map<LocalDate, List<MarketContext>> byDate = contexts.stream()
            .collect(Collectors.groupingBy(ctx -> ctx.timestamp().toLocalDate()));
        
        for (Map.Entry<LocalDate, List<MarketContext>> entry : byDate.entrySet()) {
            File exportFile = getExportFile(entry.getKey());
            exportToFile(exportFile, entry.getValue());
        }
    }
    
    /**
     * Format context as CSV row
     */
    private String formatCSVRow(MarketContext ctx) {
        List<String> values = new ArrayList<>();
        
        // Basic data
        values.add(ctx.timestamp().toLocalDate().toString());
        values.add(ctx.timestamp().toLocalTime().toString());
        values.add(ctx.symbol());
        values.add(String.format("%.2f", ctx.overnightVWAP()));
        values.add(String.format("%.2f", ctx.poc()));
        values.add(String.format("%.2f", ctx.vah()));
        values.add(String.format("%.2f", ctx.val()));
        values.add(String.format("%.2f", ctx.currentPrice()));
        values.add(String.valueOf(ctx.contextScore()));
        values.add(ContextScorer.interpretScore(ctx.contextScore()));
        
        // Component scores
        values.add(String.format("%.0f", ctx.scoreComponents().get("vwap_score")));
        values.add(String.format("%.0f", ctx.scoreComponents().get("va_score")));
        values.add(String.format("%.0f", ctx.scoreComponents().get("poc_score")));
        values.add(String.format("%.0f", ctx.scoreComponents().get("volume_score")));
        
        // Additional metrics
        values.add(String.format("%.3f", ctx.volumeBalance()));
        values.add(String.format("%.2f", ctx.vah() - ctx.val()));
        
        // VWAP deviation
        double vwapDev = ((ctx.currentPrice() - ctx.overnightVWAP()) / ctx.overnightVWAP()) * 100;
        values.add(String.format("%.2f", vwapDev));
        
        return String.join(",", values);
    }
    
    /**
     * Get export file for date
     */
    private File getExportFile(LocalDate date) {
        String filename = String.format("overnight_context_%s.csv", 
            FILE_DATE_FORMAT.format(date));
        return new File(baseDirectory, filename);
    }
    
    /**
     * Export to specific file
     */
    private void exportToFile(File file, List<MarketContext> contexts) throws IOException {
        file.getParentFile().mkdirs();
        
        try (PrintWriter writer = new PrintWriter(
                new OutputStreamWriter(
                    new FileOutputStream(file),
                    StandardCharsets.UTF_8))) {
            
            // Write headers
            writer.println(String.join(",", HEADERS));
            
            // Write data
            for (MarketContext ctx : contexts) {
                writer.println(formatCSVRow(ctx));
            }
            
            writer.flush();
        }
    }
    
    /**
     * Ensure export directory exists
     */
    private void ensureDirectoryExists() {
        if (!baseDirectory.exists()) {
            baseDirectory.mkdirs();
        }
    }
    
    /**
     * Get list of exported files
     */
    public List<File> getExportedFiles() {
        List<File> files = new ArrayList<>();
        
        if (baseDirectory.exists() && baseDirectory.isDirectory()) {
            File[] csvFiles = baseDirectory.listFiles(
                (dir, name) -> name.startsWith("overnight_context_") && name.endsWith(".csv")
            );
            
            if (csvFiles != null) {
                files.addAll(Arrays.asList(csvFiles));
            }
        }
        
        return files;
    }
}