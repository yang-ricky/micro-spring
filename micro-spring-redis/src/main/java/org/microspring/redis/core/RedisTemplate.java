package org.microspring.redis.core;

import org.microspring.redis.RedisConnection;
import org.microspring.redis.serializer.RedisSerializer;
import org.microspring.redis.serializer.StringRedisSerializer;

import java.io.IOException;

/**
 * Helper class that simplifies Redis data access code.
 * Provides high-level operations using serializers.
 */
public class RedisTemplate<K, V> {
    
    private RedisConnection connection;
    private RedisSerializer<K> keySerializer;
    private RedisSerializer<V> valueSerializer;
    private ValueOperations<K, V> valueOps;
    private SetOperations<K, V> setOps;
    
    public RedisTemplate() {
        // Default to StringRedisSerializer
        this.keySerializer = (RedisSerializer<K>) new StringRedisSerializer();
        this.valueSerializer = (RedisSerializer<V>) new StringRedisSerializer();
    }
    
    public void setConnection(RedisConnection connection) {
        this.connection = connection;
    }
    
    public void setKeySerializer(RedisSerializer<K> keySerializer) {
        this.keySerializer = keySerializer;
    }
    
    public void setValueSerializer(RedisSerializer<V> valueSerializer) {
        this.valueSerializer = valueSerializer;
    }
    
    public ValueOperations<K, V> opsForValue() {
        if (valueOps == null) {
            valueOps = new DefaultValueOperations<>(this);
        }
        return valueOps;
    }
    
    public SetOperations<K, V> opsForSet() {
        if (setOps == null) {
            setOps = new DefaultSetOperations<>(this);
        }
        return setOps;
    }
    
    // Helper method to serialize key
    protected String serializeKey(K key) {
        byte[] bytes = keySerializer.serialize(key);
        return bytes != null ? new String(bytes) : null;
    }
    
    // Helper method to serialize value
    protected String serializeValue(V value) {
        byte[] bytes = valueSerializer.serialize(value);
        return bytes != null ? new String(bytes) : null;
    }
    
    // Helper method to deserialize value
    protected V deserializeValue(String value) {
        return value != null ? valueSerializer.deserialize(value.getBytes()) : null;
    }
    
    protected RedisConnection getConnection() {
        return connection;
    }
} 