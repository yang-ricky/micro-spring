package org.microspring.redis.serializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;

/**
 * JSON serializer using Jackson library.
 */
public class JsonRedisSerializer<T> implements RedisSerializer<T> {
    private final ObjectMapper objectMapper;
    private final Class<T> type;

    public JsonRedisSerializer(Class<T> type) {
        this.type = type;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public byte[] serialize(T t) {
        if (t == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsBytes(t);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error serializing object to JSON", e);
        }
    }

    @Override
    public T deserialize(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            return objectMapper.readValue(bytes, type);
        } catch (IOException e) {
            throw new RuntimeException("Error deserializing JSON to object", e);
        }
    }
} 