#!/bin/bash

# MCP Client Restart Wrapper Script
# This script handles automatic restarts when the application exits with code 42

JAR_PATH="target/generic-mcp-client-1.0.0.jar"
MAX_RESTARTS=3
RESTART_COUNT=0

echo "Starting Generic MCP Client with auto-restart support..."
echo "Press Ctrl+C to exit permanently"
echo

while [ $RESTART_COUNT -lt $MAX_RESTARTS ]; do
    # Run the MCP client
    java -jar "$JAR_PATH"
    EXIT_CODE=$?
    
    # Check if exit code indicates restart request
    if [ $EXIT_CODE -eq 42 ]; then
        RESTART_COUNT=$((RESTART_COUNT + 1))
        echo
        echo "🔄 Restart requested (attempt $RESTART_COUNT/$MAX_RESTARTS)..."
        echo "Restarting in 2 seconds..."
        sleep 2
        echo
    else
        # Normal exit or error - don't restart
        echo "Application exited with code $EXIT_CODE"
        break
    fi
done

if [ $RESTART_COUNT -eq $MAX_RESTARTS ]; then
    echo "⚠ Maximum restart attempts reached. Exiting."
fi

echo "Generic MCP Client stopped."
