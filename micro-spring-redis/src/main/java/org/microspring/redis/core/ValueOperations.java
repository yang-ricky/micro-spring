package org.microspring.redis.core;

/**
 * Interface defining string (or value) operations.
 */
public interface ValueOperations<K, V> {

    void set(K key, V value);
    
    V get(K key);
    
    Boolean setIfAbsent(K key, V value);
    
    void append(K key, String value);
    
    Long increment(K key);
    
    Long increment(K key, long delta);
    
    Double increment(K key, double delta);
    
    Long decrement(K key);
    
    Long decrement(K key, long delta);
} 