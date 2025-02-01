package org.microspring.redis.serializer;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Simple String to byte[] serializer.
 * Uses UTF-8 by default.
 */
public class StringRedisSerializer implements RedisSerializer<String> {

    private final Charset charset;

    public StringRedisSerializer() {
        this(StandardCharsets.UTF_8);
    }

    public StringRedisSerializer(Charset charset) {
        this.charset = charset;
    }

    @Override
    public byte[] serialize(String string) {
        return (string == null ? null : string.getBytes(charset));
    }

    @Override
    public String deserialize(byte[] bytes) {
        return (bytes == null ? null : new String(bytes, charset));
    }
} 