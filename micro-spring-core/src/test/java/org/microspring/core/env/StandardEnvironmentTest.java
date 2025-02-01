package org.microspring.core.env;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class StandardEnvironmentTest {
    private StandardEnvironment environment;

    @Before
    public void setUp() {
        environment = new StandardEnvironment(new String[]{"test"});
    }

    @Test
    public void testPropertyPriority() {
        // 1. Profile Properties 优先级最高
        assertEquals("from-profile-properties", environment.getProperty("app.name"));
        assertEquals("8083", environment.getProperty("server.port"));

        // 2. Profile YAML 次之
        assertEquals("from-profile-yaml", environment.getProperty("app.profile.specific"));

        // 3. 默认 Properties 再次之
        assertEquals("from-properties", environment.getProperty("app.default.properties"));

        // 4. 默认 YAML 优先级最低
        assertEquals("from-yaml", environment.getProperty("app.default.yaml"));
    }

    @Test
    public void testProfileOverride() {
        // Profile 配置应该覆盖默认配置
        assertEquals("from-profile-properties", environment.getProperty("app.name"));
        assertEquals("8083", environment.getProperty("server.port"));
    }

    @Test
    public void testPropertiesOverYaml() {
        // 在同一优先级下，properties 应该覆盖 yaml
        assertEquals("from-profile-properties", environment.getProperty("app.name"));
    }
} 