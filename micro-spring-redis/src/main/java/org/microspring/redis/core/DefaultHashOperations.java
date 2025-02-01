package org.microspring.redis.core;

import org.microspring.redis.protocol.RedisResponse;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * HashOperations 接口的默认实现
 */
@SuppressWarnings("unchecked")
public class DefaultHashOperations<H, HK, HV> implements HashOperations<H, HK, HV> {

    private final RedisTemplate<H, HV> template;

    public DefaultHashOperations(RedisTemplate<H, HV> template) {
        this.template = template;
    }

    private List<HV> parseArrayResponse(RedisResponse response) {
        if (response == null || response.getArray() == null) {
            return new ArrayList<>();
        }
        return response.getArray().stream()
            .map(RedisResponse::getValue)
            .map(template::deserializeHashValue)
            .collect(Collectors.toList());
    }

    @Override
    public void put(H key, HK hashKey, HV value) {
        try {
            template.getConnection().sendCommand("HSET",
                template.serializeKey(key),
                template.serializeHashKey((String)hashKey),
                template.serializeHashValue(value));
        } catch (IOException e) {
            throw new RuntimeException("Error setting hash value", e);
        }
    }

    @Override
    public void putAll(H key, Map<? extends HK, ? extends HV> map) {
        try {
            List<String> args = new ArrayList<>();
            args.add(template.serializeKey(key));
            map.forEach((hashKey, value) -> {
                args.add(template.serializeHashKey((String)hashKey));
                args.add(template.serializeHashValue(value));
            });

            template.getConnection().sendCommand("HMSET", args.toArray(new String[0]));
        } catch (IOException e) {
            throw new RuntimeException("Error setting multiple hash values", e);
        }
    }

    @Override
    public HV get(H key, HK hashKey) {
        try {
            RedisResponse response = template.getConnection().sendCommand("HGET",
                template.serializeKey(key),
                template.serializeHashKey((String)hashKey));
            return template.deserializeHashValue(response.getValue());
        } catch (IOException e) {
            throw new RuntimeException("Error getting hash value", e);
        }
    }

    @Override
    public Long delete(H key, Object... hashKeys) {
        try {
            List<String> args = new ArrayList<>();
            args.add(template.serializeKey(key));
            Arrays.stream(hashKeys)
                .map(hashKey -> template.serializeHashKey((String)hashKey))
                .forEach(args::add);

            RedisResponse response = template.getConnection().sendCommand("HDEL",
                args.toArray(new String[0]));
            return Long.parseLong(response.getValue());
        } catch (IOException e) {
            throw new RuntimeException("Error deleting hash fields", e);
        }
    }

    @Override
    public Boolean hasKey(H key, HK hashKey) {
        try {
            RedisResponse response = template.getConnection().sendCommand("HEXISTS",
                template.serializeKey(key),
                template.serializeHashKey((String)hashKey));
            return "1".equals(response.getValue());
        } catch (IOException e) {
            throw new RuntimeException("Error checking hash field existence", e);
        }
    }

    @Override
    public Map<HK, HV> entries(H key) {
        try {
            RedisResponse response = template.getConnection().sendCommand("HGETALL",
                template.serializeKey(key));
            List<RedisResponse> array = response.getArray();
            if (array == null || array.isEmpty()) {
                return new HashMap<>();
            }

            Map<HK, HV> result = new HashMap<>();
            for (int i = 0; i < array.size(); i += 2) {
                HK hashKey = (HK) template.deserializeHashKey(array.get(i).getValue());
                HV value = template.deserializeHashValue(array.get(i + 1).getValue());
                result.put(hashKey, value);
            }
            return result;
        } catch (IOException e) {
            throw new RuntimeException("Error getting all hash entries", e);
        }
    }

    @Override
    public Set<HK> keys(H key) {
        try {
            RedisResponse response = template.getConnection().sendCommand("HKEYS",
                template.serializeKey(key));
            return response.getArray().stream()
                .map(RedisResponse::getValue)
                .map(value -> (HK) template.deserializeHashKey(value))
                .collect(Collectors.toSet());
        } catch (IOException e) {
            throw new RuntimeException("Error getting hash keys", e);
        }
    }

    @Override
    public List<HV> values(H key) {
        try {
            RedisResponse response = template.getConnection().sendCommand("HVALS",
                template.serializeKey(key));
            return parseArrayResponse(response);
        } catch (IOException e) {
            throw new RuntimeException("Error getting hash values", e);
        }
    }

    @Override
    public Long size(H key) {
        try {
            RedisResponse response = template.getConnection().sendCommand("HLEN",
                template.serializeKey(key));
            return Long.parseLong(response.getValue());
        } catch (IOException e) {
            throw new RuntimeException("Error getting hash size", e);
        }
    }

    @Override
    public Long increment(H key, HK hashKey, long delta) {
        try {
            RedisResponse response = template.getConnection().sendCommand("HINCRBY",
                template.serializeKey(key),
                template.serializeHashKey((String)hashKey),
                String.valueOf(delta));
            return Long.parseLong(response.getValue());
        } catch (IOException e) {
            throw new RuntimeException("Error incrementing hash value", e);
        }
    }

    @Override
    public Double increment(H key, HK hashKey, double delta) {
        try {
            RedisResponse response = template.getConnection().sendCommand("HINCRBYFLOAT",
                template.serializeKey(key),
                template.serializeHashKey((String)hashKey),
                String.valueOf(delta));
            return Double.parseDouble(response.getValue());
        } catch (IOException e) {
            throw new RuntimeException("Error incrementing hash value", e);
        }
    }

    @Override
    public Boolean putIfAbsent(H key, HK hashKey, HV value) {
        try {
            RedisResponse response = template.getConnection().sendCommand("HSETNX",
                template.serializeKey(key),
                template.serializeHashKey((String)hashKey),
                template.serializeHashValue(value));
            return "1".equals(response.getValue());
        } catch (IOException e) {
            throw new RuntimeException("Error setting hash value if absent", e);
        }
    }
} 