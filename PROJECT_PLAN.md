# Generic MCP Client Project Plan

## Project Overview
**Project Name:** base-mcp-client  
**Purpose:** A generic Model Context Protocol (MCP) client for testing and cloning into other MCP clients  
**Tech Stack:** Java 21, Spring Boot 3.5.3, Spring AI 1.0.0, Maven  
**Transport:** STDIO (primary focus)  

## Project Architecture

### Core Components
1. **CLI Interface** - Command-line interface for interacting with MCP servers
2. **MCP Client Manager** - Handles connection lifecycle and tool discovery
3. **Tool Executor** - Direct tool execution without LLM integration
4. **Configuration Manager** - Handles application properties and OpenAI key management

### Package Structure
```
com.example.mcpclient/
├── McpClientApplication.java           # Main Spring Boot application
├── cli/
│   ├── CliRunner.java                 # Main CLI interface
│   ├── CommandHandler.java            # Command parsing and execution
│   └── commands/
│       ├── ConnectCommand.java        # connect <name> stdio <jar>
│       ├── ListToolsCommand.java      # list-tools
│       ├── StatusCommand.java         # status
│       ├── DescribeToolCommand.java   # describe-tool <name>
│       ├── InvokeToolCommand.java     # invoke-tool <name> [args]
│       └── ExitCommand.java           # exit
├── client/
│   ├── McpClientManager.java          # MCP client lifecycle management
│   ├── ConnectionState.java           # Connection status tracking
│   └── ToolDiscoveryService.java      # Tool discovery and caching
├── config/
│   ├── McpClientConfig.java           # Spring configuration
│   └── ApplicationProperties.java     # Configuration properties
└── util/
    ├── ProcessManager.java            # Process management for STDIO
    └── JsonFormatter.java             # JSON pretty printing for responses
```

## Implementation Phases

### Phase 1: Project Setup and Basic Structure
**Goal:** Create the basic Spring Boot project with proper configuration

**Tasks:**
1. Create Maven project with correct dependencies
2. Set up application.properties template and .gitignore
3. Create main application class
4. Implement basic CLI runner structure
5. Add logging configuration

**Dependencies:**
```xml
<!-- Spring AI MCP Client Starter -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-starter-mcp-client</artifactId>
</dependency>

<!-- Spring Boot Starter -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter</artifactId>
</dependency>

<!-- Spring Boot Configuration Processor -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-configuration-processor</artifactId>
    <optional>true</optional>
</dependency>

<!-- OpenAI (for future LLM integration) -->
<dependency>
    <groupId>org.springframework.ai</groupId>
    <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
</dependency>
```

### Phase 2: MCP Client Core Implementation
**Goal:** Implement MCP client connection and basic tool discovery

**Tasks:**
1. Create McpClientManager for STDIO transport
2. Implement connection lifecycle (connect, disconnect, status)
3. Add tool discovery and listing functionality
4. Create connection state management
5. Add error handling and logging

**Key Features:**
- STDIO transport configuration
- Automatic tool discovery after connection
- Connection status tracking
- Graceful error handling

### Phase 3: CLI Command System
**Goal:** Implement the command-line interface with all required commands

**Commands to Implement:**
1. `connect <name> stdio <jar-location>` - Connect to MCP server
2. `list-tools` - List all available tools from connected server
3. `status` - Show connection status and server info
4. `describe-tool <tool-name>` - Show tool description and parameters
5. `invoke-tool <tool-name> [params...]` - Execute tool directly
6. `exit` - Clean exit with connection cleanup

**CLI Features:**
- Command parsing and validation
- Interactive prompt
- Help system
- Error handling with user-friendly messages
- Clean exit handling

### Phase 4: Tool Execution System
**Goal:** Enable direct tool execution without LLM integration

**Tasks:**
1. Implement tool parameter parsing
2. Create tool execution service
3. Add response formatting
4. Handle tool errors gracefully
5. Add parameter validation

**Tool Execution Flow:**
1. Parse command and parameters
2. Validate tool exists and parameters are correct
3. Execute tool via MCP client
4. Format and display response
5. Handle errors with clear messages

### Phase 5: Configuration and Security
**Goal:** Proper configuration management and security

**Tasks:**
1. Create application.properties template
2. Set up .gitignore for actual properties file
3. Implement OpenAI configuration (for future use)
4. Add validation for required configurations
5. Secure handling of API keys

**Configuration Files:**
- `application.properties.template` - Template with placeholders
- `application.properties` - Actual config (gitignored)
- Logging configuration
- MCP client configuration

## CLI Command Specifications

### Connect Command
```bash
connect <server-name> stdio <jar-location>
```
- Starts the MCP server process
- Establishes STDIO connection
- Discovers and caches available tools
- Reports connection status

### List Tools Command
```bash
list-tools
```
- Shows all available tools from connected server
- Displays tool names and brief descriptions
- Shows parameter count for each tool

### Status Command
```bash
status
```
- Shows current connection status
- Displays server information
- Shows number of available tools
- Reports any connection errors

### Describe Tool Command
```bash
describe-tool <tool-name>
```
- Shows detailed tool description
- Lists all parameters with types and descriptions
- Shows required vs optional parameters
- Provides usage examples

### Invoke Tool Command
```bash
invoke-tool <tool-name> [param1=value1] [param2=value2]...
```
- Executes the specified tool with parameters
- Validates parameters before execution
- Displays formatted response
- Handles execution errors

### Exit Command
```bash
exit
```
- Cleanly disconnects from MCP server
- Stops server process if started by client
- Saves any necessary state
- Exits application

## Configuration Management

### Application Properties Template
```properties
# OpenAI Configuration (for future LLM integration)
spring.ai.openai.api-key=YOUR_OPENAI_API_KEY_HERE

# MCP Client Configuration
spring.ai.mcp.client.enabled=true
spring.ai.mcp.client.name=base-mcp-client
spring.ai.mcp.client.type=SYNC

# Logging Configuration
logging.level.com.example.mcpclient=DEBUG
logging.level.org.springframework.ai.mcp=DEBUG
```

### .gitignore Entries
```
# Application Properties (contains API keys)
application.properties

# IDE files
.idea/
*.iml
.vscode/

# Maven
target/
```

## Testing Strategy

### Unit Tests
- Command parsing and validation
- Tool parameter validation
- Response formatting
- Error handling

### Integration Tests
- MCP client connection
- Tool discovery
- Tool execution
- Process management

### Manual Testing
- CLI interaction flow
- Connection to generic-mcp-server
- All command functionality
- Error scenarios

## Success Criteria

### Phase 1 Success
- [x] Maven project builds successfully
- [x] Spring Boot application starts
- [x] Basic CLI prompt appears
- [x] Configuration template created

### Phase 2 Success
- [x] Can connect to generic-mcp-server via STDIO
- [x] Tools are discovered automatically
- [x] Connection status is tracked
- [x] Clean disconnect functionality

### Phase 3 Success
- [x] All CLI commands implemented
- [x] Command parsing works correctly
- [x] Help system functional
- [x] Error handling user-friendly

### Phase 4 Success
- [x] Can execute tools directly
- [x] Parameters parsed correctly
- [x] Responses formatted properly
- [x] Error handling robust

### Phase 5 Success
- [x] Configuration secure
- [x] OpenAI integration ready
- [x] Documentation complete
- [x] Ready for cloning/extension

## Future Enhancements (Phase 6+)
- LLM integration for natural language tool execution
- Multiple server connection support
- Tool execution history
- Configuration profiles
- Web interface option
- Tool result caching

## Key Design Decisions

### Why STDIO Focus?
- Primary use case is testing MCP servers
- Matches generic-mcp-server design
- Simpler than SSE for basic testing
- Easy to add SSE support later

### Why Direct Tool Execution?
- Enables testing without LLM dependency
- Faster feedback loop for development
- Easier debugging of tool issues
- LLM integration can be added later

### Why Minimalist Design?
- KISS principle as requested
- Easier to understand and extend
- Follows Spring AI examples closely
- Reduces complexity for cloning

## Questions for Product Manager

1. **Server Process Management**: Should the client automatically start/stop the MCP server process, or assume it's already running?

2. **Parameter Handling**: For tool parameters, should we support JSON input, key=value pairs, or both?

3. **Response Formatting**: How detailed should the tool response formatting be? JSON pretty-print or simplified output?

4. **Error Recovery**: Should the client automatically reconnect on connection loss, or require manual reconnection?

5. **Configuration**: Any specific configuration requirements beyond OpenAI API key?

6. **Logging**: What level of logging detail is needed for debugging vs production use?

This plan provides a solid foundation for building the Generic MCP Client while maintaining flexibility for future enhancements and easy cloning for specific use cases.