package org.microspring.test.stereotype;

import org.microspring.stereotype.Service;
import org.microspring.beans.factory.annotation.Autowired;
import org.microspring.beans.factory.annotation.Qualifier;

@Service("testService")
public class TestService {
    private final TestRepository repository;

    @Autowired
    public TestService(@Qualifier("testRepo") TestRepository repository) {
        this.repository = repository;
    }

    public String getServiceData() {
        return "Service: " + repository.getData();
    }
} 