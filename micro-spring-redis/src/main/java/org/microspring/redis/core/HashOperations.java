package org.microspring.redis.core;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Redis Hash 操作接口
 */
public interface HashOperations<H, HK, HV> {
    /**
     * 设置 hash 表中指定字段的值
     */
    void put(H key, HK hashKey, HV value);

    /**
     * 批量设置 hash 表中多个字段的值
     */
    void putAll(H key, Map<? extends HK, ? extends HV> map);

    /**
     * 获取 hash 表中指定字段的值
     */
    HV get(H key, HK hashKey);

    /**
     * 删除 hash 表中的一个或多个字段
     */
    Long delete(H key, Object... hashKeys);

    /**
     * 判断 hash 表中是否存在指定的字段
     */
    Boolean hasKey(H key, HK hashKey);

    /**
     * 获取 hash 表中所有字段的值
     */
    Map<HK, HV> entries(H key);

    /**
     * 获取 hash 表中所有字段名
     */
    Set<HK> keys(H key);

    /**
     * 获取 hash 表中所有字段的值
     */
    List<HV> values(H key);

    /**
     * 获取 hash 表中字段的数量
     */
    Long size(H key);

    /**
     * 为哈希表中的字段值加上指定增量值
     */
    Long increment(H key, HK hashKey, long delta);

    /**
     * 为哈希表中的字段值加上指定浮点数增量值
     */
    Double increment(H key, HK hashKey, double delta);

    /**
     * 仅当字段不存在时，才设置 hash 表中字段的值
     */
    Boolean putIfAbsent(H key, HK hashKey, HV value);
} 