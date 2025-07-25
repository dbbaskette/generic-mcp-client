package com.baskettecase.mcpclient.cli;

import com.baskettecase.mcpclient.client.SpringAiMcpClientManager;
import com.baskettecase.mcpclient.config.YamlConfigService;
import com.baskettecase.mcpclient.util.ParameterParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Main CLI interface for the MCP Client
 * 
 * Provides an interactive command-line interface for:
 * - Connecting to MCP servers via STDIO transport
 * - Discovering and listing available tools
 * - Executing tools directly with hybrid parameter parsing
 * - Managing server connections and state
 */
@Component
public class CliRunner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(CliRunner.class);
    
    private final Scanner scanner = new Scanner(System.in);
    private final String prompt;
    private volatile boolean running = true;
    
    private final SpringAiMcpClientManager clientManager;
    private final ParameterParser parameterParser;
    private final YamlConfigService yamlConfigService;

    public CliRunner(SpringAiMcpClientManager clientManager, ParameterParser parameterParser, YamlConfigService yamlConfigService) {
        this.prompt = "mcp-client> ";
        this.clientManager = clientManager;
        this.parameterParser = parameterParser;
        this.yamlConfigService = yamlConfigService;
        
        // Add shutdown hook for clean disconnect
        Runtime.getRuntime().addShutdownHook(new Thread(clientManager::shutdown));
    }

    @Override
    public void run(String... args) throws Exception {
        printWelcome();
        
        // Check for auto-connect to default server
        if (clientManager.hasDefaultServer()) {
            var defaultConfig = clientManager.getDefaultServerConfig();
            if (defaultConfig.isPresent()) {
                var config = defaultConfig.get();
                System.out.println("Found default server configuration:");
                System.out.println("  Name: " + config.serverName());
                System.out.println("  JAR: " + config.jarPath());
                System.out.println("  Saved: " + new java.util.Date(config.savedAt()));
                System.out.println();
                System.out.println("Connecting to default server...");
                
                boolean success = clientManager.connectToDefaultServer();
                if (success) {
                    System.out.println("✓ Successfully connected to default server: " + config.serverName());
                    
                    // Show available tools with clean output
                    try {
                        List<String> tools = clientManager.listToolNames();
                        System.out.println("✓ Discovered " + tools.size() + " tools:");
                        tools.forEach(toolName -> {
                            // Get basic description without full schema
                            String description = clientManager.getToolDescription(toolName);
                            String shortDescription = extractShortDescription(description);
                            System.out.println("  - " + toolName + ": " + shortDescription);
                        });
                        System.out.println("Use 'describe-tool <name>' for detailed parameter information.");
                    } catch (Exception e) {
                        System.out.println("⚠ Connected but failed to list tools: " + e.getMessage());
                    }
                    System.out.println();
                } else {
                    System.out.println("✗ Failed to connect to default server");
                    System.out.println("  You can connect manually using: connect " + config.serverName() + " stdio " + config.jarPath());
                    System.out.println();
                }
            }
        }
        
        startInteractiveMode();
    }

    private void printWelcome() {
        System.out.println();
        System.out.println("=== Generic MCP Client ===");
        System.out.println("Generic Model Context Protocol Client for testing and development");
        System.out.println();
        System.out.println("Available commands:");
        System.out.println("  connect <name> stdio <jar-path>     - Connect to MCP server");
        System.out.println("  connect <name> stdio <jar-path> --save-default - Connect and save as default");
        System.out.println("  list-tools                          - List available tools");
        System.out.println("  status                              - Show connection status");
        System.out.println("  describe-tool <tool-name>           - Show tool details");
        System.out.println("  invoke-tool <tool-name> [params...] - Execute tool");
        System.out.println("  show-default                        - Show default server configuration");
        System.out.println("  remove-default                      - Remove default server configuration");
        System.out.println("  generate-config                     - Generate application.yml configuration");
        System.out.println("  help                                - Show this help");
        System.out.println("  exit                                - Exit application");
        System.out.println();
        System.out.println("Parameter formats:");
        System.out.println("  Key-value pairs: key1=value1 key2=value2");
        System.out.println("  JSON (complex):  '{\"key\":\"value\",\"nested\":{\"data\":true}}'");
        System.out.println();
    }

    private void startInteractiveMode() {
        while (running) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            
            if (input.isEmpty()) {
                continue;
            }
            
            try {
                processCommand(input);
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage());
            }
        }
    }

    private void processCommand(String input) {
        String[] parts = input.split("\\s+", 2);
        String command = parts[0].toLowerCase();
        String args = parts.length > 1 ? parts[1] : "";

        switch (command) {
            case "connect" -> handleConnect(args);
            case "list-tools" -> handleListTools();
            case "status" -> handleStatus();
            case "describe-tool" -> handleDescribeTool(args);
            case "invoke-tool" -> handleInvokeTool(args);
            case "show-default" -> handleShowDefault();
            case "remove-default" -> handleRemoveDefault();
            case "generate-config" -> handleGenerateConfig();
            case "help" -> printWelcome();
            case "exit", "quit", "q" -> handleExit();
            default -> System.out.println("Unknown command: " + command + ". Type 'help' for available commands.");
        }
    }

    private void handleConnect(String args) {
        if (args.trim().isEmpty()) {
            System.out.println("Usage: connect <name> stdio <jar-path> [--save-default]");
            return;
        }

        String[] parts = args.split("\\s+");
        if (parts.length < 3 || !parts[1].equals("stdio")) {
            System.out.println("Usage: connect <name> stdio <jar-path> [--save-default]");
            System.out.println("Example: connect myserver stdio /path/to/server.jar");
            return;
        }

        String serverName = parts[0];
        String jarPath = parts[2];
        boolean saveDefault = parts.length > 3 && "--save-default".equals(parts[3]);

        System.out.println("Connecting to MCP server...");
        System.out.println("  Name: " + serverName);
        System.out.println("  JAR: " + jarPath);
        
        if (saveDefault) {
            System.out.println("  Will save as default server");
        }

        boolean success = clientManager.connect(serverName, jarPath, saveDefault);
        
        if (success) {
            System.out.println("✓ Successfully connected to server: " + serverName);
            
            if (saveDefault) {
                System.out.println("✓ Saved as default server");
                System.out.println("  Config file: " + clientManager.getDefaultServerConfigPath());
            }
            
            // Show available tools with clean output
            try {
                List<String> tools = clientManager.listToolNames();
                System.out.println("✓ Discovered " + tools.size() + " tools:");
                tools.forEach(toolName -> {
                    // Get basic description without full schema
                    String description = clientManager.getToolDescription(toolName);
                    String shortDescription = extractShortDescription(description);
                    System.out.println("  - " + toolName + ": " + shortDescription);
                });
                System.out.println("Use 'describe-tool <name>' for detailed parameter information.");
            } catch (Exception e) {
                System.out.println("⚠ Connected but failed to list tools: " + e.getMessage());
            }
        } else {
            System.out.println("✗ Failed to connect to server: " + serverName);
            System.out.println("  Check that the JAR path is correct and the server is compatible");
        }
    }

    private void handleListTools() {
        if (!clientManager.isConnected()) {
            System.out.println("✗ Not connected to any MCP server");
            System.out.println("  Use 'connect <name> stdio <jar-path>' to connect first");
            return;
        }

        try {
            List<String> tools = clientManager.listToolNames();
            
            if (tools.isEmpty()) {
                System.out.println("No tools available from server: " + clientManager.getCurrentServerName());
                return;
            }

            System.out.println("Available tools from server: " + clientManager.getCurrentServerName());
            System.out.println();
            
            for (int i = 0; i < tools.size(); i++) {
                String toolName = tools.get(i);
                String description = clientManager.getToolDescription(toolName);
                String shortDescription = extractShortDescription(description);
                
                System.out.printf("%d. %s%n", i + 1, toolName);
                System.out.printf("   Description: %s%n", shortDescription);
                System.out.println("   Use 'describe-tool " + toolName + "' for full parameter details");
                System.out.println();
            }
            
        } catch (Exception e) {
            System.out.println("✗ Failed to list tools: " + e.getMessage());
            logger.error("Error listing tools", e);
        }
    }

    private void handleStatus() {
        System.out.println("=== MCP Client Status ===");
        System.out.println();
        System.out.println(clientManager.getServerInfo());
        System.out.println();
    }

    private void handleDescribeTool(String args) {
        if (args.trim().isEmpty()) {
            System.out.println("Usage: describe-tool <tool-name>");
            return;
        }

        if (!clientManager.isConnected()) {
            System.out.println("✗ Not connected to any MCP server");
            System.out.println("  Use 'connect <name> stdio <jar-path>' to connect first");
            return;
        }

        String toolName = args.trim();
        
        try {
            String description = clientManager.getToolDescription(toolName);
            
            if ("Unknown tool".equals(description)) {
                System.out.println("✗ Tool not found: " + toolName);
                System.out.println("  Use 'list-tools' to see available tools");
                return;
            }

            System.out.println("=== Tool Details ===");
            System.out.println();
            System.out.println("Name: " + toolName);
            System.out.println("Description: " + description);
            System.out.println();
            System.out.println("Parameters: Basic implementation - parameter details will be available");
            System.out.println("           when full MCP integration is complete");
            System.out.println();
            System.out.println("Usage examples:");
            System.out.println("  invoke-tool " + toolName + " param1=value1 param2=value2");
            System.out.println("  invoke-tool " + toolName + " '{\"param1\":\"value1\",\"param2\":\"value2\"}'");
            System.out.println();
            
        } catch (Exception e) {
            System.out.println("✗ Failed to describe tool: " + e.getMessage());
            logger.error("Error describing tool: " + toolName, e);
        }
    }

    private void handleInvokeTool(String args) {
        if (args.trim().isEmpty()) {
            System.out.println("Usage: invoke-tool <tool-name> [parameters...]");
            System.out.println("Parameters can be:");
            System.out.println("  Key-value pairs: invoke-tool mytool param1=value1 param2=value2");
            System.out.println("  JSON format: invoke-tool mytool '{\"param1\":\"value1\",\"param2\":\"value2\"}'");
            System.out.println("  Interactive: invoke-tool mytool (prompts for parameters)");
            return;
        }

        if (!clientManager.isConnected()) {
            System.out.println("✗ Not connected to any MCP server");
            System.out.println("  Use 'connect <name> stdio <jar-path>' to connect first");
            return;
        }

        String[] parts = args.split("\\s+", 2);
        String toolName = parts[0];
        String[] paramArgs = parts.length > 1 ? parts[1].split("\\s+") : new String[0];

        try {
            // Check if tool exists
            String toolDescription = clientManager.getToolDescription(toolName);
            if (toolDescription.startsWith("Tool not found") || toolDescription.startsWith("Unknown tool")) {
                System.out.println("✗ Tool not found: " + toolName);
                System.out.println("  Use 'list-tools' to see available tools");
                return;
            }

            Map<String, Object> parameters;
            
            // If no parameters provided, check if tool has parameters and prompt interactively
            if (paramArgs.length == 0) {
                parameters = promptForParameters(toolName, toolDescription);
                if (parameters == null) {
                    return; // User cancelled or error occurred
                }
            } else {
                // Parse provided parameters
                try {
                    parameters = parameterParser.parseParameters(paramArgs);
                } catch (IllegalArgumentException e) {
                    System.out.println("✗ Invalid parameters: " + e.getMessage());
                    return;
                }
            }

            System.out.println("Executing tool: " + toolName);
            if (!parameters.isEmpty()) {
                System.out.println("Parameters: " + parameterParser.formatParameters(parameters));
            }
            System.out.println();

            // Execute the tool
            String result = clientManager.executeTool(toolName, parameters);

            System.out.println("=== Tool Result ===");
            System.out.println(result);
            System.out.println();

        } catch (Exception e) {
            System.out.println("✗ Failed to execute tool: " + e.getMessage());
            logger.error("Error executing tool: " + toolName, e);
        }
    }

    /**
     * Prompt user for tool parameters interactively based on the tool's schema
     */
    private Map<String, Object> promptForParameters(String toolName, String toolDescription) {
        try {
            // Parse the tool description to extract parameter information
            Map<String, ParameterInfo> parameterInfos = parseParameterSchema(toolDescription);
            
            if (parameterInfos.isEmpty()) {
                System.out.println("Tool '" + toolName + "' has no parameters.");
                return new HashMap<>();
            }

            System.out.println("=== Interactive Parameter Collection ===");
            System.out.println("Tool: " + toolName);
            System.out.println();

            Map<String, Object> parameters = new HashMap<>();
            
            for (Map.Entry<String, ParameterInfo> entry : parameterInfos.entrySet()) {
                String paramName = entry.getKey();
                ParameterInfo paramInfo = entry.getValue();
                
                String prompt = String.format("%s%s: %s", 
                    paramName,
                    paramInfo.required ? " (required)" : " (optional)",
                    paramInfo.description
                );
                
                System.out.print(prompt + "\n> ");
                String input = scanner.nextLine().trim();
                
                // Handle required parameters
                if (paramInfo.required && input.isEmpty()) {
                    System.out.println("✗ Required parameter cannot be empty. Operation cancelled.");
                    return null;
                }
                
                // Add parameter if not empty
                if (!input.isEmpty()) {
                    parameters.put(paramName, input);
                }
            }
            
            System.out.println();
            return parameters;
            
        } catch (Exception e) {
            System.out.println("✗ Failed to parse parameter schema: " + e.getMessage());
            logger.error("Error parsing parameter schema for tool: " + toolName, e);
            return null;
        }
    }

    /**
     * Parse the JSON schema from tool description to extract parameter information
     */
    private Map<String, ParameterInfo> parseParameterSchema(String toolDescription) {
        Map<String, ParameterInfo> parameters = new LinkedHashMap<>();
        
        try {
            // Extract JSON schema from the tool description
            int schemaStart = toolDescription.indexOf("Input Schema: ");
            if (schemaStart == -1) {
                return parameters; // No schema found
            }
            
            String schemaJson = toolDescription.substring(schemaStart + 14).trim();
            logger.debug("Parsing schema JSON: {}", schemaJson);
            
            // Better JSON parsing - look for complete parameter objects
            if (schemaJson.contains("\"properties\"")) {
                // Find the properties section
                int propertiesStart = schemaJson.indexOf("\"properties\":{") + 14;
                int propertiesEnd = findMatchingBrace(schemaJson, propertiesStart - 1);
                
                if (propertiesStart > 13 && propertiesEnd > propertiesStart) {
                    String propertiesSection = schemaJson.substring(propertiesStart, propertiesEnd);
                    logger.debug("Properties section: {}", propertiesSection);
                    
                    // Parse each parameter
                    String[] paramParts = propertiesSection.split("(?=\"[^\"]+\":\\{)");
                    
                    for (String paramPart : paramParts) {
                        if (paramPart.trim().isEmpty()) continue;
                        
                        // Extract parameter name
                        int nameStart = paramPart.indexOf("\"") + 1;
                        int nameEnd = paramPart.indexOf("\"", nameStart);
                        if (nameStart <= 0 || nameEnd <= nameStart) continue;
                        
                        String paramName = paramPart.substring(nameStart, nameEnd);
                        
                        // Extract description
                        String description = "No description available";
                        int descStart = paramPart.indexOf("\"description\":\"");
                        if (descStart >= 0) {
                            descStart += 15;
                            int descEnd = paramPart.indexOf("\"", descStart);
                            if (descEnd > descStart) {
                                description = paramPart.substring(descStart, descEnd);
                            }
                        }
                        
                        // Check if required
                        boolean isRequired = schemaJson.contains("\"required\":[") && 
                                           schemaJson.contains("\"" + paramName + "\"");
                        
                        parameters.put(paramName, new ParameterInfo(description, isRequired));
                        logger.debug("Found parameter: {} (required: {}) - {}", paramName, isRequired, description);
                    }
                }
            }
            
        } catch (Exception e) {
            logger.debug("Error parsing schema: {}", e.getMessage());
        }
        
        return parameters;
    }

    /**
     * Find the matching closing brace for a JSON object
     */
    private int findMatchingBrace(String json, int openBraceIndex) {
        int braceCount = 1;
        int index = openBraceIndex + 1;
        
        while (index < json.length() && braceCount > 0) {
            char c = json.charAt(index);
            if (c == '{') {
                braceCount++;
            } else if (c == '}') {
                braceCount--;
            }
            index++;
        }
        
        return braceCount == 0 ? index - 1 : -1;
    }

    /**
     * Extract just the description from the full tool description, without the schema
     */
    private String extractShortDescription(String fullDescription) {
        try {
            // Extract the description line from the tool description
            String[] lines = fullDescription.split("\n");
            for (String line : lines) {
                if (line.startsWith("Description: ")) {
                    return line.substring(13).trim();
                }
            }
            return "No description available";
        } catch (Exception e) {
            return "No description available";
        }
    }

    /**
     * Parameter information holder
     */
    private static class ParameterInfo {
        final String description;
        final boolean required;
        
        ParameterInfo(String description, boolean required) {
            this.description = description;
            this.required = required;
        }
    }

    private void handleShowDefault() {
        if (clientManager.hasDefaultServer()) {
            var defaultConfig = clientManager.getDefaultServerConfig();
            if (defaultConfig.isPresent()) {
                var config = defaultConfig.get();
                System.out.println("=== Default Server Configuration ===");
                System.out.println();
                System.out.println("Name: " + config.serverName());
                System.out.println("JAR Path: " + config.jarPath());
                System.out.println("Saved: " + new java.util.Date(config.savedAt()));
                System.out.println("Config File: " + clientManager.getDefaultServerConfigPath());
                System.out.println();
                System.out.println("To connect: connect " + config.serverName() + " stdio " + config.jarPath());
                System.out.println("To remove: remove-default");
            } else {
                System.out.println("✗ Failed to load default server configuration");
            }
        } else {
            System.out.println("No default server configuration found");
            System.out.println("Save a server as default using: connect <name> stdio <jar-path> --save-default");
        }
        System.out.println();
    }

    private void handleRemoveDefault() {
        if (clientManager.hasDefaultServer()) {
            var defaultConfig = clientManager.getDefaultServerConfig();
            if (defaultConfig.isPresent()) {
                var config = defaultConfig.get();
                System.out.println("Removing default server configuration:");
                System.out.println("  Name: " + config.serverName());
                System.out.println("  JAR: " + config.jarPath());
                System.out.println();
                
                boolean removed = clientManager.removeDefaultServer();
                if (removed) {
                    System.out.println("✓ Default server configuration removed successfully");
                } else {
                    System.out.println("✗ Failed to remove default server configuration");
                }
            } else {
                System.out.println("✗ Failed to load default server configuration for removal");
            }
        } else {
            System.out.println("No default server configuration to remove");
        }
        System.out.println();
    }

    private void handleGenerateConfig() {
        System.out.println("=== Spring AI MCP Client Configuration Generator ===");
        System.out.println();
        
        if (clientManager.hasDefaultServer()) {
            var defaultConfig = clientManager.getDefaultServerConfig();
            if (defaultConfig.isPresent()) {
                var config = defaultConfig.get();
                System.out.println("Found default server configuration:");
                System.out.println("  Name: " + config.serverName());
                System.out.println("  JAR: " + config.jarPath());
                System.out.println();
                
                // Offer automatic configuration and restart
                System.out.println("Choose an option:");
                System.out.println("  1. Auto-configure and restart (recommended)");
                System.out.println("  2. Show YAML configuration only");
                System.out.print("Enter choice (1 or 2): ");
                
                String choice = scanner.nextLine().trim();
                System.out.println();
                
                if ("1".equals(choice)) {
                    handleAutoConfigureAndRestart(config.serverName(), config.jarPath());
                } else {
                    generateConfigForServer(config.serverName(), config.jarPath());
                }
            } else {
                System.out.println("✗ Failed to load default server configuration");
            }
        } else {
            System.out.println("No default server found. Connect to a server first with --save-default flag.");
            System.out.println("Example: connect myserver stdio /path/to/server.jar --save-default");
        }
        System.out.println();
    }
    
    private void generateConfigForServer(String serverName, String jarPath) {
        System.out.println("Add this configuration to your src/main/resources/application.yml:");
        System.out.println();
        System.out.println("spring:");
        System.out.println("  ai:");
        System.out.println("    mcp:");
        System.out.println("      client:");
        System.out.println("        enabled: true");
        System.out.println("        type: SYNC");
        System.out.println("        stdio:");
        System.out.println("          connections:");
        System.out.println("            " + serverName + ":");
        System.out.println("              command: java");
        System.out.println("              args:");
        System.out.println("                - -Dlogging.level.root=OFF");
        System.out.println("                - -Dspring.main.banner-mode=off");
        System.out.println("                - -Dspring.main.log-startup-info=false");
        System.out.println("                - -jar");
        System.out.println("                - " + jarPath);
        System.out.println();
        System.out.println("⚠ Important: After adding this configuration, restart the application");
        System.out.println("to establish the STDIO connection and discover tools.");
    }

    private void handleAutoConfigureAndRestart(String serverName, String jarPath) {
        System.out.println("=== Auto-Configure and Restart ===");
        System.out.println();
        
        // Check if server already exists in configuration
        if (yamlConfigService.serverExistsInConfig(serverName)) {
            System.out.println("⚠ Server '" + serverName + "' already exists in application.yml");
            System.out.println("Proceeding with restart to ensure connection is active...");
        } else {
            System.out.println("Adding server configuration to application.yml...");
            boolean success = yamlConfigService.addServerToApplicationYml(serverName, jarPath);
            
            if (success) {
                System.out.println("✓ Successfully added server '" + serverName + "' to application.yml");
            } else {
                System.out.println("✗ Failed to update application.yml");
                System.out.println("Please manually add the configuration and restart.");
                return;
            }
        }
        
        System.out.println();
        System.out.println("🔄 Restarting application to establish STDIO connection...");
        System.out.println("The application will restart automatically and connect to the server.");
        System.out.println();
        
        // Trigger application restart
        triggerRestart();
    }

    private void triggerRestart() {
        try {
            // Clean shutdown
            running = false;
            
            // Exit with special code that can be caught by a wrapper script
            // or use Spring Boot's restart mechanism
            System.out.println("Exiting for restart...");
            System.exit(42); // Special exit code for restart
            
        } catch (Exception e) {
            logger.error("Failed to trigger restart", e);
            System.out.println("✗ Failed to trigger automatic restart");
            System.out.println("Please manually restart the application to see the new server.");
        }
    }

    private void handleExit() {
        System.out.println("Shutting down MCP client...");
        
        if (clientManager.isConnected()) {
            System.out.println("Disconnecting from server: " + clientManager.getCurrentServerName());
            clientManager.disconnect();
        }
        
        running = false;
        System.out.println("Goodbye!");
        System.exit(0);
    }
}