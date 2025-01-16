package org.microspring.test.stereotype;

import org.microspring.stereotype.Repository;

@Repository
public class DefaultNameRepository {
    public String getData() {
        return "default data";
    }
} 