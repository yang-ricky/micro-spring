package org.microspring.redis.core;

import java.util.Set;

/**
 * Redis Sorted Set 操作接口
 */
public interface ZSetOperations<K, V> {

    /**
     * 添加元素到有序集合，如果元素已存在则更新其分数
     */
    Boolean add(K key, V value, double score);

    /**
     * 从有序集合中移除一个或多个元素
     */
    Long remove(K key, Object... values);

    /**
     * 增加有序集合中元素的分数
     */
    Double incrementScore(K key, V value, double delta);

    /**
     * 获取有序集合中元素的分数
     */
    Double score(K key, V value);

    /**
     * 获取有序集合的元素数量
     */
    Long size(K key);

    /**
     * 获取有序集合中指定分数范围的元素（按分数从小到大）
     */
    Set<V> rangeByScore(K key, double min, double max);

    /**
     * 获取有序集合中指定分数范围的元素（按分数从大到小）
     */
    Set<V> reverseRangeByScore(K key, double min, double max);

    /**
     * 获取有序集合中指定排名范围的元素（按分数从小到大，0表示第一个元素）
     */
    Set<V> range(K key, long start, long end);

    /**
     * 获取有序集合中指定排名范围的元素（按分数从大到小，0表示分数最高的元素）
     */
    Set<V> reverseRange(K key, long start, long end);

    /**
     * 获取有序集合中元素的排名（按分数从小到大，0表示第一个元素）
     */
    Long rank(K key, V value);

    /**
     * 获取有序集合中元素的排名（按分数从大到小，0表示分数最高的元素）
     */
    Long reverseRank(K key, V value);

    /**
     * 统计有序集合中指定分数范围内的元素个数
     */
    Long count(K key, double min, double max);

    /**
     * 计算多个有序集合的交集并将结果存储在新的有序集合中
     */
    Long intersectAndStore(K key, K otherKey, K destKey);

    /**
     * 计算多个有序集合的并集并将结果存储在新的有序集合中
     */
    Long unionAndStore(K key, K otherKey, K destKey);
} 