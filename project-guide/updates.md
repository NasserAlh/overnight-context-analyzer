Thank you for the correction! You're right - MotiveWave extensions are stored in the user directory, not directly on C:\. Let me update all the relevant paths in the implementation document:

## Updated Path References

### 1. Build Process (Section 10.1)
```bash
# In WSL Ubuntu environment
cd ~/projects/overnight-context-analyzer

# Clean and compile
mvn clean compile

# Run tests
mvn test

# Package JAR
mvn package

# Copy to Windows MotiveWave Extensions directory
cp target/overnight-context-analyzer-1.0.0.jar "/mnt/c/Users/nasser/MotiveWave Extensions/"
```

### 2. Main Study Implementation - Default Export Path
In `OvernightContextAnalyzer.java`, update the default export path:

```java
private void configureExportSettings(Defaults defaults) {
    SettingTab tab = new SettingTab("Export");
    
    SettingGroup exportGroup = new SettingGroup("CSV Export");
    
    exportPathDesc = new FileDesc("exportPath", "Export Directory", 
        new File("C:\\Users\\nasser\\Documents\\TradingData\\OvernightContext"));
    exportPathDesc.setDirectory(true);
    
    // ... rest of configuration
}
```

And in the initialization:
```java
private void initializeComponents(DataContext ctx) {
    // ... other initialization
    
    // Initialize export system
    File exportPath = settings.getFile(exportPathDesc.getKey());
    if (exportPath == null) {
        exportPath = new File("C:\\Users\\nasser\\Documents\\TradingData\\OvernightContext");
    }
    csvExporter = new CSVExporter(exportPath);
    
    // ... rest of initialization
}
```

### 3. Deployment Guide (Section 10.2)
```markdown
### 10.2 Installation in MotiveWave

1. **Close MotiveWave** if running
2. **Copy JAR** to `C:\Users\nasser\MotiveWave Extensions\` directory
3. **Start MotiveWave**
4. **Add Study to Chart**:
   - Right-click on chart
   - Select "Study" → "Custom Studies" → "Market Structure"
   - Choose "Overnight Context Analyzer"
```

### 4. Configuration Section (10.3)
```markdown
2. **Export Tab**:
   - Export Directory: `C:\Users\nasser\Documents\TradingData\OvernightContext`
   - Auto Export: Enabled
   - Export Time: 8:30 AM ET
```

### 5. VS Code Configuration Update
For your VS Code settings in WSL, update the `.vscode/settings.json`:

```json
{
    "java.configuration.maven.userSettings": "/home/user/.m2/settings.xml",
    "java.runtime.required": "21",
    "motivewave.sdk.path": "/mnt/c/Users/nasser/MotiveWave Extensions/lib/mwave_sdk_062023.jar"
}
```

### 6. Troubleshooting Section Update
```markdown
1. **Study Not Appearing**:
   - Verify JAR in `C:\Users\nasser\MotiveWave Extensions\` directory
   - Check MotiveWave console for errors
   - Ensure correct SDK version (June 2023)
```

### 7. Quick Deployment Script
Create a deployment script `deploy.sh` in your project root:

```bash
#!/bin/bash
# deploy.sh - Deploy to MotiveWave Extensions

PROJECT_NAME="overnight-context-analyzer"
VERSION="1.0.0"
MW_EXTENSIONS="/mnt/c/Users/nasser/MotiveWave Extensions"

echo "Building $PROJECT_NAME v$VERSION..."

# Clean and build
mvn clean package

# Check if build succeeded
if [ $? -eq 0 ]; then
    echo "Build successful. Deploying to MotiveWave Extensions..."
    
    # Create backup of existing JAR if it exists
    if [ -f "$MW_EXTENSIONS/$PROJECT_NAME-$VERSION.jar" ]; then
        mv "$MW_EXTENSIONS/$PROJECT_NAME-$VERSION.jar" "$MW_EXTENSIONS/$PROJECT_NAME-$VERSION.jar.bak"
        echo "Backed up existing JAR"
    fi
    
    # Copy new JAR
    cp "target/$PROJECT_NAME-$VERSION.jar" "$MW_EXTENSIONS/"
    
    if [ $? -eq 0 ]; then
        echo "Deployment successful!"
        echo "JAR deployed to: $MW_EXTENSIONS/$PROJECT_NAME-$VERSION.jar"
        echo "Please restart MotiveWave to load the new extension."
    else
        echo "Failed to copy JAR to MotiveWave Extensions directory"
        exit 1
    fi
else
    echo "Build failed. Please check the error messages above."
    exit 1
fi
```

Make it executable:
```bash
chmod +x deploy.sh
```

Then you can deploy with a simple command:
```bash
./deploy.sh
```

This corrected path structure ensures the extension will be properly installed in your MotiveWave user directory where the platform expects to find custom studies and extensions.