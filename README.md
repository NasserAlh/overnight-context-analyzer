# Overnight Context Analyzer

A MotiveWave study that analyzes overnight market structure and generates actionable trading context scores for ES futures. The system calculates overnight VWAP, volume profile metrics, and produces composite context scores from -10 (strong bearish) to +10 (strong bullish).

## Features

- **Overnight VWAP Calculation**: Volume-weighted average price with standard deviation bands for overnight sessions (6:00 PM - 9:30 AM ET)
- **Volume Profile Analysis**: Point of Control (POC), Value Area High/Low (VAH/VAL) calculation with 70% volume distribution
- **Context Scoring**: Multi-component scoring system combining VWAP position, value area analysis, POC proximity, and volume balance
- **Automated Export**: Daily CSV export at 8:30 AM ET with comprehensive market context data
- **Real-time Updates**: Live calculation during market hours with incremental VWAP updates

## Architecture

### Core Components

- **OvernightContextAnalyzer**: Main study class with MotiveWave integration
- **VWAPCalculator**: Incremental VWAP calculation with volume-weighted standard deviation
- **VolumeProfileAnalyzer**: Price-volume distribution analysis using Gaussian weighting
- **ContextScorer**: Composite scoring engine with configurable weighting methods
- **SessionDetector**: Eastern Time session boundary detection and management
- **CSVExporter**: Automated daily export with standardized format

### Data Models

- **MarketContext**: Immutable record containing complete analysis results
- **VolumeProfile**: Price-volume distribution with value area calculations
- **SessionBoundary**: Session timing and index management

## Installation

1. **Prerequisites**:
   - Java 21 with preview features enabled
   - Maven 3.8+
   - MotiveWave platform (June 2023 SDK)

2. **Build**:
   ```bash
   mvn clean compile
   mvn package
   ```

3. **Deploy to MotiveWave**:
   ```bash
   cp target/overnight-context-analyzer-1.0.0.jar /path/to/MotiveWave/plugins/
   ```

4. **WSL Users**:
   ```bash
   cp target/overnight-context-analyzer-1.0.0.jar /mnt/c/MotiveWave/plugins/
   ```

## Configuration

### Study Settings

The study provides three configuration tabs:

#### General Settings
- **VWAP Band Multiplier**: Standard deviation multiplier for VWAP bands (default: 2.0)
- **ATR Period**: Period for Average True Range calculation (default: 14)

#### Export Settings
- **Export Directory**: Path for CSV file output (default: `C:\TradingData\OvernightContext`)
- **Auto Export**: Enable/disable scheduled exports (default: enabled)
- **Export Time**: Hour and minute for daily export (default: 8:30 AM ET)

#### Scoring Settings
- **Scoring Method**: Weight distribution for context components
  - `BALANCED`: Equal weighting (25% each component)
  - `VWAP_HEAVY`: VWAP-focused (40% VWAP, 20% others)
  - `VOLUME_HEAVY`: Volume profile focused (30% VA/POC, 20% others)

## Usage

1. **Apply Study**: Add "Overnight Context Analyzer" from Custom Studies → Market Structure
2. **Configure Settings**: Adjust parameters in the three settings tabs
3. **Monitor Real-time**: Context scores update live during overnight sessions
4. **Review Exports**: Daily CSV files contain complete analysis at 8:30 AM ET

### Exported Data Format

CSV files include:
- Date, Time, Symbol
- VWAP, POC, VAH, VAL values
- Current price and context score
- Individual component scores
- Volume balance and value area metrics
- VWAP deviation percentage

## Context Scoring

### Components (Default Weights)

1. **VWAP Position (25%)**: Price relative to overnight VWAP in ATR units
2. **Value Area Position (25%)**: Location within/outside VAH-VAL range
3. **POC Proximity (25%)**: Distance from Point of Control
4. **Volume Balance (25%)**: Upper vs lower half volume distribution

### Score Interpretation

- **+8 to +10**: Strong Bullish
- **+4 to +7**: Bullish  
- **-3 to +3**: Neutral
- **-7 to -4**: Bearish
- **-10 to -8**: Strong Bearish

## Development

### Project Structure
```
src/main/java/com/tradingtools/overnight/
├── OvernightContextAnalyzer.java       # Main study class
├── components/                         # Core calculation engines
│   ├── ContextScorer.java
│   ├── VWAPCalculator.java
│   └── VolumeProfileAnalyzer.java
├── export/                            # Export system
│   ├── CSVExporter.java
│   └── ExportScheduler.java
├── models/                            # Data models
│   ├── MarketContext.java
│   ├── SessionBoundary.java
│   └── VolumeProfile.java
└── utils/                             # Utilities
    ├── SessionDetector.java
    └── TimeUtils.java
```

### Key Design Patterns
- **Builder Pattern**: Complex object construction (MarketContext)
- **Record Classes**: Immutable data models
- **Strategy Pattern**: Configurable scoring methods
- **Incremental Updates**: Performance optimization for real-time calculation

### Time Zone Handling
All calculations use Eastern Time (`America/New_York`) with proper DST handling. Overnight sessions span 6:00 PM (previous day) to 9:30 AM ET.

## Contributing

1. Follow existing code patterns and conventions
2. Maintain defensive programming practices
3. Test with multiple timeframes and market conditions
4. Update documentation for API changes

## License

This project is for educational and research purposes. Trading involves substantial risk and past performance does not guarantee future results.