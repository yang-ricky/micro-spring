package org.microspring.redis.core;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.microspring.redis.RedisConnection;
import org.microspring.redis.serializer.JsonRedisSerializer;
import org.microspring.redis.serializer.StringRedisSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class RedisTemplateTest {
    private static final Logger logger = LoggerFactory.getLogger(RedisTemplateTest.class);
    private static RedisServer redisServer;
    private static int redisPort;
    private RedisConnection connection;
    private RedisTemplate<String, String> template;
    private RedisTemplate<String, TestUser> jsonTemplate;
    
    // Test class for JSON serialization
    public static class TestUser {
        private String name;
        private int age;
        
        public TestUser() {} // For Jackson
        
        public TestUser(String name, int age) {
            this.name = name;
            this.age = age;
        }
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
        
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestUser testUser = (TestUser) o;
            return age == testUser.age && name.equals(testUser.name);
        }
    }
    
    private static int findFreePort() throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(0)) {
            return serverSocket.getLocalPort();
        }
    }
    
    @BeforeClass
    public static void setUpClass() throws Exception {
        try {
            redisPort = findFreePort();
            logger.info("Starting embedded Redis on port {}", redisPort);
            
            redisServer = new RedisServer(redisPort);
            redisServer.start();
            logger.info("Embedded Redis started successfully");
        } catch (Exception e) {
            logger.error("Failed to start embedded Redis", e);
            throw e;
        }
    }
    
    @AfterClass
    public static void tearDownClass() {
        try {
            if (redisServer != null) {
                redisServer.stop();
                logger.info("Embedded Redis stopped");
            }
        } catch (Exception e) {
            logger.error("Error stopping Redis server", e);
        }
    }
    
    @Before
    public void setUp() throws Exception {
        try {
            connection = new RedisConnection();
            connection.connect("localhost", redisPort);
            
            // Setup string template
            template = new RedisTemplate<>();
            template.setConnection(connection);
            template.setKeySerializer(new StringRedisSerializer());
            template.setValueSerializer(new StringRedisSerializer());
            
            // Setup JSON template
            jsonTemplate = new RedisTemplate<>();
            jsonTemplate.setConnection(connection);
            jsonTemplate.setKeySerializer(new StringRedisSerializer());
            jsonTemplate.setValueSerializer(new JsonRedisSerializer<>(TestUser.class));
            
            logger.info("Test connection established");
        } catch (Exception e) {
            logger.error("Failed to establish test connection", e);
            throw e;
        }
    }
    
    @After
    public void tearDown() throws Exception {
        if (connection != null) {
            connection.close();
            logger.info("Test connection closed");
        }
    }
    
    @Test
    public void testBasicOperations() {
        ValueOperations<String, String> ops = template.opsForValue();
        
        String key = "test:template:key";
        String value = "template test value";
        
        logger.info("Testing SET operation");
        ops.set(key, value);
        
        logger.info("Testing GET operation");
        String retrieved = ops.get(key);
        assertEquals(value, retrieved);
        
        logger.info("Testing INCREMENT operation");
        String counterKey = "test:template:counter";
        ops.set(counterKey, "10");
        Long newValue = ops.increment(counterKey);
        assertEquals(Long.valueOf(11), newValue);
        
        logger.info("Testing DECREMENT operation");
        newValue = ops.decrement(counterKey);
        assertEquals(Long.valueOf(10), newValue);
        
        logger.info("Testing SETNX operation");
        Boolean setResult = ops.setIfAbsent(key, "new value");
        assertFalse(setResult); // Should return false as key already exists
        assertEquals(value, ops.get(key)); // Original value should remain
        
        logger.info("Testing APPEND operation");
        String appendKey = "test:template:append";
        ops.set(appendKey, "Hello");
        ops.append(appendKey, " World");
        assertEquals("Hello World", ops.get(appendKey));
    }
    
    @Test
    public void testSetOperations() {
        SetOperations<String, String> ops = template.opsForSet();
        
        String key1 = "test:set:1";
        String key2 = "test:set:2";
        
        logger.info("Testing SADD operation");
        Long addCount = ops.add(key1, "a", "b", "c");
        assertEquals(Long.valueOf(3), addCount);
        
        logger.info("Testing SCARD operation");
        Long size = ops.size(key1);
        assertEquals(Long.valueOf(3), size);
        
        logger.info("Testing SISMEMBER operation");
        assertTrue(ops.isMember(key1, "a"));
        assertFalse(ops.isMember(key1, "d"));
        
        logger.info("Testing SMEMBERS operation");
        Set<String> members = ops.members(key1);
        assertEquals(new HashSet<>(Arrays.asList("a", "b", "c")), members);
        
        logger.info("Testing set operations between two sets");
        ops.add(key2, "c", "d", "e");
        
        Set<String> intersection = ops.intersect(key1, key2);
        assertEquals(new HashSet<>(Arrays.asList("c")), intersection);
        
        Set<String> union = ops.union(key1, key2);
        assertEquals(new HashSet<>(Arrays.asList("a", "b", "c", "d", "e")), union);
        
        Set<String> difference = ops.difference(key1, key2);
        assertEquals(new HashSet<>(Arrays.asList("a", "b")), difference);
    }
    
    @Test
    public void testJsonSerialization() {
        ValueOperations<String, TestUser> ops = jsonTemplate.opsForValue();
        
        String key = "test:json:user";
        TestUser user = new TestUser("John Doe", 30);
        
        logger.info("Testing JSON SET operation");
        ops.set(key, user);
        
        logger.info("Testing JSON GET operation");
        TestUser retrieved = ops.get(key);
        assertEquals(user, retrieved);
        assertEquals("John Doe", retrieved.getName());
        assertEquals(30, retrieved.getAge());
    }
} 