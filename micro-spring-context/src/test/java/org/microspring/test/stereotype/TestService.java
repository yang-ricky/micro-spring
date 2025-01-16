package org.microspring.test.stereotype;

import org.microspring.stereotype.Service;

@Service("testService")
public class TestService {
    private final TestRepository repository;

    public TestService(TestRepository repository) {
        this.repository = repository;
    }

    public String getServiceData() {
        return "Service: " + repository.getData();
    }
} 