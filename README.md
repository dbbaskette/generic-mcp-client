# 🚀 Generic MCP Client

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.java.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.3-green.svg)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0.0-blue.svg)](https://spring.io/projects/spring-ai)
[![Maven](https://img.shields.io/badge/Maven-3.11.0-red.svg)](https://maven.apache.org/)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

A generic Model Context Protocol (MCP) client built with Spring Boot and Spring AI for testing and extending MCP servers. This client provides a clean, extensible foundation that can be cloned and customized for specific use cases.

## ✨ Features

- 🔌 **STDIO Transport Support** - Connect to MCP servers via standard I/O
- 🛠️ **Dynamic Server Management** - Add/remove servers at runtime
- 🔍 **Tool Discovery** - Automatic discovery and listing of available tools
- ⚡ **Direct Tool Execution** - Execute tools without LLM integration
- 🎯 **CLI Interface** - Interactive command-line interface
- 🔒 **Secure Configuration** - Template-based config with gitignored sensitive files
- 📝 **Comprehensive Logging** - Detailed logging for debugging and monitoring

## 🏗️ Architecture

```
generic-mcp-client/
├── 📁 src/main/java/com/baskettecase/mcpclient/
│   ├── 🎯 McpClientApplication.java          # Main Spring Boot application
│   ├── 📁 cli/
│   │   └── 🖥️ CliRunner.java                # Command-line interface
│   ├── 📁 client/
│   │   ├── 🔌 SpringAiMcpClientManager.java # MCP client lifecycle
│   │   └── 📊 ConnectionState.java          # Connection status tracking
│   ├── 📁 config/
│   │   ├── ⚙️ DefaultServerConfigService.java # Default server management
│   │   ├── 🔄 DynamicMcpConfigService.java  # Dynamic configuration
│   │   └── 📄 YamlConfigService.java        # YAML configuration
│   └── 📁 util/
│       └── 🔧 ParameterParser.java           # Parameter parsing utilities
└── 📁 src/main/resources/
    ├── ⚙️ application.yml.template          # Configuration template
    └── 📝 logback-spring.xml               # Logging configuration
```

## 🚀 Quick Start

### Prerequisites

- **Java 21** or higher
- **Maven 3.11.0** or higher
- **MCP Server** (e.g., [generic-mcp-server](https://github.com/dbbaskette/generic-mcp-server))

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/dbbaskette/generic-mcp-client.git
   cd generic-mcp-client
   ```

2. **Setup configuration**
   ```bash
   # Copy the template and edit with your settings
   cp src/main/resources/application.yml.template src/main/resources/application.yml
   # Edit src/main/resources/application.yml with your server JAR path
   ```

3. **Run the application**
   ```bash
   ./mcp-client.sh
   ```

## 📖 Usage

### CLI Commands

| Command | Description | Example |
|---------|-------------|---------|
| `connect <name> stdio <jar>` | Connect to MCP server | `connect myserver stdio /path/to/server.jar` |
| `list-tools` | List available tools | `list-tools` |
| `describe-tool <name>` | Show tool details | `describe-tool file_search` |
| `invoke-tool <name> [params]` | Execute a tool | `invoke-tool file_search path=/tmp` |
| `status` | Show connection status | `status` |
| `exit` | Clean exit | `exit` |

### Example Session

```bash
$ ./mcp-client.sh
[INFO] Starting Generic MCP Client...

mcp-client> connect myserver stdio /path/to/server.jar
[INFO] Connecting to MCP server...
[INFO] ✓ Connected to myserver

mcp-client> list-tools
Available tools:
- file_search: Search for files in the filesystem
- read_file: Read file contents
- list_dir: List directory contents

mcp-client> describe-tool file_search
Tool: file_search
Description: Search for files in the filesystem
Parameters:
- query (string, required): Search query
- path (string, optional): Base path to search

mcp-client> invoke-tool file_search query=*.txt path=/tmp
[INFO] Executing file_search...
Result: ["/tmp/example.txt", "/tmp/test.txt"]

mcp-client> exit
[INFO] Disconnecting from MCP server...
[INFO] ✓ Clean exit
```

## ⚙️ Configuration

### Application Configuration

The client uses YAML configuration with a template approach for security:

```yaml
spring:
  ai:
    mcp:
      client:
        enabled: true
        name: generic-mcp-client
        version: 1.0.0
        request-timeout: 30s
        type: SYNC
        stdio:
          connections:
            generic:
              command: java
              args:
              - -Dlogging.level.root=OFF
              - -Dspring.main.banner-mode=off
              - -Dspring.main.log-startup-info=false
              - -jar
              - /path/to/your/mcp-server.jar
```

### Security Features

- 🔒 **Template-based configuration** - `application.yml.template` is committed, actual config is gitignored
- 🚫 **No sensitive data in repo** - API keys and personal paths are excluded
- 🔐 **Secure defaults** - Sensible security defaults enabled

## 🛠️ Development

### Building from Source

```bash
# Clean and compile
mvn clean compile

# Run tests
mvn test

# Package
mvn package

# Run with Maven
mvn spring-boot:run
```

### Project Structure

- **`src/main/java/`** - Core application code
- **`src/main/resources/`** - Configuration and resources
- **`src/test/java/`** - Unit and integration tests
- **`scripts/`** - Utility scripts for development

### Key Components

- **`CliRunner`** - Main CLI interface and command processing
- **`SpringAiMcpClientManager`** - MCP client lifecycle management
- **`DynamicMcpConfigService`** - Runtime server configuration
- **`ParameterParser`** - Hybrid parameter parsing (key=value + JSON)

## 🔮 Future Plans

### 🎯 Phase 1: Enhanced CLI Experience
- [ ] **Interactive Help System** - Context-sensitive help and examples
- [ ] **Command History** - Persistent command history with search
- [ ] **Auto-completion** - Tab completion for commands and tool names
- [ ] **Progress Indicators** - Visual feedback for long-running operations

### 🤖 Phase 2: AI Integration
- [ ] **OpenAI Integration** - Connect to OpenAI for natural language tool execution
- [ ] **Conversation Memory** - Maintain context across multiple interactions
- [ ] **Smart Parameter Inference** - AI-powered parameter suggestion
- [ ] **Natural Language Queries** - "Show me all text files in /tmp" → tool execution

### 🔧 Phase 3: Advanced Features
- [ ] **Multiple Server Support** - Connect to multiple MCP servers simultaneously
- [ ] **Server Health Monitoring** - Health checks and automatic reconnection
- [ ] **Plugin System** - Extensible plugin architecture for custom functionality
- [ ] **Web Interface** - Optional web UI for non-CLI users

### 🌐 Phase 4: Network & Security
- [ ] **SSE Transport Support** - Server-Sent Events for remote connections
- [ ] **TLS/SSL Support** - Secure connections for remote servers
- [ ] **Authentication** - Support for authenticated MCP connections
- [ ] **Connection Pooling** - Efficient resource management

### 📊 Phase 5: Monitoring & Observability
- [ ] **Metrics Collection** - Prometheus metrics for monitoring
- [ ] **Distributed Tracing** - OpenTelemetry integration
- [ ] **Performance Profiling** - Tool execution performance analysis
- [ ] **Error Analytics** - Error tracking and reporting

### 🎨 Phase 6: User Experience
- [ ] **Themes & Customization** - Customizable CLI appearance
- [ ] **Export/Import** - Configuration and state management
- [ ] **Batch Operations** - Execute multiple tools in sequence
- [ ] **Scripting Support** - Run predefined tool sequences

### 🔌 Phase 7: Ecosystem Integration
- [ ] **IDE Plugins** - IntelliJ IDEA and VS Code extensions
- [ ] **Docker Support** - Containerized deployment options
- [ ] **Kubernetes Integration** - K8s deployment and management
- [ ] **CI/CD Integration** - Automated testing and deployment

## 🤝 Contributing

We welcome contributions! Please see our [Contributing Guidelines](CONTRIBUTING.md) for details.

### Development Setup

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/amazing-feature`
3. Make your changes and add tests
4. Commit your changes: `git commit -m 'Add amazing feature'`
5. Push to the branch: `git push origin feature/amazing-feature`
6. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **Spring AI Team** - For the excellent MCP client implementation
- **Model Context Protocol** - For the innovative protocol specification
- **Open Source Community** - For the tools and libraries that make this possible

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/dbbaskette/generic-mcp-client/issues)
- **Discussions**: [GitHub Discussions](https://github.com/dbbaskette/generic-mcp-client/discussions)
- **Documentation**: [Project Wiki](https://github.com/dbbaskette/generic-mcp-client/wiki)

---

<div align="center">

**Built with ❤️ using [Spring Boot](https://spring.io/projects/spring-boot) and [Spring AI](https://spring.io/projects/spring-ai)**

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.3-green.svg)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0.0-blue.svg)](https://spring.io/projects/spring-ai)

</div> 