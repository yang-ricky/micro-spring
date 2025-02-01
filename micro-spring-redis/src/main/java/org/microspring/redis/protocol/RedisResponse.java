package org.microspring.redis.protocol;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Handles Redis RESP protocol responses
 */
public class RedisResponse {
    public static final char SIMPLE_STRING = '+';
    public static final char ERROR = '-';
    public static final char INTEGER = ':';
    public static final char BULK_STRING = '$';
    public static final char ARRAY = '*';

    private final String value;
    private final char type;

    private RedisResponse(char type, String value) {
        this.type = type;
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public char getType() {
        return type;
    }

    public static RedisResponse read(BufferedReader reader) throws IOException {
        int firstByte = reader.read();
        if (firstByte == -1) {
            throw new IOException("Connection closed by Redis server");
        }

        char type = (char) firstByte;
        String line = reader.readLine();  // Read the rest of the line

        switch (type) {
            case SIMPLE_STRING:
            case ERROR:
            case INTEGER:
                return new RedisResponse(type, line);
                
            case BULK_STRING:
                int length = Integer.parseInt(line);
                if (length == -1) {
                    return new RedisResponse(type, null); // null bulk string
                }
                char[] bulk = new char[length];
                reader.read(bulk, 0, length);
                reader.readLine(); // consume the trailing \r\n
                return new RedisResponse(type, new String(bulk));
                
            case ARRAY:
                // For now, we'll just return the array size as a string
                // In a full implementation, we would recursively read array elements
                return new RedisResponse(type, line);
                
            default:
                throw new IOException("Unknown response type: " + type);
        }
    }

    @Override
    public String toString() {
        if (type == ERROR) {
            return String.format("Error: %s", value);
        }
        return value != null ? value : "null";
    }
} 