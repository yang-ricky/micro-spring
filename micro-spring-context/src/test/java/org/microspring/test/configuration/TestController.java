package org.microspring.test.configuration;

public class TestController {
    private final TestService testService;
    
    public TestController(TestService testService) {
        this.testService = testService;
    }
    
    public String handle() {
        return testService.serve();
    }
    
    public TestService getTestService() {
        return testService;
    }
} 