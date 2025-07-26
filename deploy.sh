#!/bin/bash

# MotiveWave Studies - Complete Build & Deploy Script
# This script cleans, compiles, timestamps, and deploys studies to MotiveWave

set -e  # Exit on any error

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
PROJECT_NAME="overnight-context-analyzer"
MOTIVEWAVE_DIR="/mnt/c/Users/$(whoami)/MotiveWave Extensions"
TARGET_DIR="target"

# Helper functions
print_step() {
    echo -e "${BLUE}🔄 $1${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_info() {
    echo -e "${YELLOW}📌 $1${NC}"
}

# Check if we're in the right directory
if [[ ! -f "pom.xml" ]]; then
    print_error "pom.xml not found! Please run this script from the project root directory."
    exit 1
fi

# Check if MotiveWave Extensions directory exists
if [[ ! -d "$MOTIVEWAVE_DIR" ]]; then
    print_error "MotiveWave Extensions directory not found: $MOTIVEWAVE_DIR"
    print_info "Please check if MotiveWave is installed and the path is correct."
    exit 1
fi

echo "🚀 MotiveWave Studies - Build & Deploy Script"
echo "=============================================="

# Step 1: Clean previous builds
print_step "Cleaning previous builds..."
mvn clean -q
if [[ $? -eq 0 ]]; then
    print_success "Project cleaned successfully"
else
    print_error "Clean failed"
    exit 1
fi

# Step 2: Compile and package
print_step "Compiling and packaging studies..."
mvn package -q
if [[ $? -eq 0 ]]; then
    print_success "Compilation successful"
else
    print_error "Compilation failed"
    exit 1
fi

# Step 3: Generate timestamp and create new filename
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
ORIGINAL_JAR="$TARGET_DIR/$PROJECT_NAME-1.0.0.jar"
TIMESTAMPED_JAR="$TARGET_DIR/$PROJECT_NAME-$TIMESTAMP.jar"

# Check if original JAR exists
if [[ ! -f "$ORIGINAL_JAR" ]]; then
    print_error "Built JAR file not found: $ORIGINAL_JAR"
    exit 1
fi

# Step 4: Rename with timestamp
print_step "Creating timestamped JAR file..."
cp "$ORIGINAL_JAR" "$TIMESTAMPED_JAR"
if [[ $? -eq 0 ]]; then
    print_success "Created: $(basename "$TIMESTAMPED_JAR")"
else
    print_error "Failed to create timestamped JAR"
    exit 1
fi

# Step 5: Get file size for display
FILE_SIZE=$(du -h "$TIMESTAMPED_JAR" | cut -f1)

# Step 6: Copy to MotiveWave Extensions directory
print_step "Deploying to MotiveWave Extensions..."
cp "$TIMESTAMPED_JAR" "$MOTIVEWAVE_DIR/"
if [[ $? -eq 0 ]]; then
    print_success "Successfully deployed to MotiveWave Extensions"
else
    print_error "Failed to deploy to MotiveWave Extensions"
    exit 1
fi

# Step 7: Clean up old JAR files in MotiveWave directory (optional)
print_step "Cleaning up old study files..."
OLD_FILES=$(find "$MOTIVEWAVE_DIR" -name "$PROJECT_NAME-*.jar" -not -name "$(basename "$TIMESTAMPED_JAR")" 2>/dev/null | wc -l)
if [[ $OLD_FILES -gt 0 ]]; then
    print_warning "Found $OLD_FILES old study file(s)"
    echo "Do you want to remove old study files? (y/N): "
    read -r REMOVE_OLD
    if [[ $REMOVE_OLD =~ ^[Yy]$ ]]; then
        find "$MOTIVEWAVE_DIR" -name "$PROJECT_NAME-*.jar" -not -name "$(basename "$TIMESTAMPED_JAR")" -delete 2>/dev/null
        print_success "Old study files removed"
    else
        print_info "Old files kept (you may want to remove them manually)"
    fi
fi

# Step 8: Display summary
echo ""
echo "=============================================="
echo -e "${GREEN}🎉 DEPLOYMENT SUCCESSFUL${NC}"
echo "=============================================="
echo "📁 Project: $PROJECT_NAME"
echo "📦 JAR File: $(basename "$TIMESTAMPED_JAR")"
echo "📏 File Size: $FILE_SIZE"
echo "🎯 Deployed to: $MOTIVEWAVE_DIR"
echo "⏰ Timestamp: $TIMESTAMP"
echo ""

# Step 9: List studies in the JAR
print_step "Studies included in this build:"
jar -tf "$TIMESTAMPED_JAR" | grep '\.class$' | grep -v '\$' | sed 's|/|.|g' | sed 's|\.class$||' | grep '^com\.tradingtools\.' | sed 's|^com\.tradingtools\.||' | sort
echo ""

# Step 10: Final instructions
print_info "Next steps:"
echo "   1. Close MotiveWave completely (if running)"
echo "   2. Restart MotiveWave"
echo "   3. Go to Study > All Studies to find your custom studies"
echo ""
print_warning "Remember: MotiveWave loads extensions only on startup!"

# Step 11: Show MotiveWave Extensions directory contents
echo ""
print_step "Current MotiveWave Extensions directory contents:"
ls -la "$MOTIVEWAVE_DIR"/*.jar 2>/dev/null | awk '{print "   " $9 " (" $5 " bytes)"}'

echo ""
print_success "Deployment completed successfully! 🚀"