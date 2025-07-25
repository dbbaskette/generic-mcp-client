package com.baskettecase.mcpclient.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for managing YAML configuration file updates
 */
@Service
public class YamlConfigService {

    private static final Logger logger = LoggerFactory.getLogger(YamlConfigService.class);
    private static final String APPLICATION_YML_PATH = "src/main/resources/application.yml";

    /**
     * Add or update MCP server configuration in application.yml
     */
    public boolean addServerToApplicationYml(String serverName, String jarPath) {
        try {
            Path yamlPath = Paths.get(APPLICATION_YML_PATH);
            
            // Read existing YAML
            Map<String, Object> config = loadYamlConfig(yamlPath);
            
            // Navigate to the connections section
            Map<String, Object> connections = getOrCreateConnectionsSection(config);
            
            // Add the new server configuration
            Map<String, Object> serverConfig = createServerConfig(jarPath);
            connections.put(serverName, serverConfig);
            
            // Write updated YAML back to file
            writeYamlConfig(yamlPath, config);
            
            logger.info("✓ Added MCP server '{}' to application.yml", serverName);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to update application.yml with server: {}", serverName, e);
            return false;
        }
    }

    /**
     * Check if a server already exists in the configuration
     */
    public boolean serverExistsInConfig(String serverName) {
        try {
            Path yamlPath = Paths.get(APPLICATION_YML_PATH);
            Map<String, Object> config = loadYamlConfig(yamlPath);
            Map<String, Object> connections = getConnectionsSection(config);
            return connections != null && connections.containsKey(serverName);
        } catch (Exception e) {
            logger.error("Failed to check if server exists in config: {}", serverName, e);
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> loadYamlConfig(Path yamlPath) throws IOException {
        Yaml yaml = new Yaml();
        try (FileReader reader = new FileReader(yamlPath.toFile())) {
            Map<String, Object> config = yaml.load(reader);
            return config != null ? config : new LinkedHashMap<>();
        }
    }

    private void writeYamlConfig(Path yamlPath, Map<String, Object> config) throws IOException {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        
        Yaml yaml = new Yaml(options);
        try (FileWriter writer = new FileWriter(yamlPath.toFile())) {
            yaml.dump(config, writer);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getOrCreateConnectionsSection(Map<String, Object> config) {
        Map<String, Object> spring = (Map<String, Object>) config.computeIfAbsent("spring", k -> new LinkedHashMap<>());
        Map<String, Object> ai = (Map<String, Object>) spring.computeIfAbsent("ai", k -> new LinkedHashMap<>());
        Map<String, Object> mcp = (Map<String, Object>) ai.computeIfAbsent("mcp", k -> new LinkedHashMap<>());
        Map<String, Object> client = (Map<String, Object>) mcp.computeIfAbsent("client", k -> new LinkedHashMap<>());
        
        // Ensure basic client configuration
        client.putIfAbsent("enabled", true);
        client.putIfAbsent("type", "SYNC");
        
        Map<String, Object> stdio = (Map<String, Object>) client.computeIfAbsent("stdio", k -> new LinkedHashMap<>());
        return (Map<String, Object>) stdio.computeIfAbsent("connections", k -> new LinkedHashMap<>());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getConnectionsSection(Map<String, Object> config) {
        try {
            Map<String, Object> spring = (Map<String, Object>) config.get("spring");
            if (spring == null) return null;
            
            Map<String, Object> ai = (Map<String, Object>) spring.get("ai");
            if (ai == null) return null;
            
            Map<String, Object> mcp = (Map<String, Object>) ai.get("mcp");
            if (mcp == null) return null;
            
            Map<String, Object> client = (Map<String, Object>) mcp.get("client");
            if (client == null) return null;
            
            Map<String, Object> stdio = (Map<String, Object>) client.get("stdio");
            if (stdio == null) return null;
            
            return (Map<String, Object>) stdio.get("connections");
        } catch (ClassCastException e) {
            return null;
        }
    }

    private Map<String, Object> createServerConfig(String jarPath) {
        Map<String, Object> serverConfig = new LinkedHashMap<>();
        serverConfig.put("command", "java");
        
        List<String> args = List.of(
            "-Dlogging.level.root=OFF",
            "-Dspring.main.banner-mode=off", 
            "-Dspring.main.log-startup-info=false",
            "-jar",
            jarPath
        );
        serverConfig.put("args", args);
        
        return serverConfig;
    }
}
