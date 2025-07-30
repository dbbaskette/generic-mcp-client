#!/bin/bash

# Generic MCP Client Launcher Script
# Usage: ./mcp-client.sh [OPTIONS] [maven-args...]
# 
# OPTIONS:
#   --no-server              Start client without auto-starting any MCP server
#   --profile <profile-name> Start with specific server profile
#   --help, -h               Show this help message
#
# Examples:
#   ./mcp-client.sh --no-server              # Clean mode
#   ./mcp-client.sh --profile test4          # Load test4 server
#   ./mcp-client.sh --profile generic-server # Load generic server
#   ./mcp-client.sh                          # Interactive profile selection

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

# Show help message
show_help() {
    echo "Generic MCP Client Launcher Script"
    echo
    echo "Usage: ./mcp-client.sh [OPTIONS] [maven-args...]"
    echo
    echo "OPTIONS:"
    echo "  --no-server              Start client without auto-starting any MCP server"
    echo "  --profile <profile-name> Start with specific server profile"
    echo "  --help, -h               Show this help message"
    echo
    echo "Available Profiles:"
    echo "  no-server      Clean mode - no auto-connections"
    echo "  test4          Test4 MCP server configuration"
    echo "  generic-server Generic MCP server configuration"
    echo
    echo "Examples:"
    echo "  ./mcp-client.sh --no-server              # Clean mode"
    echo "  ./mcp-client.sh --profile test4          # Load test4 server"
    echo "  ./mcp-client.sh --profile generic-server # Load generic server"
    echo "  ./mcp-client.sh                          # Interactive profile selection"
    echo
}

# Parse command line arguments
NO_SERVER=false
PROFILE=""
MAVEN_ARGS=()

while [[ $# -gt 0 ]]; do
    case $1 in
        --no-server)
            NO_SERVER=true
            PROFILE="no-server"
            shift
            ;;
        --profile)
            PROFILE="$2"
            if [ -z "$PROFILE" ]; then
                print_error "Profile name required after --profile"
                exit 1
            fi
            shift 2
            ;;
        -h|--help)
            show_help
            exit 0
            ;;
        *)
            # All other arguments are passed to Maven
            MAVEN_ARGS+=("$1")
            shift
            ;;
    esac
done

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

# Function to discover available profiles
discover_profiles() {
    local profiles=()
    for file in src/main/resources/application-*.yml; do
        if [ -f "$file" ]; then
            local profile=$(basename "$file" .yml | sed 's/application-//')
            profiles+=("$profile")
        fi
    done
    echo "${profiles[@]}"
}

# Function to select profile interactively
select_profile() {
    local profiles=($(discover_profiles))
    
    if [ ${#profiles[@]} -eq 0 ]; then
        print_error "No profiles found in src/main/resources/"
        exit 1
    fi
    
    echo "Available profiles:"
    for i in "${!profiles[@]}"; do
        echo "  $((i+1)). ${profiles[i]}"
    done
    echo
    
    while true; do
        read -p "Select profile (1-${#profiles[@]}): " choice
        if [[ "$choice" =~ ^[0-9]+$ ]] && [ "$choice" -ge 1 ] && [ "$choice" -le ${#profiles[@]} ]; then
            PROFILE="${profiles[$((choice-1))]}"
            break
        else
            echo "Invalid choice. Please enter a number between 1 and ${#profiles[@]}."
        fi
    done
}

# Configure based on profile
if [ -z "$PROFILE" ]; then
    print_status "No profile specified - starting interactive selection..."
    select_profile
fi

# Validate profile exists
if [ ! -f "src/main/resources/application-${PROFILE}.yml" ]; then
    print_error "Profile '$PROFILE' not found (missing application-${PROFILE}.yml)"
    print_status "Available profiles: $(discover_profiles)"
    exit 1
fi

print_status "Starting Generic MCP Client with profile: $PROFILE"
MAVEN_ARGS+=("-Dspring-boot.run.profiles=$PROFILE")

print_status "Maven command: $MAVEN_CMD"

# Create logs directory if it doesn't exist
mkdir -p logs

# Run the application
# Pass processed arguments to Maven
exec "$MAVEN_CMD" spring-boot:run "${MAVEN_ARGS[@]}"