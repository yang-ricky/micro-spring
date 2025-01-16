package org.microspring.test.stereotype;

import org.microspring.stereotype.Repository;

@Repository("testRepo")
public class TestRepository {
    public String getData() {
        return "test data";
    }
} 