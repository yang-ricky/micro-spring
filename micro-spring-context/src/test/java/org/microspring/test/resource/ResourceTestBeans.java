package org.microspring.test.resource;

import org.microspring.stereotype.Component;
import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Component
public class ResourceTestBeans {
    
    // 测试按名称注入
    @Resource(name = "testService")
    private TestService namedService;
    
    // 测试按字段名注入
    @Resource
    private TestService testService;
    
    // 测试按类型注入（当按名称找不到时）
    @Resource
    private AnotherService anotherService;
    
    // 测试集合类型
    @Resource
    private List<TestService> serviceList;
    
    @Resource
    private Map<String, TestService> serviceMap;
    
    // 测试setter方法注入
    private TestService setterService;
    
    @Resource(name = "testService")
    public void setSetterService(TestService service) {
        this.setterService = service;
    }
    
    // Getters
    public TestService getNamedService() {
        return namedService;
    }
    
    public TestService getTestService() {
        return testService;
    }
    
    public AnotherService getAnotherService() {
        return anotherService;
    }
    
    public List<TestService> getServiceList() {
        return serviceList;
    }
    
    public Map<String, TestService> getServiceMap() {
        return serviceMap;
    }
    
    public TestService getSetterService() {
        return setterService;
    }
}

@Component
class TestService {
    private String name = "testService";
    
    public String getName() {
        return name;
    }
}

@Component
class AnotherService {
    private String name = "anotherService";
    
    public String getName() {
        return name;
    }
}

@Component("customNamedService")
class CustomNamedService extends TestService {
    @Override
    public String getName() {
        return "customNamedService";
    }
} 