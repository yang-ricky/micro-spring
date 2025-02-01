package org.microspring.redis.core;

import java.util.List;

/**
 * Interface that specifies a basic set of Redis list operations
 */
public interface ListOperations<K, V> {
    /**
     * Push an element to the head of the list
     */
    Long leftPush(K key, V value);

    /**
     * Push multiple elements to the head of the list
     */
    Long leftPushAll(K key, V... values);

    /**
     * Push an element to the tail of the list
     */
    Long rightPush(K key, V value);

    /**
     * Push multiple elements to the tail of the list
     */
    Long rightPushAll(K key, V... values);

    /**
     * Remove and get the first element of the list
     */
    V leftPop(K key);

    /**
     * Remove and get the last element of the list
     */
    V rightPop(K key);

    /**
     * Get a range of elements from the list
     */
    List<V> range(K key, long start, long end);

    /**
     * Trim the list to the specified range
     */
    void trim(K key, long start, long end);

    /**
     * Get the size of the list
     */
    Long size(K key);

    /**
     * Set the value at the specified index
     */
    void set(K key, long index, V value);

    /**
     * Get the value at the specified index
     */
    V index(K key, long index);

    /**
     * Remove elements equal to value from the list
     */
    Long remove(K key, long count, V value);
} 