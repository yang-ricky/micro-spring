package org.microspring.redis.core;

import java.io.IOException;

/**
 * Default implementation of ValueOperations interface.
 */
public class DefaultValueOperations<K, V> implements ValueOperations<K, V> {

    private final RedisTemplate<K, V> template;

    public DefaultValueOperations(RedisTemplate<K, V> template) {
        this.template = template;
    }

    @Override
    public void set(K key, V value) {
        try {
            template.getConnection().set(template.serializeKey(key), template.serializeValue(value));
        } catch (IOException e) {
            throw new RuntimeException("Error setting value", e);
        }
    }

    @Override
    public V get(K key) {
        try {
            String value = template.getConnection().get(template.serializeKey(key));
            return template.deserializeValue(value);
        } catch (IOException e) {
            throw new RuntimeException("Error getting value", e);
        }
    }

    @Override
    public Boolean setIfAbsent(K key, V value) {
        try {
            String result = template.getConnection().sendCommand("SETNX", 
                template.serializeKey(key), 
                template.serializeValue(value)
            ).toString();
            return "1".equals(result);
        } catch (IOException e) {
            throw new RuntimeException("Error setting value if absent", e);
        }
    }

    @Override
    public void append(K key, String value) {
        try {
            template.getConnection().sendCommand("APPEND", 
                template.serializeKey(key), 
                value
            );
        } catch (IOException e) {
            throw new RuntimeException("Error appending value", e);
        }
    }

    @Override
    public Long increment(K key) {
        return increment(key, 1L);
    }

    @Override
    public Long increment(K key, long delta) {
        try {
            String result = template.getConnection().sendCommand("INCRBY", 
                template.serializeKey(key), 
                String.valueOf(delta)
            ).toString();
            return Long.parseLong(result);
        } catch (IOException e) {
            throw new RuntimeException("Error incrementing value", e);
        }
    }

    @Override
    public Double increment(K key, double delta) {
        try {
            String result = template.getConnection().sendCommand("INCRBYFLOAT", 
                template.serializeKey(key), 
                String.valueOf(delta)
            ).toString();
            return Double.parseDouble(result);
        } catch (IOException e) {
            throw new RuntimeException("Error incrementing value", e);
        }
    }

    @Override
    public Long decrement(K key) {
        return decrement(key, 1L);
    }

    @Override
    public Long decrement(K key, long delta) {
        return increment(key, -delta);
    }
} 