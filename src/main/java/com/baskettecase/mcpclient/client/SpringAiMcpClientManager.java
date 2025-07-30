package com.baskettecase.mcpclient.client;

import com.baskettecase.mcpclient.config.DefaultServerConfigService;
import com.baskettecase.mcpclient.config.YamlConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.mcp.SyncMcpToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Spring AI MCP Client Manager
 * 
 * Proper implementation using Spring AI 1.0.0 MCP Client functionality.
 * This follows the official Spring AI MCP Client documentation and examples.
 */
@Service
public class SpringAiMcpClientManager {

    private static final Logger logger = LoggerFactory.getLogger(SpringAiMcpClientManager.class);

    private final DefaultServerConfigService defaultServerConfigService;
    private final YamlConfigService yamlConfigService;
    private final Environment environment;
    
    // Spring AI MCP Client components - injected when available
    private SyncMcpToolCallbackProvider toolCallbackProvider;
    
    private String currentServerName;

    @Autowired
    public SpringAiMcpClientManager(DefaultServerConfigService defaultServerConfigService, 
                                   YamlConfigService yamlConfigService,
                                   Environment environment) {
        this.defaultServerConfigService = defaultServerConfigService;
        this.yamlConfigService = yamlConfigService;
        this.environment = environment;
    }


    /**
     * Set the tool callback provider (injected when available)
     */
    @Autowired(required = false)
    public void setToolCallbackProvider(SyncMcpToolCallbackProvider toolCallbackProvider) {
        this.toolCallbackProvider = toolCallbackProvider;
        if (toolCallbackProvider != null) {
            logger.info("Spring AI MCP Tool Callback Provider is now available");
        }
    }

    /**
     * Connect to an MCP server by adding it to Spring configuration
     */
    public boolean connect(String serverName, String jarPath) {
        return connect(serverName, jarPath, false);
    }
    
    /**
     * Connect to an MCP server with optional save as default
     */
    public boolean connect(String serverName, String jarPath, boolean saveAsDefault) {
        logger.info("Configuring MCP server: {} using JAR: {}", serverName, jarPath);
        
        try {
            // Save as default if requested
            if (saveAsDefault) {
                boolean saved = defaultServerConfigService.saveDefaultServer(serverName, jarPath);
                if (saved) {
                    logger.info("Saved {} as default server", serverName);
                } else {
                    logger.warn("Failed to save {} as default server", serverName);
                }
            }
            
            // Check if Spring AI MCP Client has this server configured
            boolean hasActiveConnection = checkForServerConnection(serverName);
            
            if (hasActiveConnection) {
                logger.debug("✓ Spring AI MCP Client already has active connection to: {}", serverName);
                currentServerName = serverName;
                return true;
            } else {
                logger.debug("⚠ Server '{}' not found in Spring AI MCP Client connections", serverName);
                
                // Still consider this a "successful" connection for CLI purposes
                currentServerName = serverName;
                return true;
            }
            
        } catch (Exception e) {
            logger.error("Failed to configure MCP server: {} with JAR: {}", serverName, jarPath, e);
            return false;
        }
    }

    /**
     * Disconnect from the current MCP server
     */
    public void disconnect() {
        if (currentServerName == null) {
            logger.info("No active connection to disconnect");
            return;
        }

        logger.info("Disconnecting from MCP server: {}", currentServerName);
        currentServerName = null;
        logger.info("Disconnected successfully");
    }

    /**
     * Get current connection status
     */
    public ConnectionState getConnectionState() {
        return isConnected() ? ConnectionState.CONNECTED : ConnectionState.DISCONNECTED;
    }

    /**
     * Get current server name
     */
    public String getCurrentServerName() {
        return currentServerName;
    }

    /**
     * Check if currently connected to a server
     */
    public boolean isConnected() {
        return currentServerName != null;
    }

    /**
     * Check if MCP client is enabled and available
     */
    public boolean isMcpEnabled() {
        // Check if running in no-server profile
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if ("no-server".equals(profile)) {
                return false;
            }
        }
        
        // Check if MCP client is explicitly disabled
        Boolean mcpEnabled = environment.getProperty("spring.ai.mcp.client.enabled", Boolean.class, true);
        return mcpEnabled && toolCallbackProvider != null;
    }

    /**
     * List all available tools from the connected MCP server
     */
    public List<String> listToolNames() {
        if (!isConnected()) {
            throw new IllegalStateException("Not connected to any MCP server");
        }

        try {
            if (toolCallbackProvider != null) {
                ToolCallback[] callbacks = toolCallbackProvider.getToolCallbacks();
                List<String> toolNames = new ArrayList<>();
                
                logger.debug("Found {} tool callbacks from Spring AI MCP Client", callbacks.length);
                
                for (ToolCallback callback : callbacks) {
                    try {
                        // Extract tool name from the callback
                        String toolName = extractToolNameFromCallback(callback);
                        if (toolName != null && !toolName.isEmpty()) {
                            toolNames.add(toolName);
                            logger.debug("Found tool: {}", toolName);
                        }
                    } catch (Exception e) {
                        logger.debug("Error processing tool callback: {}", e.getMessage());
                    }
                }
                
                if (!toolNames.isEmpty()) {
                    return toolNames;
                }
            }
            
            logger.warn("No Spring AI MCP Client tools available. Server may not be properly configured.");
            return Collections.emptyList();
            
        } catch (Exception e) {
            logger.error("Failed to list tools from MCP server", e);
            throw new RuntimeException("Failed to list tools: " + e.getMessage(), e);
        }
    }

    /**
     * Get detailed description of a tool
     */
    public String getToolDescription(String toolName) {
        if (!isConnected()) {
            throw new IllegalStateException("Not connected to any MCP server");
        }

        try {
            if (toolCallbackProvider != null) {
                ToolCallback[] callbacks = toolCallbackProvider.getToolCallbacks();
                for (ToolCallback callback : callbacks) {
                    String cleanedName = extractToolNameFromCallback(callback);
                    String fullName = callback.getToolDefinition().name();
                    
                    // Match either the cleaned name or the full name
                    if (toolName.equals(cleanedName) || toolName.equals(fullName)) {
                        ToolDefinition toolDefinition = callback.getToolDefinition();
                        if (toolDefinition != null) {
                            return String.format("Tool: %s\nDescription: %s\nInput Schema: %s", 
                                cleanedName,  // Show cleaned name in description
                                toolDefinition.description(), 
                                toolDefinition.inputSchema());
                        } else {
                            return String.format("Tool: %s\nDescription: MCP tool available via Spring AI Client\nCallback: %s", 
                                cleanedName, callback.getClass().getSimpleName());
                        }
                    }
                }
            }
            
            return "Tool not found or Spring AI MCP Client not properly configured";
            
        } catch (Exception e) {
            logger.error("Failed to get tool description for: {}", toolName, e);
            return "Error retrieving tool description: " + e.getMessage();
        }
    }

    /**
     * Execute a tool with the given parameters using direct ToolCallback execution
     * This provides direct tool execution without LLM involvement
     */
    public String executeTool(String toolName, Map<String, Object> parameters) {
        if (!isConnected()) {
            throw new IllegalStateException("Not connected to any MCP server");
        }

        try {
            if (toolCallbackProvider == null) {
                return "Spring AI MCP Client not available. Please ensure proper configuration.";
            }
            
            // Find the specific tool callback by matching either cleaned name or full name
            ToolCallback[] callbacks = toolCallbackProvider.getToolCallbacks();
            ToolCallback targetCallback = null;
            String matchedToolName = null;
            
            for (ToolCallback callback : callbacks) {
                String cleanedName = extractToolNameFromCallback(callback);
                String fullName = callback.getToolDefinition().name();
                
                // Match either the cleaned name or the full name
                if (toolName.equals(cleanedName) || toolName.equals(fullName)) {
                    targetCallback = callback;
                    matchedToolName = cleanedName;
                    break;
                }
            }
            
            if (targetCallback == null) {
                return "Tool not found: " + toolName;
            }
            
            try {
                // Convert parameters to JSON string for the callback
                String jsonParams = convertParametersToJson(parameters);
                
                // Execute the tool directly via the callback
                // This is the "user-controlled tool execution" approach from Spring AI docs
                Object result = targetCallback.call(jsonParams);
                
                return result != null ? result.toString() : "Tool executed successfully (no result)";
                
            } catch (Exception e) {
                logger.error("Error executing tool directly: {}", matchedToolName, e);
                return "Error executing tool: " + e.getMessage();
            }
            
        } catch (Exception e) {
            logger.error("Failed to execute tool: {} with parameters: {}", toolName, parameters, e);
            throw new RuntimeException("Failed to execute tool: " + e.getMessage(), e);
        }
    }

    /**
     * Execute tools via natural language using ChatModel (Future Enhancement)
     * This is a placeholder for future LLM integration using the user-controlled tool execution pattern
     * 
     * @param naturalLanguageRequest The user's natural language request
     * @return The final response after tool execution
     */
    public String executeToolsViaLLM(String naturalLanguageRequest) {
        if (!isConnected()) {
            throw new IllegalStateException("Not connected to any MCP server");
        }
        
        if (toolCallbackProvider == null) {
            return "Spring AI MCP Client not available. Please ensure proper configuration.";
        }
        
        // Future implementation will use the user-controlled tool execution pattern:
        // 1. Create ToolCallingChatOptions with internalToolExecutionEnabled=false
        // 2. Call ChatModel with the user's natural language request
        // 3. Check ChatResponse.hasToolCalls() in a loop
        // 4. Use ToolCallingManager.executeToolCalls() for each tool call
        // 5. Continue until no more tool calls are needed
        // 6. Return the final response
        
        return "LLM integration not yet implemented. Use direct tool execution via 'invoke-tool' command.";
    }

    /**
     * Get server information and capabilities
     */
    public String getServerInfo() {
        if (!isConnected()) {
            return "Not connected to any server";
        }

        try {
            List<String> toolNames = listToolNames();
            String implementationStatus = getImplementationStatus();

            return String.format(
                "Connected to: %s%n" +
                "Available Tools: %d%n" +
                "Tools: %s%n" +
                "Connection State: %s%n" +
                "Implementation: %s",
                currentServerName,
                toolNames.size(),
                toolNames.isEmpty() ? "None (check configuration)" : String.join(", ", toolNames),
                getConnectionState(),
                implementationStatus
            );
            
        } catch (Exception e) {
            logger.error("Failed to get server info", e);
            return String.format(
                "Connected to: %s%n" +
                "Connection State: %s%n" +
                "Error getting server info: %s",
                currentServerName,
                getConnectionState(),
                e.getMessage()
            );
        }
    }

    /**
     * Connect to the default server if one is configured
     */
    public boolean connectToDefaultServer() {
        var defaultConfig = defaultServerConfigService.loadDefaultServer();
        if (defaultConfig.isPresent()) {
            var config = defaultConfig.get();
            logger.info("Connecting to default server: {} -> {}", config.serverName(), config.jarPath());
            return connect(config.serverName(), config.jarPath());
        } else {
            logger.info("No default server configuration found");
            return false;
        }
    }
    
    /**
     * Check if a default server is configured
     */
    public boolean hasDefaultServer() {
        return defaultServerConfigService.hasDefaultServer();
    }
    
    /**
     * Get the default server configuration
     */
    public Optional<DefaultServerConfigService.DefaultServerConfig> getDefaultServerConfig() {
        return defaultServerConfigService.loadDefaultServer();
    }
    
    /**
     * Remove the default server configuration
     */
    public boolean removeDefaultServer() {
        return defaultServerConfigService.removeDefaultServer();
    }
    
    /**
     * Get the path to the default server configuration file
     */
    public String getDefaultServerConfigPath() {
        return defaultServerConfigService.getConfigFilePath();
    }

    /**
     * Shutdown hook to ensure clean disconnect
     */
    public void shutdown() {
        logger.info("Shutting down MCP client manager");
        
        if (isConnected()) {
            disconnect();
        }
        
        currentServerName = null;
        logger.info("MCP client manager shutdown complete");
    }

    // Private helper methods

    private boolean checkForServerConnection(String serverName) {
        try {
            if (toolCallbackProvider != null) {
                ToolCallback[] callbacks = toolCallbackProvider.getToolCallbacks();
                return callbacks != null && callbacks.length > 0;
            }
            return false;
        } catch (Exception e) {
            logger.debug("Error checking for server connection: {}", e.getMessage());
            return false;
        }
    }

    private String extractToolNameFromCallback(ToolCallback callback) {
        try {
            // Use the proper Spring AI approach: get the ToolDefinition and extract the name
            ToolDefinition toolDefinition = callback.getToolDefinition();
            if (toolDefinition != null) {
                String fullToolName = toolDefinition.name();
                if (fullToolName != null && !fullToolName.isEmpty()) {
                    // Clean up the tool name by removing Spring AI MCP prefixes
                    String cleanedName = cleanToolName(fullToolName);
                    logger.debug("Extracted tool name '{}' -> cleaned to '{}'", fullToolName, cleanedName);
                    return cleanedName;
                }
            }
            
            // Fallback: use the class name or hash
            String fallbackName = "tool_" + System.identityHashCode(callback);
            logger.debug("Using fallback tool name: {}", fallbackName);
            return fallbackName;
            
        } catch (Exception e) {
            logger.debug("Error extracting tool name from callback: {}", e.getMessage());
            return "unknown_tool";
        }
    }

    /**
     * Clean up Spring AI MCP generated tool names by removing predictable prefixes
     * Pattern: [client-name]_[server-name]_[method-name] -> [method-name]
     */
    private String cleanToolName(String fullToolName) {
        try {
            // Spring AI MCP generates names like: generic_mcp_client_generic_getHello
            // We want to extract just: getHello
            
            // Split by underscores and take the last part (method name)
            String[] parts = fullToolName.split("_");
            if (parts.length >= 3) {
                // Return the last part which should be the actual method name
                String methodName = parts[parts.length - 1];
                logger.debug("Cleaning tool name: '{}' -> '{}'", fullToolName, methodName);
                return methodName;
            }
            
            // If the pattern doesn't match expected format, return as-is
            logger.debug("Tool name '{}' doesn't match expected pattern, using as-is", fullToolName);
            return fullToolName;
            
        } catch (Exception e) {
            logger.debug("Error cleaning tool name '{}': {}", fullToolName, e.getMessage());
            return fullToolName;
        }
    }

    private String convertParametersToJson(Map<String, Object> parameters) {
        if (parameters.isEmpty()) {
            return "{}";
        }
        
        StringBuilder json = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            if (!first) {
                json.append(",");
            }
            json.append("\"").append(entry.getKey()).append("\":\"").append(entry.getValue()).append("\"");
            first = false;
        }
        json.append("}");
        return json.toString();
    }

    private String getImplementationStatus() {
        if (toolCallbackProvider != null) {
            return "Spring AI MCP Client (Tool Callbacks Available)";
        } else {
            return "Spring AI MCP Client (Configuration Required)";
        }
    }

    private void printServerConfiguration(String serverName, String jarPath) {
        logger.warn("spring:");
        logger.warn("  ai:");
        logger.warn("    mcp:");
        logger.warn("      client:");
        logger.warn("        enabled: true");
        logger.warn("        type: SYNC");
        logger.warn("        stdio:");
        logger.warn("          connections:");
        logger.warn("            {}:", serverName);
        logger.warn("              command: java");
        logger.warn("              args:");
        logger.warn("                - -Dlogging.level.root=OFF");
        logger.warn("                - -Dspring.main.banner-mode=off");
        logger.warn("                - -Dspring.main.log-startup-info=false");
        logger.warn("                - -jar");
        logger.warn("                - {}", jarPath);
    }
}