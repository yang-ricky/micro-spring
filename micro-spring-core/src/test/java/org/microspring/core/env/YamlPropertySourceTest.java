package org.microspring.core.env;

import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import static org.junit.Assert.*;

public class YamlPropertySourceTest {

    private YamlPropertySource yamlPropertySource;
    private File tempYamlFile;

    @Before
    public void setUp() throws IOException {
        // 创建临时的 YAML 文件
        tempYamlFile = File.createTempFile("application", ".yml");
        try (FileWriter writer = new FileWriter(tempYamlFile)) {
            writer.write("app:\n");
            writer.write("  name: micro-spring\n");
            writer.write("  version: 1.0.0\n");
            writer.write("server:\n");
            writer.write("  port: 8080\n");
            writer.write("kafka:\n");
            writer.write("  bootstrap:\n");
            writer.write("    servers: localhost:9092\n");
            writer.write("  producer:\n");
            writer.write("    batch:\n");
            writer.write("      size: 16384\n");
            writer.write("  consumer:\n");
            writer.write("    enable:\n");
            writer.write("      auto:\n");
            writer.write("        commit: true\n");
        }

        // 读取 YAML 内容
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(tempYamlFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        yamlPropertySource = YamlPropertySource.fromYaml("application", content.toString());
    }

    @Test
    public void testGetSimpleProperty() {
        assertEquals("micro-spring", yamlPropertySource.getProperty("app.name"));
        assertEquals("1.0.0", yamlPropertySource.getProperty("app.version"));
        assertEquals("8080", yamlPropertySource.getProperty("server.port").toString());
    }

    @Test
    public void testGetNestedProperty() {
        assertEquals("localhost:9092", yamlPropertySource.getProperty("kafka.bootstrap.servers"));
        assertEquals("16384", yamlPropertySource.getProperty("kafka.producer.batch.size").toString());
        assertEquals("true", yamlPropertySource.getProperty("kafka.consumer.enable.auto.commit").toString());
    }

    @Test
    public void testNonExistentProperty() {
        assertNull(yamlPropertySource.getProperty("non.existent.property"));
    }

    @Test
    public void testPartialPath() {
        Object serverConfig = yamlPropertySource.getProperty("server");
        assertTrue(serverConfig instanceof java.util.Map);
    }

    @Test
    public void cleanup() throws IOException {
        if (tempYamlFile != null && tempYamlFile.exists()) {
            tempYamlFile.delete();
        }
    }
}