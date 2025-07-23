# Blueprint: Overnight Market Context Analyzer for MotiveWave

## Project Overview

### Objective

Build a MotiveWave study that analyzes overnight market structure and exports actionable trading context scores at 8:30 AM ET daily.

### Core Components

1. Overnight VWAP Calculator
2. Volume Profile Analyzer
3. Market Context Scoring Engine
4. Automated CSV Exporter

## Phase 1: Project Setup and Infrastructure

### 1.1 Maven Project Structure

```
overnight-context-analyzer/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── tradingtools/
│   │   │           └── overnight/
│   │   │               ├── OvernightContextAnalyzer.java    (Main Study)
│   │   │               ├── components/
│   │   │               │   ├── VWAPCalculator.java
│   │   │               │   ├── VolumeProfileAnalyzer.java
│   │   │               │   └── ContextScorer.java
│   │   │               ├── models/
│   │   │               │   ├── MarketContext.java
│   │   │               │   ├── VolumeProfile.java
│   │   │               │   └── SessionBoundary.java
│   │   │               ├── export/
│   │   │               │   ├── CSVExporter.java
│   │   │               │   └── ExportScheduler.java
│   │   │               └── utils/
│   │   │                   ├── SessionDetector.java
│   │   │                   └── TimeUtils.java
│   │   └── resources/
│   │       └── config/
│   │           └── default-settings.properties
├── lib/
│   └── mwave_sdk_062023.jar
└── README.md
```

### 1.2 Maven Configuration (pom.xml)

```xml
<project>
    <groupId>com.tradingtools</groupId>
    <artifactId>overnight-context-analyzer</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    
    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
    </properties>
    
    <dependencies>
        <dependency>
            <groupId>com.motivewave</groupId>
            <artifactId>motivewave-sdk</artifactId>
            <version>062023</version>
            <scope>system</scope>
            <systemPath>${project.basedir}/lib/mwave_sdk_062023.jar</systemPath>
        </dependency>
    </dependencies>
</project>
```

## Phase 2: Core Classes Design

### 2.1 Main Study Class

```java
@StudyHeader(
    name = "Overnight Context Analyzer",
    description = "Analyzes overnight market structure and exports context scores",
    menu = "Custom Studies",
    overlay = false,
    requiresVolume = true
)
public class OvernightContextAnalyzer extends Study {
    // Key components
    private VWAPCalculator vwapCalc;
    private VolumeProfileAnalyzer volumeProfile;
    private ContextScorer scorer;
    private CSVExporter exporter;
    private ExportScheduler scheduler;
    
    // Study settings descriptors
    private IntegerDescriptor exportHourDesc;
    private IntegerDescriptor exportMinuteDesc;
    private FileDesc exportPathDesc;
    private BooleanDescriptor autoExportDesc;
    private ChoiceDescriptor scoringMethodDesc;
}
```

### 2.2 Data Models

#### MarketContext.java

```java
public record MarketContext(
    LocalDateTime timestamp,
    double overnightVWAP,
    double currentPrice,
    double poc,
    double vah,
    double val,
    double volumeBalance,
    int contextScore,
    Map<String, Double> scoreComponents
) {}
```

#### VolumeProfile.java

```java
public class VolumeProfile {
    private TreeMap<Double, Long> priceLevels;
    private double poc;
    private double vah;
    private double val;
    private double totalVolume;
    private int priceTickSize;
}
```

## Phase 3: Component Implementation Plan

### 3.1 Session Detection Logic

```java
public class SessionDetector {
    // Identifies market sessions
    public SessionBoundary getOvernightSession(DataSeries series, int currentIndex);
    public boolean isRTHSession(long timestamp);
    public boolean isOvernightSession(long timestamp);
    private LocalDateTime toMarketTime(long epochMillis);
}
```

**Key Times (ET)**:

- RTH Close: 4:00 PM
- Overnight Start: 6:00 PM
- Overnight End: 9:30 AM
- Export Time: 8:30 AM

### 3.2 VWAP Calculator Implementation

```java
public class VWAPCalculator {
    // Core calculation methods
    public double calculateVWAP(DataSeries series, int startIndex, int endIndex);
    public double getVWAPDeviation(double price, double vwap);
    public Map<Integer, Double> getVWAPBands(double vwap, double stdDev);
    
    // Storage keys for DataSeries
    private static final String OVERNIGHT_VWAP_KEY = "OVERNIGHT_VWAP";
    private static final String VWAP_UPPER_BAND = "VWAP_UPPER";
    private static final String VWAP_LOWER_BAND = "VWAP_LOWER";
}
```

### 3.3 Volume Profile Analyzer

```java
public class VolumeProfileAnalyzer {
    // Profile calculation
    public VolumeProfile buildProfile(DataSeries series, int startIndex, int endIndex);
    private void calculateValueArea(VolumeProfile profile);
    private double findPOC(TreeMap<Double, Long> priceLevels);
    
    // Price level grouping
    private double roundToTickSize(double price, double tickSize);
    private double getTickSize(Instrument instrument);
}
```

### 3.4 Context Scoring Algorithm

```java
public class ContextScorer {
    // Scoring components (each -10 to +10)
    public int scoreVWAPPosition(double price, double vwap, double atr);
    public int scoreValueAreaPosition(double price, double vah, double val);
    public int scorePOCProximity(double price, double poc, double atr);
    public int scoreVolumeBalance(VolumeProfile profile);
    
    // Composite scoring
    public MarketContext calculateContext(/* parameters */);
    private int normalizeScore(double rawScore, double min, double max);
}
```

**Scoring Logic**:

- **VWAP Position**: Above = bullish, Below = bearish
- **Value Area**: Above VAH = bullish, Below VAL = bearish
- **POC Distance**: Near POC = neutral, Far = trending
- **Volume Balance**: Upper heavy = bearish, Lower heavy = bullish

## Phase 4: Export Implementation

### 4.1 Export Scheduler

```java
public class ExportScheduler {
    private LocalDate lastExportDate;
    private final int exportHour = 8;
    private final int exportMinute = 30;
    
    public boolean shouldExport(long currentBarTime);
    public void markExported(LocalDate date);
    private boolean isExportTime(LocalDateTime time);
}
```

### 4.2 CSV Exporter

```java
public class CSVExporter {
    // CSV structure
    private static final String[] HEADERS = {
        "Date", "Time", "Symbol", "VWAP", "POC", "VAH", "VAL",
        "Current_Price", "Context_Score", "VWAP_Position",
        "VA_Position", "POC_Distance", "Volume_Balance"
    };
    
    public void exportContext(MarketContext context, File outputFile);
    private String formatCSVRow(MarketContext context);
    private File getExportFile(String basePath, LocalDate date);
}
```

## Phase 5: Study Integration

### 5.1 Initialize Method

```java
@Override
public void initialize(Defaults defaults) {
    // Configure settings
    SettingTab mainTab = new SettingTab("General");
    
    // Export settings
    exportPathDesc = new FileDesc("exportPath", "Export Directory", 
        new File("C:\\TradingData\\OvernightContext"));
    autoExportDesc = new BooleanDescriptor("autoExport", "Auto Export at 8:30 AM", true);
    
    // Scoring settings
    scoringMethodDesc = new ChoiceDescriptor("scoringMethod", "Scoring Method",
        new String[]{"BALANCED", "VWAP_HEAVY", "VOLUME_HEAVY"}, 0);
    
    // Runtime configuration
    RuntimeDescriptor rd = getRuntimeDescriptor();
    rd.exportValue(new ValueDescriptor("CONTEXT_SCORE", "Market Context Score"));
    rd.exportValue(new ValueDescriptor("OVERNIGHT_VWAP", "Overnight VWAP"));
}
```

### 5.2 Calculate Method

```java
@Override
protected void calculate(int index, DataContext ctx) {
    DataSeries series = ctx.getDataSeries();
    
    // 1. Detect session boundaries
    if (sessionDetector.isSessionBoundary(series, index)) {
        startNewSession(series, index);
    }
    
    // 2. Update calculations
    if (sessionDetector.isOvernightSession(series.getStartTime(index))) {
        updateOvernightCalculations(series, index);
    }
    
    // 3. Check export trigger
    if (scheduler.shouldExport(series.getStartTime(index))) {
        performExport(ctx);
    }
}
```

## Phase 6: Testing Strategy

### 6.1 Unit Tests

- Session detection accuracy
- VWAP calculation correctness
- Volume profile POC/VAH/VAL
- Scoring algorithm boundaries
- Export timing logic

### 6.2 Integration Tests

- Full overnight session processing
- Multi-day continuity
- Export file generation
- Settings persistence

### 6.3 Market Scenario Tests

- Trending overnight sessions
- Range-bound sessions
- Gap scenarios
- Low volume periods

## Phase 7: Deployment Plan

### 7.1 Build Process

```bash
# In WSL environment
cd overnight-context-analyzer
mvn clean compile
mvn package
cp target/overnight-context-analyzer-1.0.0.jar /mnt/c/MotiveWave/plugins/
```

### 7.2 Installation Steps

1. Build JAR in WSL
2. Copy to MotiveWave plugins directory
3. Restart MotiveWave
4. Add study to chart
5. Configure export settings
6. Verify overnight calculations

### 7.3 User Configuration Guide

- Set export directory with write permissions
- Configure export time (default 8:30 AM ET)
- Select scoring method
- Enable/disable auto-export
- Set symbol-specific parameters

## Phase 8: Future Enhancements

### 8.1 Advanced Features

- Multi-symbol batch processing
- Email notification on export
- Real-time dashboard integration
- Historical context database
- Machine learning score optimization

### 8.2 Performance Optimizations

- Incremental volume profile updates
- Cached session boundaries
- Parallel processing for multiple symbols
- Memory-efficient data structures

## Critical Implementation Notes

### SDK Compatibility

- Use June 2023 SDK for MotiveWave 6.9.12
- Avoid newer SDK features not available
- Test all API calls against documentation

### Error Handling

- File I/O exceptions for export
- Missing volume data handling
- Session detection edge cases
- Time zone considerations

### Performance Considerations

- Limit historical lookback
- Optimize volume profile calculations
- Use efficient data structures
- Minimize real-time recalculations

This blueprint provides a comprehensive roadmap for building the overnight context analyzer. Each phase can be implemented incrementally, with testing at each stage to ensure reliability and accuracy.