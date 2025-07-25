package com.baskettecase.mcpclient.client;

/**
 * Enumeration of possible MCP client connection states
 */
public enum ConnectionState {
    /**
     * Not connected to any server
     */
    DISCONNECTED,
    
    /**
     * Currently establishing connection to a server
     */
    CONNECTING,
    
    /**
     * Successfully connected to a server and ready for operations
     */
    CONNECTED,
    
    /**
     * Currently disconnecting from a server
     */
    DISCONNECTING,
    
    /**
     * Connection failed or was lost unexpectedly
     */
    ERROR
}