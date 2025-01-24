package org.microspring.test.collection;

import org.microspring.stereotype.Component;

@Component
public class MongoDataSource implements DataSource {
    @Override
    public String getType() {
        return "MongoDB";
    }
} 