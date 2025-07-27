# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a MotiveWave study that analyzes overnight market structure and exports actionable trading context scores at 8:30 AM ET daily. The system calculates overnight VWAP, volume profile metrics (POC, VAH, VAL), and generates composite context scores ranging from -10 (strong bearish) to +10 (strong bullish).

## Build and Development Commands

### Basic Build Process
```bash
# Clean and compile
mvn clean compile

# Run tests (when implemented)
mvn test

# Package the JAR for MotiveWave
mvn package

# Deploy to MotiveWave (WSL environment)
cp target/overnight-context-analyzer-1.0.0.jar "/mnt/c/Users/$(whoami)/MotiveWave Extensions/"

# Deploy with timestamp (using existing script)
./deploy.sh
```

### Development Environment
- Java 21 with preview features enabled
- MotiveWave SDK (June 2023) - local JAR in `lib/` directory
- Maven build system
- WSL development environment targeting Windows MotiveWave installation

## Architecture Overview

### Core Processing Pipeline
1. **Session Detection** (`SessionDetector`) - Identifies overnight sessions (6:00 PM - 9:30 AM ET)
2. **VWAP Calculation** (`VWAPCalculator`) - Incremental volume-weighted average price with standard deviation bands
3. **Volume Profile Analysis** (`VolumeProfileAnalyzer`) - Builds price-volume distribution, calculates POC/VAH/VAL
4. **Context Scoring** (`ContextScorer`) - Generates composite scores from multiple components with configurable weighting
5. **Automated Export** (`CSVExporter` + `ExportScheduler`) - Daily CSV export at 8:30 AM ET

### Key Design Patterns
- **Builder Pattern**: `MarketContext.Builder` for complex object construction
- **Record Classes**: Immutable data models (`MarketContext`, `SessionBoundary`)
- **Strategy Pattern**: `ScoringMethod` enum for different scoring algorithms
- **Incremental Updates**: VWAP and volume profile calculations avoid full recalculation

### Data Flow Architecture
```
DataSeries → SessionDetector → VWAPCalculator → VolumeProfileAnalyzer → ContextScorer → CSVExporter
     ↓              ↓                ↓                    ↓               ↓           ↓
TimeUtils    SessionBoundary   Cumulative Values   VolumeProfile   MarketContext   CSV Files
```

### Critical Implementation Details

#### Session Management
- Overnight sessions span 6:00 PM (previous day) to 9:30 AM ET
- Uses Eastern Time zone exclusively (`America/New_York`)
- Session boundaries trigger VWAP calculation resets

#### Volume Profile Calculation
- Price levels rounded to instrument tick size (default 0.25 for ES)
- Volume distributed using Gaussian weighting centered on bar close
- Value Area calculated as 70% of total volume around POC

#### Context Scoring Components
- **VWAP Position**: Price relative to overnight VWAP in ATR units
- **Value Area Position**: Price location within/outside VAH-VAL range  
- **POC Proximity**: Distance from Point of Control in ATR units
- **Volume Balance**: Upper vs lower half volume distribution

#### Data Storage Keys
```java
// VWAP Calculator
"ON_VWAP", "VWAP_UP", "VWAP_LO", "CUM_TPV", "CUM_VOL"

// Volume Profile
"VP_POC", "VP_VAH", "VP_VAL", "VP_BAL"

// Context Scoring
"CONTEXT_SCORE", "ATR"
```

### MotiveWave Integration Specifics
- Study appears under "Custom Studies" → "Market Structure"
- Three settings tabs: General (VWAP/ATR), Export (CSV), Scoring (method selection)
- Real-time calculation via `onBarUpdate()` method
- Uses MotiveWave's DataSeries for persistent value storage

### Export System
- CSV files named: `overnight_context_YYYYMMDD.csv`
- Default export directory: `C:\TradingData\OvernightContext`
- Export triggered once daily with 5-minute window around scheduled time
- Headers include all context components and derived metrics

### Time Zone Handling
All time calculations use Eastern Time with proper DST handling. The `TimeUtils` class centralizes timezone conversions and market date calculations (overnight sessions belong to the next trading day).

### Error Handling Patterns
- Graceful degradation when volume data unavailable
- Zero/null value checks in all calculations
- File I/O exceptions handled with logging in export system
- Session detection fallbacks for edge cases