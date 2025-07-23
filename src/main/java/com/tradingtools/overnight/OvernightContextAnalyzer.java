package com.tradingtools.overnight;

import java.io.File;
import java.time.LocalDate;
import java.util.Map;

import com.motivewave.platform.sdk.common.DataContext;
import com.motivewave.platform.sdk.common.DataSeries;
import com.motivewave.platform.sdk.common.Defaults;
import com.motivewave.platform.sdk.common.Settings;
import com.motivewave.platform.sdk.common.desc.BooleanDescriptor;
import com.motivewave.platform.sdk.common.desc.DoubleDescriptor;
import com.motivewave.platform.sdk.common.desc.FileDescriptor;
import com.motivewave.platform.sdk.common.desc.IntegerDescriptor;
import com.motivewave.platform.sdk.common.desc.SettingGroup;
import com.motivewave.platform.sdk.common.desc.SettingTab;
import com.motivewave.platform.sdk.common.desc.SettingsDescriptor;
import com.motivewave.platform.sdk.common.desc.StringDescriptor;
import com.motivewave.platform.sdk.common.desc.ValueDescriptor;
import com.motivewave.platform.sdk.study.RuntimeDescriptor;
import com.motivewave.platform.sdk.study.Study;
import com.motivewave.platform.sdk.study.StudyHeader;
import com.tradingtools.overnight.components.ContextScorer;
import com.tradingtools.overnight.components.VWAPCalculator;
import com.tradingtools.overnight.components.VolumeProfileAnalyzer;
import com.tradingtools.overnight.export.CSVExporter;
import com.tradingtools.overnight.export.ExportScheduler;
import com.tradingtools.overnight.models.MarketContext;
import com.tradingtools.overnight.models.SessionBoundary;
import com.tradingtools.overnight.models.VolumeProfile;
import com.tradingtools.overnight.utils.SessionDetector;
import com.tradingtools.overnight.utils.TimeUtils;

/**
 * Main study class for overnight market context analysis
 */
@StudyHeader(
    namespace = "com.tradingtools",
    id = "OVERNIGHT_CONTEXT_ANALYZER",
    name = "Overnight Context Analyzer",
    desc = "Analyzes overnight market structure and exports context scores",
    menu = "Custom Studies",
    overlay = false,
    requiresVolume = true
)
public class OvernightContextAnalyzer extends Study {
    
    // Component instances
    private VWAPCalculator vwapCalculator;
    private VolumeProfileAnalyzer volumeAnalyzer;
    private ContextScorer contextScorer;
    private CSVExporter csvExporter;
    private ExportScheduler exportScheduler;
    private SessionDetector sessionDetector;
    
    // Settings descriptors
    private IntegerDescriptor exportHourDesc;
    private IntegerDescriptor exportMinuteDesc;
    private FileDescriptor exportPathDesc;
    private BooleanDescriptor autoExportDesc;
    private StringDescriptor scoringMethodDesc;
    private DoubleDescriptor vwapBandMultiplierDesc;
    private IntegerDescriptor atrPeriodDesc;
    
    // Session tracking
    private SessionBoundary currentSession;
    private int sessionStartIndex = -1;
    
    // Value keys for data storage
    private static final String ATR_KEY = "ATR";
    private static final String CONTEXT_SCORE_KEY = "CONTEXT_SCORE";
    
    @Override
    public void initialize(Defaults defaults) {
        // Create settings descriptor
        SettingsDescriptor sd = new SettingsDescriptor();
        
        // Configure settings tabs
        configureGeneralSettings(sd);
        configureExportSettings(sd);
        configureScoringSettings(sd);
        
        // Set the settings descriptor
        setSettingsDescriptor(sd);
        
        // Configure runtime descriptor
        configureRuntimeDescriptor();
    }
    
    /**
     * Configure general settings tab
     */
    private void configureGeneralSettings(SettingsDescriptor sd) {
        SettingTab tab = new SettingTab("General");
        
        SettingGroup vwapGroup = new SettingGroup("VWAP Settings");
        vwapBandMultiplierDesc = new DoubleDescriptor("vwapBands", "Band Multiplier", 2.0, 0.5, 4.0, 0.25);
        vwapGroup.addRow(vwapBandMultiplierDesc);
        
        SettingGroup atrGroup = new SettingGroup("ATR Settings");
        atrPeriodDesc = new IntegerDescriptor("atrPeriod", "ATR Period", 14, 5, 50, 1);
        atrGroup.addRow(atrPeriodDesc);
        
        tab.addGroup(vwapGroup);
        tab.addGroup(atrGroup);
        
        sd.addTab(tab);
    }
    
    /**
     * Configure export settings tab
     */
    private void configureExportSettings(SettingsDescriptor sd) {
        SettingTab tab = new SettingTab("Export");
        
        SettingGroup exportGroup = new SettingGroup("CSV Export");
        
        // FileDescriptor for export path
        exportPathDesc = new FileDescriptor("exportPath", "Export Directory", 
            new File("C:\\TradingData\\OvernightContext"));
        
        autoExportDesc = new BooleanDescriptor("autoExport", "Auto Export at Schedule", true);
        
        exportHourDesc = new IntegerDescriptor("exportHour", "Export Hour (ET)", 8, 0, 23, 1);
        exportMinuteDesc = new IntegerDescriptor("exportMinute", "Export Minute", 30, 0, 59, 1);
        
        exportGroup.addRow(exportPathDesc);
        exportGroup.addRow(autoExportDesc);
        exportGroup.addRow(exportHourDesc, exportMinuteDesc);
        
        tab.addGroup(exportGroup);
        sd.addTab(tab);
    }
    
    /**
     * Configure scoring settings tab
     */
    private void configureScoringSettings(SettingsDescriptor sd) {
        SettingTab tab = new SettingTab("Scoring");
        
        SettingGroup scoringGroup = new SettingGroup("Context Scoring");
        
        // Use StringDescriptor for scoring method
        scoringMethodDesc = new StringDescriptor("scoringMethod", "Scoring Method", "BALANCED");
        
        scoringGroup.addRow(scoringMethodDesc);
        
        tab.addGroup(scoringGroup);
        sd.addTab(tab);
    }
    
    /**
     * Configure runtime descriptor
     */
    private void configureRuntimeDescriptor() {
        RuntimeDescriptor rd = getRuntimeDescriptor();
        
        // Declare paths
        rd.declarePath(VWAPCalculator.OVERNIGHT_VWAP, "vwapPath");
        rd.declarePath(VWAPCalculator.VWAP_UPPER, "vwapUpperPath");
        rd.declarePath(VWAPCalculator.VWAP_LOWER, "vwapLowerPath");
        
        // Export values
        rd.exportValue(new ValueDescriptor(CONTEXT_SCORE_KEY, "Market Context Score", null));
        rd.exportValue(new ValueDescriptor(VWAPCalculator.OVERNIGHT_VWAP, "Overnight VWAP", null));
        rd.exportValue(new ValueDescriptor(VolumeProfileAnalyzer.POC, "Point of Control", null));
        rd.exportValue(new ValueDescriptor(VolumeProfileAnalyzer.VAH, "Value Area High", null));
        rd.exportValue(new ValueDescriptor(VolumeProfileAnalyzer.VAL, "Value Area Low", null));
        
        // Add horizontal lines for key levels
        rd.setMinTopValue(10);
        rd.setMaxBottomValue(-10);
        rd.setFixedTopValue(10);
        rd.setFixedBottomValue(-10);
    }
    
    @Override
    protected void calculate(int index, DataContext ctx) {
        DataSeries series = ctx.getDataSeries();
        
        // Initialize components on first calculation
        if (index == 0) {
            initializeComponents();
        }
        
        // Calculate ATR
        calculateATR(series, index);
        
        // Check for session boundary
        if (sessionDetector.isSessionBoundary(series, index)) {
            handleSessionBoundary(series, index);
        }
        
        // Update calculations if in overnight session
        if (sessionDetector.isOvernightSession(series.getStartTime(index))) {
            updateOvernightCalculations(series, index, ctx);
        }
        
        // Check for export trigger
        if (exportScheduler.shouldExport(series.getStartTime(index))) {
            performScheduledExport(series, index, ctx);
        }
    }
    
    /**
     * Initialize component instances
     */
    private void initializeComponents() {
        Settings settings = getSettings();
        
        // Initialize calculators
        double bandMultiplier = settings.getDouble("vwapBands", 2.0);
        vwapCalculator = new VWAPCalculator(bandMultiplier);
        volumeAnalyzer = new VolumeProfileAnalyzer();
        
        // Initialize scorer
        String scoringMethod = settings.getString("scoringMethod", "BALANCED");
        ContextScorer.ScoringMethod method;
        try {
            method = ContextScorer.ScoringMethod.valueOf(scoringMethod);
        } catch (IllegalArgumentException e) {
            method = ContextScorer.ScoringMethod.BALANCED;
        }
        contextScorer = new ContextScorer(method, 1.0);
        
        // Initialize export system
        File exportPath = settings.getFile("exportPath");
        if (exportPath == null) {
            exportPath = new File("C:\\TradingData\\OvernightContext");
        }
        csvExporter = new CSVExporter(exportPath);
        
        int exportHour = settings.getInteger("exportHour", 8);
        int exportMinute = settings.getInteger("exportMinute", 30);
        exportScheduler = new ExportScheduler(exportHour, exportMinute);
        exportScheduler.setEnabled(settings.getBoolean("autoExport", true));
        
        // Initialize session detector
        sessionDetector = new SessionDetector();
    }
    
    /**
     * Calculate ATR for context scoring
     */
    private void calculateATR(DataSeries series, int index) {
        int period = getSettings().getInteger("atrPeriod", 14);
        
        if (index < period) {
            series.setDouble(index, ATR_KEY, 0.0);
            return;
        }
        
        double sum = 0;
        for (int i = 0; i < period; i++) {
            int idx = index - i;
            double high = series.getHigh(idx);
            double low = series.getLow(idx);
            double prevClose = idx > 0 ? series.getClose(idx - 1) : series.getOpen(idx);
            
            double tr = Math.max(high - low, Math.max(Math.abs(high - prevClose), Math.abs(low - prevClose)));
            sum += tr;
        }
        
        double atr = sum / period;
        series.setDouble(index, ATR_KEY, atr);
    }
    
    /**
     * Handle session boundary transition
     */
    private void handleSessionBoundary(DataSeries series, int index) {
        // Get new overnight session
        currentSession = sessionDetector.getOvernightSession(series, index);
        sessionStartIndex = currentSession.startIndex();
        
        // Reset VWAP calculations
        series.setDouble(index, VWAPCalculator.CUMULATIVE_TPV, 0.0);
        series.setDouble(index, VWAPCalculator.CUMULATIVE_VOL, 0.0);
    }
    
    /**
     * Update overnight calculations
     */
    private void updateOvernightCalculations(DataSeries series, int index, DataContext ctx) {
        if (sessionStartIndex < 0) return;
        
        // Update VWAP
        double vwap = vwapCalculator.updateVWAP(series, sessionStartIndex, index);
        Map<String, Double> bands = vwapCalculator.calculateVWAPBands(series, sessionStartIndex, index, vwap);
        vwapCalculator.storeVWAPValues(series, index, vwap, bands);
        
        // Build volume profile
        VolumeProfile profile = volumeAnalyzer.buildProfile(series, sessionStartIndex, index);
        volumeAnalyzer.storeProfileValues(series, index, profile);
        
        // Calculate context score
        double atr = series.getDouble(index, ATR_KEY);
        double currentPrice = series.getClose(index);
        
        MarketContext context = contextScorer.calculateContext(
            TimeUtils.toET(series.getStartTime(index)),
            ctx.getInstrument().getSymbol(),
            currentPrice,
            vwap,
            profile,
            atr
        );
        
        // Store context score
        series.setInt(index, CONTEXT_SCORE_KEY, context.contextScore());
    }
    
    /**
     * Perform scheduled export
     */
    private void performScheduledExport(DataSeries series, int index, DataContext ctx) {
        try {
            // Build volume profile for full session
            VolumeProfile profile = volumeAnalyzer.buildProfile(series, sessionStartIndex, index);
            
            // Get current values
            double vwap = series.getDouble(index, VWAPCalculator.OVERNIGHT_VWAP);
            double currentPrice = series.getClose(index);
            double atr = series.getDouble(index, ATR_KEY);
            
            // Create context
            MarketContext context = contextScorer.calculateContext(
                TimeUtils.toET(series.getStartTime(index)),
                ctx.getInstrument().getSymbol(),
                currentPrice,
                vwap,
                profile,
                atr
            );
            
            // Export to CSV
            csvExporter.exportContext(context);
            
            // Mark as exported
            LocalDate marketDate = TimeUtils.getMarketDate(series.getStartTime(index));
            exportScheduler.markExported(marketDate);
            
            // Log success
            info("Context exported for " + marketDate);
            
        } catch (Exception e) {
            error("Export failed: " + e.getMessage());
        }
    }
    
    @Override
    public void onBarUpdate(DataContext ctx) {
        // Handle real-time updates
        DataSeries series = ctx.getDataSeries();
        int lastIndex = series.size() - 1;
        
        calculate(lastIndex, ctx);
    }
}