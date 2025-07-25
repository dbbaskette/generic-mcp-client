package com.baskettecase.mcpclient.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

/**
 * Service for managing default server configuration persistence.
 * Saves and loads default MCP server settings to/from a local configuration file.
 */
@Service
public class DefaultServerConfigService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultServerConfigService.class);
    private static final String CONFIG_DIR = ".generic-mcp-client";
    private static final String CONFIG_FILE = "default-server.json";
    
    private final ObjectMapper objectMapper;
    private final Path configPath;

    public DefaultServerConfigService() {
        this.objectMapper = new ObjectMapper();
        this.configPath = getConfigPath();
    }

    /**
     * Save a server configuration as the default
     */
    public boolean saveDefaultServer(String serverName, String jarPath) {
        try {
            DefaultServerConfig config = new DefaultServerConfig(serverName, jarPath);
            
            // Ensure config directory exists
            Files.createDirectories(configPath.getParent());
            
            // Write configuration to file
            objectMapper.writeValue(configPath.toFile(), config);
            
            logger.info("Saved default server configuration: {} -> {}", serverName, jarPath);
            return true;
            
        } catch (IOException e) {
            logger.error("Failed to save default server configuration", e);
            return false;
        }
    }

    /**
     * Load the default server configuration
     */
    public Optional<DefaultServerConfig> loadDefaultServer() {
        try {
            if (!Files.exists(configPath)) {
                logger.debug("No default server configuration file found");
                return Optional.empty();
            }
            
            DefaultServerConfig config = objectMapper.readValue(configPath.toFile(), DefaultServerConfig.class);
            logger.info("Loaded default server configuration: {} -> {}", config.serverName(), config.jarPath());
            return Optional.of(config);
            
        } catch (IOException e) {
            logger.error("Failed to load default server configuration", e);
            return Optional.empty();
        }
    }

    /**
     * Check if a default server configuration exists
     */
    public boolean hasDefaultServer() {
        return Files.exists(configPath);
    }

    /**
     * Remove the default server configuration
     */
    public boolean removeDefaultServer() {
        try {
            if (Files.exists(configPath)) {
                Files.delete(configPath);
                logger.info("Removed default server configuration");
                return true;
            }
            return false;
        } catch (IOException e) {
            logger.error("Failed to remove default server configuration", e);
            return false;
        }
    }

    /**
     * Get the path to the configuration file
     */
    private Path getConfigPath() {
        String userHome = System.getProperty("user.home");
        return Paths.get(userHome, CONFIG_DIR, CONFIG_FILE);
    }

    /**
     * Get the configuration file path for display purposes
     */
    public String getConfigFilePath() {
        return configPath.toString();
    }

    /**
     * Default server configuration record
     */
    public record DefaultServerConfig(
        String serverName,
        String jarPath,
        long savedAt
    ) {
        public DefaultServerConfig(String serverName, String jarPath) {
            this(serverName, jarPath, System.currentTimeMillis());
        }
    }
}
