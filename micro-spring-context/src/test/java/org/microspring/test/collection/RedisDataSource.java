package org.microspring.test.collection;

import org.microspring.stereotype.Component;

@Component
public class RedisDataSource implements DataSource {
    @Override
    public String getType() {
        return "Redis";
    }
} 