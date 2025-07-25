package com.baskettecase.mcpclient.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Hybrid parameter parser supporting both key=value pairs and JSON format
 * 
 * Parsing Strategy:
 * 1. Try key=value pairs first (most common CLI usage)
 * 2. Fall back to JSON for complex nested objects
 * 3. Auto-detect format based on input structure
 */
@Component
public class ParameterParser {

    private static final Logger logger = LoggerFactory.getLogger(ParameterParser.class);
    private final ObjectMapper objectMapper;

    public ParameterParser() {
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Parse parameters from string array using hybrid approach
     * 
     * @param args Parameter arguments from CLI
     * @return Map of parameter name to value
     * @throws IllegalArgumentException if parameters cannot be parsed
     */
    public Map<String, Object> parseParameters(String[] args) {
        if (args == null || args.length == 0) {
            return new HashMap<>();
        }

        // Single argument might be JSON
        if (args.length == 1 && isJsonFormat(args[0])) {
            return parseJson(args[0]);
        }

        // Multiple arguments or non-JSON single argument - try key=value format
        if (isKeyValueFormat(args)) {
            return parseKeyValue(args);
        }

        // If we get here, the format is not recognized
        throw new IllegalArgumentException(
            "Parameters must be in key=value format (e.g., name=John age=25) " +
            "or JSON format (e.g., '{\"name\":\"John\",\"age\":25}')"
        );
    }

    /**
     * Check if input looks like JSON format
     */
    private boolean isJsonFormat(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }

        String trimmed = input.trim();
        return (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
               (trimmed.startsWith("[") && trimmed.endsWith("]"));
    }

    /**
     * Check if arguments are in key=value format
     */
    private boolean isKeyValueFormat(String[] args) {
        for (String arg : args) {
            if (arg == null || !arg.contains("=")) {
                return false;
            }
            
            // Check that it's a simple key=value, not JSON
            if (arg.trim().startsWith("{") || arg.trim().startsWith("[")) {
                return false;
            }
        }
        return true;
    }

    /**
     * Parse JSON string into parameter map
     */
    private Map<String, Object> parseJson(String jsonString) {
        try {
            logger.debug("Parsing JSON parameters: {}", jsonString);
            TypeReference<Map<String, Object>> typeRef = new TypeReference<>() {};
            Map<String, Object> result = objectMapper.readValue(jsonString, typeRef);
            logger.debug("Parsed JSON parameters: {}", result);
            return result;
        } catch (Exception e) {
            logger.error("Failed to parse JSON parameters: {}", jsonString, e);
            throw new IllegalArgumentException("Invalid JSON format: " + e.getMessage(), e);
        }
    }

    /**
     * Parse key=value pairs into parameter map
     */
    private Map<String, Object> parseKeyValue(String[] args) {
        Map<String, Object> parameters = new HashMap<>();

        for (String arg : args) {
            String[] parts = arg.split("=", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid key=value format: " + arg);
            }

            String key = parts[0].trim();
            String value = parts[1].trim();

            if (key.isEmpty()) {
                throw new IllegalArgumentException("Empty key in parameter: " + arg);
            }

            // Try to convert value to appropriate type
            Object convertedValue = convertValue(value);
            parameters.put(key, convertedValue);
            
            logger.debug("Parsed parameter: {} = {} ({})", key, convertedValue, convertedValue.getClass().getSimpleName());
        }

        logger.debug("Parsed key=value parameters: {}", parameters);
        return parameters;
    }

    /**
     * Convert string value to appropriate Java type
     * 
     * Type inference rules:
     * - "true"/"false" -> Boolean
     * - Numeric values -> Double (to handle both int and float)
     * - Everything else -> String
     */
    private Object convertValue(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }

        // Remove surrounding quotes if present
        if ((value.startsWith("\"") && value.endsWith("\"")) ||
            (value.startsWith("'") && value.endsWith("'"))) {
            return value.substring(1, value.length() - 1);
        }

        // Boolean conversion
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }

        // Numeric conversion
        try {
            // Try integer first
            if (value.matches("-?\\d+")) {
                long longValue = Long.parseLong(value);
                // Return as Integer if it fits, otherwise Long
                if (longValue >= Integer.MIN_VALUE && longValue <= Integer.MAX_VALUE) {
                    return (int) longValue;
                }
                return longValue;
            }
            
            // Try double
            if (value.matches("-?\\d*\\.\\d+([eE][+-]?\\d+)?")) {
                return Double.parseDouble(value);
            }
        } catch (NumberFormatException e) {
            // Not a number, treat as string
            logger.debug("Value '{}' is not a number, treating as string", value);
        }

        // Default to string
        return value;
    }

    /**
     * Format parameters for display (useful for debugging)
     */
    public String formatParameters(Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return "(no parameters)";
        }

        StringBuilder sb = new StringBuilder();
        parameters.forEach((key, value) -> {
            sb.append(key).append("=").append(value)
              .append(" (").append(value.getClass().getSimpleName()).append("), ");
        });
        
        // Remove trailing comma and space
        if (sb.length() > 2) {
            sb.setLength(sb.length() - 2);
        }
        
        return sb.toString();
    }
}