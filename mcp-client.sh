#!/bin/bash

# Generic MCP Client Launcher Script
# Usage: ./mcp-client [maven-args...]

set -e

# Script directory
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARN]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check if application.yml exists
if [ ! -f "src/main/resources/application.yml" ]; then
    print_warning "application.yml not found"
    if [ -f "src/main/resources/application.yml.template" ]; then
        print_status "Copying application.yml.template to application.yml"
        cp src/main/resources/application.yml.template src/main/resources/application.yml
        print_warning "Please edit src/main/resources/application.yml and update your server JAR path"
        print_warning "The file has been gitignored to keep your configuration secure"
    else
        print_error "application.yml.template not found!"
        exit 1
    fi
fi

# Check if Maven wrapper exists, otherwise use system maven
if [ -f "./mvnw" ]; then
    MAVEN_CMD="./mvnw"
else
    if ! command -v mvn &> /dev/null; then
        print_error "Maven not found! Please install Maven or add ./mvnw to the project"
        exit 1
    fi
    MAVEN_CMD="mvn"
    print_warning "Using system Maven (consider adding Maven wrapper with: mvn wrapper:wrapper)"
fi

print_status "Starting Generic MCP Client..."
print_status "Maven command: $MAVEN_CMD"

# Create logs directory if it doesn't exist
mkdir -p logs

# Run the application
# Pass any script arguments to Maven
exec "$MAVEN_CMD" spring-boot:run "$@"