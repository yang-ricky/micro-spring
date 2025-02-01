package org.microspring.redis.protocol;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handles Redis RESP protocol responses
 */
public class RedisResponse {
    public static final char SIMPLE_STRING = '+';
    public static final char ERROR = '-';
    public static final char INTEGER = ':';
    public static final char BULK_STRING = '$';
    public static final char ARRAY = '*';

    private String value;
    private List<RedisResponse> array;
    private ResponseType type;

    public RedisResponse(String value, ResponseType type) {
        this.value = value;
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public List<RedisResponse> getArray() {
        return array;
    }

    public ResponseType getType() {
        return type;
    }

    public static RedisResponse read(BufferedReader reader) throws IOException {
        String line = reader.readLine();
        if (line == null) {
            throw new IOException("Connection closed by server");
        }

        char type = line.charAt(0);
        String value = line.substring(1);

        switch (type) {
            case '+':
                return new RedisResponse(value, ResponseType.SIMPLE_STRING);
            case '-':
                return new RedisResponse(value, ResponseType.ERROR);
            case ':':
                return new RedisResponse(value, ResponseType.INTEGER);
            case '$':
                return readBulkString(reader, value);
            case '*':
                return readArray(reader, value);
            default:
                throw new IOException("Unknown response type: " + type);
        }
    }

    private static RedisResponse readBulkString(BufferedReader reader, String lengthStr) throws IOException {
        int length = Integer.parseInt(lengthStr);
        if (length == -1) {
            return new RedisResponse(null, ResponseType.BULK_STRING);
        }

        char[] chars = new char[length];
        reader.read(chars, 0, length);
        reader.readLine(); // consume CRLF
        return new RedisResponse(new String(chars), ResponseType.BULK_STRING);
    }

    private static RedisResponse readArray(BufferedReader reader, String lengthStr) throws IOException {
        int length = Integer.parseInt(lengthStr);
        if (length == -1) {
            return new RedisResponse(null, ResponseType.ARRAY);
        }

        RedisResponse response = new RedisResponse(null, ResponseType.ARRAY);
        response.array = new ArrayList<>(length);
        
        for (int i = 0; i < length; i++) {
            response.array.add(read(reader));
        }
        
        return response;
    }

    @Override
    public String toString() {
        if (type == ResponseType.ERROR) {
            return String.format("Error: %s", value);
        }
        if (type == ResponseType.ARRAY && array != null) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < array.size(); i++) {
                if (i > 0) sb.append("\n");
                sb.append(array.get(i).toString());
            }
            return sb.toString();
        }
        return value != null ? value : "null";
    }
} 