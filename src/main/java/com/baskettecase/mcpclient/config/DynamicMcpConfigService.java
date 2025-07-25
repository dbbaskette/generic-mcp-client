package com.baskettecase.mcpclient.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for dynamically managing MCP server configurations
 * This service allows adding new MCP servers at runtime by updating Spring's environment
 * and providing a way to refresh the MCP client configuration without full application restart.
 */
@Service
public class DynamicMcpConfigService {

    private static final Logger logger = LoggerFactory.getLogger(DynamicMcpConfigService.class);
    private static final String PROPERTY_SOURCE_NAME = "dynamicMcpServers";
    
    @Autowired
    private ConfigurableEnvironment environment;
    
    private final Map<String, ServerConfig> dynamicServers = new ConcurrentHashMap<>();
    
    /**
     * Add a new MCP server configuration dynamically
     * This updates Spring's environment properties to include the new server
     */
    public boolean addServer(String serverName, String jarPath) {
        try {
            logger.info("Adding dynamic MCP server: {} -> {}", serverName, jarPath);
            
            // Store the server configuration
            ServerConfig config = new ServerConfig(serverName, jarPath);
            dynamicServers.put(serverName, config);
            
            // Create property map for Spring environment
            Map<String, Object> properties = new HashMap<>();
            
            // Add all dynamic servers to the property map
            for (Map.Entry<String, ServerConfig> entry : dynamicServers.entrySet()) {
                String name = entry.getKey();
                ServerConfig serverConfig = entry.getValue();
                
                String baseKey = "spring.ai.mcp.client.stdio.connections." + name;
                properties.put(baseKey + ".command", "java");
                properties.put(baseKey + ".args[0]", "-Dlogging.level.root=OFF");
                properties.put(baseKey + ".args[1]", "-Dspring.main.banner-mode=off");
                properties.put(baseKey + ".args[2]", "-Dspring.main.log-startup-info=false");
                properties.put(baseKey + ".args[3]", "-jar");
                properties.put(baseKey + ".args[4]", serverConfig.jarPath());
            }
            
            // Remove existing dynamic property source if it exists
            if (environment.getPropertySources().contains(PROPERTY_SOURCE_NAME)) {
                environment.getPropertySources().remove(PROPERTY_SOURCE_NAME);
            }
            
            // Add new property source with updated configurations
            MapPropertySource propertySource = new MapPropertySource(PROPERTY_SOURCE_NAME, properties);
            environment.getPropertySources().addFirst(propertySource);
            
            logger.info("✓ Dynamic MCP server configuration added: {}", serverName);
            logger.info("⚠ Note: Spring AI MCP Client may require context refresh to detect new servers");
            
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to add dynamic MCP server: {} -> {}", serverName, jarPath, e);
            return false;
        }
    }
    
    /**
     * Remove a dynamic MCP server configuration
     */
    public boolean removeServer(String serverName) {
        try {
            if (!dynamicServers.containsKey(serverName)) {
                logger.warn("Server not found in dynamic configuration: {}", serverName);
                return false;
            }
            
            logger.info("Removing dynamic MCP server: {}", serverName);
            dynamicServers.remove(serverName);
            
            // Rebuild property source without the removed server
            Map<String, Object> properties = new HashMap<>();
            for (Map.Entry<String, ServerConfig> entry : dynamicServers.entrySet()) {
                String name = entry.getKey();
                ServerConfig serverConfig = entry.getValue();
                
                String baseKey = "spring.ai.mcp.client.stdio.connections." + name;
                properties.put(baseKey + ".command", "java");
                properties.put(baseKey + ".args[0]", "-Dlogging.level.root=OFF");
                properties.put(baseKey + ".args[1]", "-Dspring.main.banner-mode=off");
                properties.put(baseKey + ".args[2]", "-Dspring.main.log-startup-info=false");
                properties.put(baseKey + ".args[3]", "-jar");
                properties.put(baseKey + ".args[4]", serverConfig.jarPath());
            }
            
            // Update property source
            if (environment.getPropertySources().contains(PROPERTY_SOURCE_NAME)) {
                environment.getPropertySources().remove(PROPERTY_SOURCE_NAME);
            }
            
            if (!properties.isEmpty()) {
                MapPropertySource propertySource = new MapPropertySource(PROPERTY_SOURCE_NAME, properties);
                environment.getPropertySources().addFirst(propertySource);
            }
            
            logger.info("✓ Dynamic MCP server configuration removed: {}", serverName);
            return true;
            
        } catch (Exception e) {
            logger.error("Failed to remove dynamic MCP server: {}", serverName, e);
            return false;
        }
    }
    
    /**
     * Get all dynamically configured servers
     */
    public Map<String, ServerConfig> getDynamicServers() {
        return new HashMap<>(dynamicServers);
    }
    
    /**
     * Check if a server is dynamically configured
     */
    public boolean hasServer(String serverName) {
        return dynamicServers.containsKey(serverName);
    }
    
    /**
     * Get the current dynamic configuration as YAML for manual application.yml updates
     */
    public String generateYamlConfig() {
        if (dynamicServers.isEmpty()) {
            return "# No dynamic servers configured";
        }
        
        StringBuilder yaml = new StringBuilder();
        yaml.append("spring:\n");
        yaml.append("  ai:\n");
        yaml.append("    mcp:\n");
        yaml.append("      client:\n");
        yaml.append("        enabled: true\n");
        yaml.append("        type: SYNC\n");
        yaml.append("        stdio:\n");
        yaml.append("          connections:\n");
        
        for (Map.Entry<String, ServerConfig> entry : dynamicServers.entrySet()) {
            String name = entry.getKey();
            ServerConfig config = entry.getValue();
            
            yaml.append("            ").append(name).append(":\n");
            yaml.append("              command: java\n");
            yaml.append("              args:\n");
            yaml.append("                - -Dlogging.level.root=OFF\n");
            yaml.append("                - -Dspring.main.banner-mode=off\n");
            yaml.append("                - -Dspring.main.log-startup-info=false\n");
            yaml.append("                - -jar\n");
            yaml.append("                - ").append(config.jarPath()).append("\n");
        }
        
        return yaml.toString();
    }
    
    /**
     * Server configuration record
     */
    public record ServerConfig(String serverName, String jarPath) {}
}
