package org.microspring.redis.core;

import java.util.Set;

/**
 * Interface defining Set operations.
 */
public interface SetOperations<K, V> {
    
    Long add(K key, V... values);
    
    Long remove(K key, V... values);
    
    Boolean isMember(K key, V value);
    
    Set<V> members(K key);
    
    Long size(K key);
    
    Set<V> intersect(K key, K otherKey);
    
    Set<V> union(K key, K otherKey);
    
    Set<V> difference(K key, K otherKey);
    
    Boolean move(K sourceKey, K destinationKey, V value);
    
    V pop(K key);
    
    V randomMember(K key);
} 