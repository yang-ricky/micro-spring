package org.microspring.redis.core;

import org.microspring.redis.protocol.RedisResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Default implementation of ListOperations interface
 */
public class DefaultListOperations<K, V> implements ListOperations<K, V> {

    private final RedisTemplate<K, V> template;

    public DefaultListOperations(RedisTemplate<K, V> template) {
        this.template = template;
    }

    private List<V> parseArrayResponse(RedisResponse response) {
        if (response == null || response.getArray() == null) {
            return new ArrayList<>();
        }
        return response.getArray().stream()
            .map(RedisResponse::getValue)
            .map(template::deserializeValue)
            .collect(Collectors.toList());
    }

    @Override
    public Long leftPush(K key, V value) {
        try {
            RedisResponse response = template.getConnection().sendCommand("LPUSH",
                template.serializeKey(key),
                template.serializeValue(value));
            return Long.parseLong(response.getValue());
        } catch (IOException e) {
            throw new RuntimeException("Error pushing to list", e);
        }
    }

    @Override
    public Long leftPushAll(K key, V... values) {
        try {
            List<String> args = new ArrayList<>();
            args.add(template.serializeKey(key));
            Arrays.stream(values)
                .map(template::serializeValue)
                .forEach(args::add);

            RedisResponse response = template.getConnection().sendCommand("LPUSH",
                args.toArray(new String[0]));
            return Long.parseLong(response.getValue());
        } catch (IOException e) {
            throw new RuntimeException("Error pushing multiple values to list", e);
        }
    }

    @Override
    public Long rightPush(K key, V value) {
        try {
            RedisResponse response = template.getConnection().sendCommand("RPUSH",
                template.serializeKey(key),
                template.serializeValue(value));
            return Long.parseLong(response.getValue());
        } catch (IOException e) {
            throw new RuntimeException("Error pushing to list", e);
        }
    }

    @Override
    public Long rightPushAll(K key, V... values) {
        try {
            List<String> args = new ArrayList<>();
            args.add(template.serializeKey(key));
            Arrays.stream(values)
                .map(template::serializeValue)
                .forEach(args::add);

            RedisResponse response = template.getConnection().sendCommand("RPUSH",
                args.toArray(new String[0]));
            return Long.parseLong(response.getValue());
        } catch (IOException e) {
            throw new RuntimeException("Error pushing multiple values to list", e);
        }
    }

    @Override
    public V leftPop(K key) {
        try {
            RedisResponse response = template.getConnection().sendCommand("LPOP",
                template.serializeKey(key));
            return template.deserializeValue(response.getValue());
        } catch (IOException e) {
            throw new RuntimeException("Error popping from list", e);
        }
    }

    @Override
    public V rightPop(K key) {
        try {
            RedisResponse response = template.getConnection().sendCommand("RPOP",
                template.serializeKey(key));
            return template.deserializeValue(response.getValue());
        } catch (IOException e) {
            throw new RuntimeException("Error popping from list", e);
        }
    }

    @Override
    public List<V> range(K key, long start, long end) {
        try {
            RedisResponse response = template.getConnection().sendCommand("LRANGE",
                template.serializeKey(key),
                String.valueOf(start),
                String.valueOf(end));
            return parseArrayResponse(response);
        } catch (IOException e) {
            throw new RuntimeException("Error getting range from list", e);
        }
    }

    @Override
    public void trim(K key, long start, long end) {
        try {
            template.getConnection().sendCommand("LTRIM",
                template.serializeKey(key),
                String.valueOf(start),
                String.valueOf(end));
        } catch (IOException e) {
            throw new RuntimeException("Error trimming list", e);
        }
    }

    @Override
    public Long size(K key) {
        try {
            RedisResponse response = template.getConnection().sendCommand("LLEN",
                template.serializeKey(key));
            return Long.parseLong(response.getValue());
        } catch (IOException e) {
            throw new RuntimeException("Error getting list size", e);
        }
    }

    @Override
    public void set(K key, long index, V value) {
        try {
            template.getConnection().sendCommand("LSET",
                template.serializeKey(key),
                String.valueOf(index),
                template.serializeValue(value));
        } catch (IOException e) {
            throw new RuntimeException("Error setting list element", e);
        }
    }

    @Override
    public V index(K key, long index) {
        try {
            RedisResponse response = template.getConnection().sendCommand("LINDEX",
                template.serializeKey(key),
                String.valueOf(index));
            return template.deserializeValue(response.getValue());
        } catch (IOException e) {
            throw new RuntimeException("Error getting list element", e);
        }
    }

    @Override
    public Long remove(K key, long count, V value) {
        try {
            RedisResponse response = template.getConnection().sendCommand("LREM",
                template.serializeKey(key),
                String.valueOf(count),
                template.serializeValue(value));
            return Long.parseLong(response.getValue());
        } catch (IOException e) {
            throw new RuntimeException("Error removing from list", e);
        }
    }
} 