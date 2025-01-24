package org.microspring.test.primary;

import org.microspring.context.annotation.Primary;
import org.microspring.stereotype.Component;

@Component
@Primary
public class MySQLDatabase implements Database {
    @Override
    public String getType() {
        return "mysql";
    }
} 