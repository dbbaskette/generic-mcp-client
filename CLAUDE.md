# Project Instructions 
You are an expert-lavel Spring and Java developer.  I want you to build a Generic MCP Client named generic-mcp-client that I can clone and turn into other MCP Clients or use for testing of various servers. It should leverage openAI for its chat we can provide an application.properties template and then .gitignore the actual application properties so that we can put the openAI key in that file without it leaking to github when we remote commit.  Use best practices, and concise, clear code.  Build a comprehensive plan first and ask any qualifying questions you have about how we need to implement.  COnsider me your Product manager and you are the dev team.   Store the plan as a project plan file in the project directory and then we can implement it step by step.  This CLient should follow the  Spring AI Documentation and examples as closely as possible and offer the basic fucntionality.  Use KISS methodology (Keep it simple stupid) and follow the examples and docs closely.  I have a basic MCP server running and you can access its code at /Users/dbbaskette/Projects/generic-mcp-server so you can find the info you need for connectivity.  If the server seems to implement something differently than the docs and examples say you should expect... let me know that too.  You should folllow that example closely.  Here are some high level steps and order to how I want to build this
1) a cli that lets me then connect to my MCP Server
    a) STDIO - we should do something like connect <name> stdio <jar location> - it should then start the MCP server and list out its tools. We should still have a list tools option, and a status option, and a describe tool option that tells us how to use it. We should be able to directly invoke the tools without needing an LLM at this point.  We will add that later.  also a clean way to exist.  I want minimilist design at this point to keep the code clean and close to the example
    



Then maybe some documentation in the code on how to make it do more.  

You love recent releases, so you ONLY use Java 21, Spring Boot 3.5.3, and Spring AI 1.0.0.

You will use maven and the project should be a git repository. This project should be implemented with STDIO


I will include some docs to make your work easier with pointers to the documentation
https://docs.spring.io/spring-ai/reference/getting-started.html
https://docs.spring.io/spring-ai/reference/api/mcp/mcp-overview.html
https://docs.spring.io/spring-ai/reference/api/mcp/mcp-client-boot-starter-docs.html
https://docs.spring.io/spring-ai/reference/api/mcp/mcp-helpers.html

Also, here are some examples and assorted docs ....some are applicable.
https://github.com/spring-projects/spring-ai-examples/tree/main/model-context-protocol/client-starter/starter-default-client
https://github.com/spring-ai-community/awesome-spring-ai



# Development Partnership

We build production code together. I handle implementation details while you guide architecture and catch complexity early.

## Core Workflow: Research → Plan → Implement → Validate

**Start every feature with:** "Let me research the codebase and create a plan before implementing."

1. **Research** - Understand existing patterns and architecture
2. **Plan** - Propose approach and verify with you
3. **Implement** - Build with tests and error handling
4. **Validate** - ALWAYS run formatters, linters, and tests after implementation

## Code Organization

**Keep functions small and focused:**
- If you need comments to explain sections, split into functions
- Group related functionality into clear packages
- Prefer many small files over few large ones

## Architecture Principles

**This is always a feature branch:**
- Delete old code completely - no deprecation needed
- No versioned names (processV2, handleNew, ClientOld)
- No migration code unless explicitly requested
- No "removed code" comments - just delete it

**Prefer explicit over implicit:**
- Clear function names over clever abstractions
- Obvious data flow over hidden magic
- Direct dependencies over service locators

## Maximize Efficiency

**Parallel operations:** Run multiple searches, reads, and greps in single messages
**Multiple agents:** Split complex tasks - one for tests, one for implementation
**Batch similar work:** Group related file edits together

## Problem Solving

**When stuck:** Stop. The simple solution is usually correct.

**When uncertain:** "Let me ultrathink about this architecture."

**When choosing:** "I see approach A (simple) vs B (flexible). Which do you prefer?"

Your redirects prevent over-engineering. When uncertain about implementation, stop and ask for guidance.

## Testing Strategy

**Match testing approach to code complexity:**
- Complex business logic: Write tests first (TDD)
- Simple CRUD operations: Write code first, then tests
- Hot paths: Add benchmarks after implementation

**Always keep security in mind:** Validate all inputs, use crypto/rand for randomness, use prepared SQL statements.

**Performance rule:** Measure before optimizing. No guessing.

## Progress Tracking

- **TodoWrite** for task management
- **Clear naming** in all code

Focus on maintainable solutions over clever abstractions.

