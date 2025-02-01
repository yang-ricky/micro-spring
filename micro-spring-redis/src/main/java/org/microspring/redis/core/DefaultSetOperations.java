package org.microspring.redis.core;

import org.microspring.redis.protocol.RedisResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.List;

/**
 * Default implementation of SetOperations interface.
 */
public class DefaultSetOperations<K, V> implements SetOperations<K, V> {

    private final RedisTemplate<K, V> template;

    public DefaultSetOperations(RedisTemplate<K, V> template) {
        this.template = template;
    }

    private Set<V> parseArrayResponse(RedisResponse response) {
        if (response == null || response.getArray() == null) {
            return new HashSet<>();
        }
        return response.getArray().stream()
            .map(RedisResponse::getValue)
            .map(template::deserializeValue)
            .collect(Collectors.toSet());
    }

    @Override
    public Long add(K key, V... values) {
        try {
            List<String> args = new ArrayList<>();
            args.add(template.serializeKey(key));
            Arrays.stream(values)
                .map(template::serializeValue)
                .forEach(args::add);
            
            RedisResponse response = template.getConnection().sendCommand("SADD", 
                args.toArray(new String[0]));
            return Long.parseLong(response.getValue());
        } catch (IOException e) {
            throw new RuntimeException("Error adding to set", e);
        }
    }

    @Override
    public Long remove(K key, V... values) {
        try {
            List<String> args = new ArrayList<>();
            args.add(template.serializeKey(key));
            Arrays.stream(values)
                .map(template::serializeValue)
                .forEach(args::add);
            
            RedisResponse response = template.getConnection().sendCommand("SREM", 
                args.toArray(new String[0]));
            return Long.parseLong(response.getValue());
        } catch (IOException e) {
            throw new RuntimeException("Error removing from set", e);
        }
    }

    @Override
    public Boolean isMember(K key, V value) {
        try {
            RedisResponse response = template.getConnection().sendCommand("SISMEMBER", 
                template.serializeKey(key),
                template.serializeValue(value));
            return "1".equals(response.getValue());
        } catch (IOException e) {
            throw new RuntimeException("Error checking set membership", e);
        }
    }

    @Override
    public Set<V> members(K key) {
        try {
            RedisResponse response = template.getConnection().sendCommand("SMEMBERS", 
                template.serializeKey(key));
            return parseArrayResponse(response);
        } catch (IOException e) {
            throw new RuntimeException("Error getting set members", e);
        }
    }

    @Override
    public Long size(K key) {
        try {
            RedisResponse response = template.getConnection().sendCommand("SCARD", 
                template.serializeKey(key));
            return Long.parseLong(response.getValue());
        } catch (IOException e) {
            throw new RuntimeException("Error getting set size", e);
        }
    }

    @Override
    public Set<V> intersect(K key, K otherKey) {
        try {
            RedisResponse response = template.getConnection().sendCommand("SINTER", 
                template.serializeKey(key),
                template.serializeKey(otherKey));
            return parseArrayResponse(response);
        } catch (IOException e) {
            throw new RuntimeException("Error performing set intersection", e);
        }
    }

    @Override
    public Set<V> union(K key, K otherKey) {
        try {
            RedisResponse response = template.getConnection().sendCommand("SUNION", 
                template.serializeKey(key),
                template.serializeKey(otherKey));
            return parseArrayResponse(response);
        } catch (IOException e) {
            throw new RuntimeException("Error performing set union", e);
        }
    }

    @Override
    public Set<V> difference(K key, K otherKey) {
        try {
            RedisResponse response = template.getConnection().sendCommand("SDIFF", 
                template.serializeKey(key),
                template.serializeKey(otherKey));
            return parseArrayResponse(response);
        } catch (IOException e) {
            throw new RuntimeException("Error performing set difference", e);
        }
    }

    @Override
    public Boolean move(K sourceKey, K destinationKey, V value) {
        try {
            RedisResponse response = template.getConnection().sendCommand("SMOVE", 
                template.serializeKey(sourceKey),
                template.serializeKey(destinationKey),
                template.serializeValue(value));
            return "1".equals(response.getValue());
        } catch (IOException e) {
            throw new RuntimeException("Error moving element between sets", e);
        }
    }

    @Override
    public V pop(K key) {
        try {
            RedisResponse response = template.getConnection().sendCommand("SPOP", 
                template.serializeKey(key));
            return template.deserializeValue(response.getValue());
        } catch (IOException e) {
            throw new RuntimeException("Error popping from set", e);
        }
    }

    @Override
    public V randomMember(K key) {
        try {
            RedisResponse response = template.getConnection().sendCommand("SRANDMEMBER", 
                template.serializeKey(key));
            return template.deserializeValue(response.getValue());
        } catch (IOException e) {
            throw new RuntimeException("Error getting random member", e);
        }
    }
} 