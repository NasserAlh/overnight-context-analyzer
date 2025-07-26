# Overnight Context Analyzer Architecture

## Overview

The Overnight Context Analyzer is a MotiveWave study that processes real-time market data to calculate comprehensive overnight trading context scores. The system uses a modular architecture with clear separation of concerns and well-defined data flows.

## Class Hierarchy & Relationships

### Core Study Class
- **`OvernightContextAnalyzer`** - Main MotiveWave study class that orchestrates all components

### Calculation Components
- **`VWAPCalculator`** - Calculates overnight VWAP with standard deviation bands
- **`VolumeProfileAnalyzer`** - Builds price-volume distribution and calculates POC/VAH/VAL
- **`ContextScorer`** - Generates composite context scores from multiple market factors

### Data Models
- **`MarketContext`** - Immutable record containing complete analysis results
- **`VolumeProfile`** - Encapsulates volume distribution data and statistics
- **`SessionBoundary`** - Defines session time boundaries and indices

### Utility Classes
- **`SessionDetector`** - Identifies overnight session boundaries
- **`TimeUtils`** - Timezone handling and market time calculations

### Export System
- **`CSVExporter`** - Handles CSV file output formatting
- **`ExportScheduler`** - Manages automated daily export timing

## Component Interactions

### 1. Main Processing Flow

```
OvernightContextAnalyzer.calculate()
├── SessionDetector.isSessionBoundary()
├── VWAPCalculator.updateVWAP()
├── VolumeProfileAnalyzer.buildProfile()
├── ContextScorer.calculateContext()
└── CSVExporter.exportContext()
```

### 2. Data Dependencies

```
DataSeries (MotiveWave) → SessionDetector → SessionBoundary
                       ↓
                    VWAPCalculator → VWAP values
                       ↓
                VolumeProfileAnalyzer → VolumeProfile
                       ↓
                  ContextScorer → MarketContext
                       ↓
                   CSVExporter → CSV files
```

### 3. Component Relationships

#### OvernightContextAnalyzer (Main Orchestrator)
- **Creates and manages**: All component instances
- **Calls**: `SessionDetector`, `VWAPCalculator`, `VolumeProfileAnalyzer`, `ContextScorer`, `CSVExporter`
- **Stores data in**: MotiveWave DataSeries using predefined keys
- **Triggers**: Automated export via `ExportScheduler`

#### VWAPCalculator 
- **Receives**: Price/volume data from DataSeries
- **Calculates**: Incremental VWAP, standard deviation bands
- **Stores**: Cumulative TPV/Volume in DataSeries for efficiency
- **Provides**: VWAP values to ContextScorer

#### VolumeProfileAnalyzer
- **Receives**: Price/volume data from DataSeries  
- **Creates**: VolumeProfile objects with POC/VAH/VAL calculations
- **Uses**: Gaussian distribution for volume allocation across price levels
- **Provides**: Volume profile data to ContextScorer

#### ContextScorer
- **Receives**: Current price, VWAP, VolumeProfile, ATR
- **Applies**: Configurable scoring algorithms (BALANCED, VWAP_HEAVY, VOLUME_HEAVY)
- **Calculates**: Individual component scores and weighted composite
- **Creates**: MarketContext objects with complete analysis

#### SessionDetector
- **Monitors**: Market time transitions (6PM-9:30AM ET overnight sessions)
- **Identifies**: Session boundaries for VWAP calculation resets
- **Handles**: Eastern Time with DST support
- **Provides**: SessionBoundary objects with time ranges and indices

#### Export System
- **CSVExporter**: Formats MarketContext data to CSV with headers
- **ExportScheduler**: Triggers daily export at 8:30 AM ET
- **Coordinates**: File naming, directory management, duplicate prevention

## Data Flow Architecture

### 1. Initialization Phase
```
OvernightContextAnalyzer.initialize()
├── Configure MotiveWave settings (tabs, descriptors)
├── Setup runtime paths for charting
└── Prepare for calculate() callbacks
```

### 2. Real-Time Processing Phase
```
MotiveWave calls calculate(index, DataContext)
├── Check session boundary (SessionDetector)
├── Calculate ATR for scoring
├── Update VWAP (incremental calculation)
├── Build volume profile (session range)
├── Score market context (weighted components)
├── Store values in DataSeries
└── Check export trigger (time-based)
```

### 3. Export Phase
```
ExportScheduler.shouldExport() triggers
├── Gather current session data
├── Create final MarketContext
├── Format as CSV row
├── Append to daily file
└── Mark as exported (prevent duplicates)
```

## Key Design Patterns

### Builder Pattern
- **MarketContext.Builder** - Complex object construction with validation
- Enables flexible, readable context creation
- Ensures immutable final objects

### Strategy Pattern  
- **ScoringMethod enum** - Different weighting algorithms
- Allows runtime scoring method selection
- Easy to extend with new scoring approaches

### Incremental Processing
- **VWAPCalculator** - Avoids full recalculation each bar
- **VolumeProfile** - Builds progressively during session  
- Optimizes performance for real-time processing

### Immutable Data Models
- **MarketContext** and **VolumeProfile** records
- Thread-safe, predictable state management
- Clear data contracts between components

## Data Storage Strategy

### MotiveWave DataSeries Keys
```java
// VWAP Calculator
"ON_VWAP"     - Overnight VWAP value
"VWAP_UP"     - Upper standard deviation band  
"VWAP_LO"     - Lower standard deviation band
"CUM_TPV"     - Cumulative Total Price * Volume
"CUM_VOL"     - Cumulative Volume

// Volume Profile  
"VP_POC"      - Point of Control price
"VP_VAH"      - Value Area High
"VP_VAL"      - Value Area Low
"VP_BAL"      - Volume balance ratio

// Context Scoring
"CONTEXT_SCORE" - Final composite score (-10 to +10)
"ATR"           - Average True Range for normalization
```

### Memory Management
- Components store minimal state (just configuration)
- Heavy calculations use DataSeries for persistence
- Session boundaries trigger cleanup/reset cycles

## Error Handling & Resilience

### Data Validation
- **Bar validation** - Checks for valid OHLCV data
- **Null safety** - Handles missing DataSeries values
- **Range checks** - Validates indices and time boundaries

### Graceful Degradation  
- **Missing volume** - Falls back to price-only calculations
- **Invalid sessions** - Uses fallback time boundaries
- **Export failures** - Logs errors but continues processing

### Session Management
- **Time zone handling** - Robust DST transitions
- **Session detection** - Multiple fallback strategies
- **State recovery** - Rebuilds from DataSeries on restart

## Configuration & Extensibility

### MotiveWave Settings Integration
- **General Tab** - VWAP bands, ATR period
- **Export Tab** - File paths, scheduling
- **Scoring Tab** - Algorithm selection

### Extension Points
- **New scoring methods** - Add to ScoringMethod enum
- **Additional metrics** - Extend MarketContext record  
- **Export formats** - Implement additional exporters
- **Session types** - Extend SessionDetector logic

This architecture provides a robust, maintainable foundation for overnight market analysis while remaining flexible for future enhancements.