package com.baskettecase.mcpclient;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Generic MCP Client Application
 * 
 * A generic Model Context Protocol (MCP) client designed for:
 * - Testing MCP servers via STDIO transport
 * - Direct tool execution without LLM integration
 * - Easy cloning and extension for specific use cases
 * 
 * Key Features:
 * - Smart connect with default server management
 * - Hybrid parameter parsing (KV pairs + JSON fallback)
 * - Interactive CLI with comprehensive tool management
 * - Secure API key handling via gitignored properties
 * 
 * Usage:
 * 1. Copy application.properties.template to application.properties
 * 2. Configure your OpenAI API key (for future LLM integration)
 * 3. Run: java -jar generic-mcp-client.jar
 * 4. Use CLI commands to connect and interact with MCP servers
 */
@SpringBootApplication
public class McpClientApplication {

    public static void main(String[] args) {
        // Disable Spring Boot banner and startup info for cleaner CLI experience
        System.setProperty("spring.main.banner-mode", "off");
        System.setProperty("spring.main.log-startup-info", "false");
        
        SpringApplication.run(McpClientApplication.class, args);
    }
}