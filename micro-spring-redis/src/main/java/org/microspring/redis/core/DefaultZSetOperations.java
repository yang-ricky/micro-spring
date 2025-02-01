package org.microspring.redis.core;

import org.microspring.redis.protocol.RedisResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ZSetOperations 接口的默认实现
 */
public class DefaultZSetOperations<K, V> implements ZSetOperations<K, V> {

    private final RedisTemplate<K, V> template;

    public DefaultZSetOperations(RedisTemplate<K, V> template) {
        this.template = template;
    }

    private Set<V> parseArrayResponse(RedisResponse response) {
        if (response == null || response.getArray() == null) {
            return new LinkedHashSet<>();
        }
        return response.getArray().stream()
            .map(RedisResponse::getValue)
            .map(value -> template.deserializeValue(value))
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public Boolean add(K key, V value, double score) {
        try {
            RedisResponse response = template.getConnection().sendCommand("ZADD",
                template.serializeKey(key),
                String.valueOf(score),
                template.serializeValue(value));
            return "1".equals(response.getValue());
        } catch (IOException e) {
            throw new RuntimeException("Error adding to sorted set", e);
        }
    }

    @Override
    public Long remove(K key, Object... values) {
        try {
            List<String> args = new ArrayList<>();
            args.add(template.serializeKey(key));
            for (Object value : values) {
                @SuppressWarnings("unchecked")
                V typedValue = (V) value;
                args.add(template.serializeValue(typedValue));
            }

            RedisResponse response = template.getConnection().sendCommand("ZREM",
                args.toArray(new String[0]));
            return Long.parseLong(response.getValue());
        } catch (IOException e) {
            throw new RuntimeException("Error removing from sorted set", e);
        }
    }

    @Override
    public Double incrementScore(K key, V value, double delta) {
        try {
            RedisResponse response = template.getConnection().sendCommand("ZINCRBY",
                template.serializeKey(key),
                String.valueOf(delta),
                template.serializeValue(value));
            return Double.parseDouble(response.getValue());
        } catch (IOException e) {
            throw new RuntimeException("Error incrementing score", e);
        }
    }

    @Override
    public Double score(K key, V value) {
        try {
            RedisResponse response = template.getConnection().sendCommand("ZSCORE",
                template.serializeKey(key),
                template.serializeValue(value));
            String scoreStr = response.getValue();
            return scoreStr != null ? Double.parseDouble(scoreStr) : null;
        } catch (IOException e) {
            throw new RuntimeException("Error getting score", e);
        }
    }

    @Override
    public Long size(K key) {
        try {
            RedisResponse response = template.getConnection().sendCommand("ZCARD",
                template.serializeKey(key));
            return Long.parseLong(response.getValue());
        } catch (IOException e) {
            throw new RuntimeException("Error getting sorted set size", e);
        }
    }

    @Override
    public Set<V> rangeByScore(K key, double min, double max) {
        try {
            RedisResponse response = template.getConnection().sendCommand("ZRANGEBYSCORE",
                template.serializeKey(key),
                String.valueOf(min),
                String.valueOf(max));
            return parseArrayResponse(response);
        } catch (IOException e) {
            throw new RuntimeException("Error getting range by score", e);
        }
    }

    @Override
    public Set<V> reverseRangeByScore(K key, double min, double max) {
        try {
            RedisResponse response = template.getConnection().sendCommand("ZREVRANGEBYSCORE",
                template.serializeKey(key),
                String.valueOf(max),
                String.valueOf(min));
            return parseArrayResponse(response);
        } catch (IOException e) {
            throw new RuntimeException("Error getting reverse range by score", e);
        }
    }

    @Override
    public Set<V> range(K key, long start, long end) {
        try {
            RedisResponse response = template.getConnection().sendCommand("ZRANGE",
                template.serializeKey(key),
                String.valueOf(start),
                String.valueOf(end));
            return parseArrayResponse(response);
        } catch (IOException e) {
            throw new RuntimeException("Error getting range", e);
        }
    }

    @Override
    public Set<V> reverseRange(K key, long start, long end) {
        try {
            RedisResponse response = template.getConnection().sendCommand("ZREVRANGE",
                template.serializeKey(key),
                String.valueOf(start),
                String.valueOf(end));
            return parseArrayResponse(response);
        } catch (IOException e) {
            throw new RuntimeException("Error getting reverse range", e);
        }
    }

    @Override
    public Long rank(K key, V value) {
        try {
            RedisResponse response = template.getConnection().sendCommand("ZRANK",
                template.serializeKey(key),
                template.serializeValue(value));
            String rankStr = response.getValue();
            return rankStr != null ? Long.parseLong(rankStr) : null;
        } catch (IOException e) {
            throw new RuntimeException("Error getting rank", e);
        }
    }

    @Override
    public Long reverseRank(K key, V value) {
        try {
            RedisResponse response = template.getConnection().sendCommand("ZREVRANK",
                template.serializeKey(key),
                template.serializeValue(value));
            String rankStr = response.getValue();
            return rankStr != null ? Long.parseLong(rankStr) : null;
        } catch (IOException e) {
            throw new RuntimeException("Error getting reverse rank", e);
        }
    }

    @Override
    public Long count(K key, double min, double max) {
        try {
            RedisResponse response = template.getConnection().sendCommand("ZCOUNT",
                template.serializeKey(key),
                String.valueOf(min),
                String.valueOf(max));
            return Long.parseLong(response.getValue());
        } catch (IOException e) {
            throw new RuntimeException("Error counting elements", e);
        }
    }

    @Override
    public Long intersectAndStore(K key, K otherKey, K destKey) {
        try {
            RedisResponse response = template.getConnection().sendCommand("ZINTERSTORE",
                template.serializeKey(destKey),
                "2",
                template.serializeKey(key),
                template.serializeKey(otherKey));
            return Long.parseLong(response.getValue());
        } catch (IOException e) {
            throw new RuntimeException("Error performing intersection", e);
        }
    }

    @Override
    public Long unionAndStore(K key, K otherKey, K destKey) {
        try {
            RedisResponse response = template.getConnection().sendCommand("ZUNIONSTORE",
                template.serializeKey(destKey),
                "2",
                template.serializeKey(key),
                template.serializeKey(otherKey));
            return Long.parseLong(response.getValue());
        } catch (IOException e) {
            throw new RuntimeException("Error performing union", e);
        }
    }
} 