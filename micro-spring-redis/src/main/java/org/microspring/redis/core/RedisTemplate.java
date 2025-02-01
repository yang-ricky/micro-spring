package org.microspring.redis.core;

import org.microspring.redis.RedisConnection;
import org.microspring.redis.serializer.RedisSerializer;
import org.microspring.redis.serializer.StringRedisSerializer;

import java.io.IOException;

/**
 * Helper class that simplifies Redis data access code.
 * Provides high-level operations using serializers.
 */
@SuppressWarnings("unchecked")
public class RedisTemplate<K, V> {
    
    private RedisConnection connection;
    private RedisSerializer<K> keySerializer;
    private RedisSerializer<V> valueSerializer;
    private RedisSerializer<String> hashKeySerializer;
    private RedisSerializer<V> hashValueSerializer;
    private ValueOperations<K, V> valueOps;
    private SetOperations<K, V> setOps;
    private ListOperations<K, V> listOps;
    private HashOperations<K, String, V> hashOps;
    
    public RedisTemplate() {
        // Default to StringRedisSerializer
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        this.keySerializer = (RedisSerializer<K>) stringSerializer;
        this.valueSerializer = (RedisSerializer<V>) stringSerializer;
        this.hashKeySerializer = stringSerializer;
        this.hashValueSerializer = (RedisSerializer<V>) stringSerializer;
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

    public void setHashKeySerializer(RedisSerializer<String> hashKeySerializer) {
        this.hashKeySerializer = hashKeySerializer;
    }

    public void setHashValueSerializer(RedisSerializer<V> hashValueSerializer) {
        this.hashValueSerializer = hashValueSerializer;
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

    public ListOperations<K, V> opsForList() {
        if (listOps == null) {
            listOps = new DefaultListOperations<>(this);
        }
        return listOps;
    }

    public HashOperations<K, String, V> opsForHash() {
        if (hashOps == null) {
            hashOps = new DefaultHashOperations<>(this);
        }
        return hashOps;
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

    // Helper method to serialize hash key
    protected String serializeHashKey(String hashKey) {
        byte[] bytes = hashKeySerializer.serialize(hashKey);
        return bytes != null ? new String(bytes) : null;
    }

    // Helper method to serialize hash value
    protected String serializeHashValue(V value) {
        byte[] bytes = hashValueSerializer.serialize(value);
        return bytes != null ? new String(bytes) : null;
    }
    
    // Helper method to deserialize value
    protected V deserializeValue(String value) {
        return value != null ? valueSerializer.deserialize(value.getBytes()) : null;
    }

    // Helper method to deserialize hash key
    protected String deserializeHashKey(String value) {
        return value != null ? hashKeySerializer.deserialize(value.getBytes()) : null;
    }

    // Helper method to deserialize hash value
    protected V deserializeHashValue(String value) {
        return value != null ? hashValueSerializer.deserialize(value.getBytes()) : null;
    }
    
    protected RedisConnection getConnection() {
        return connection;
    }
} 