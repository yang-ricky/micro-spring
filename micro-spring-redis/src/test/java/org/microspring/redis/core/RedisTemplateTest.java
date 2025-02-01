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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.ArrayList;

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
            
            redisServer = new RedisServer(redisPort);
            redisServer.start();
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
            JsonRedisSerializer<TestUser> jsonSerializer = new JsonRedisSerializer<>(TestUser.class);
            jsonTemplate = new RedisTemplate<>();
            jsonTemplate.setConnection(connection);
            jsonTemplate.setKeySerializer(new StringRedisSerializer());
            jsonTemplate.setValueSerializer(jsonSerializer);
            jsonTemplate.setHashValueSerializer(jsonSerializer);
            
        } catch (Exception e) {
            logger.error("Failed to establish test connection", e);
            throw e;
        }
    }
    
    @After
    public void tearDown() throws Exception {
        if (connection != null) {
            connection.close();
        }
    }
    
    @Test
    public void testBasicOperations() {
        ValueOperations<String, String> ops = template.opsForValue();
        
        String key = "test:template:key";
        String value = "template test value";
        
        ops.set(key, value);
        
        String retrieved = ops.get(key);
        assertEquals(value, retrieved);
        
        String counterKey = "test:template:counter";
        ops.set(counterKey, "10");
        Long newValue = ops.increment(counterKey);
        assertEquals(Long.valueOf(11), newValue);
        
        newValue = ops.decrement(counterKey);
        assertEquals(Long.valueOf(10), newValue);
        
        Boolean setResult = ops.setIfAbsent(key, "new value");
        assertFalse(setResult); // Should return false as key already exists
        assertEquals(value, ops.get(key)); // Original value should remain
        
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
        
        Long addCount = ops.add(key1, "a", "b", "c");
        assertEquals(Long.valueOf(3), addCount);
        
        Long size = ops.size(key1);
        assertEquals(Long.valueOf(3), size);
        
        assertTrue(ops.isMember(key1, "a"));
        assertFalse(ops.isMember(key1, "d"));
        
        Set<String> members = ops.members(key1);
        assertEquals(new HashSet<>(Arrays.asList("a", "b", "c")), members);
        
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
        
        ops.set(key, user);
        
        TestUser retrieved = ops.get(key);
        assertEquals(user, retrieved);
        assertEquals("John Doe", retrieved.getName());
        assertEquals(30, retrieved.getAge());
    }

    @Test
    public void testListOperations() {
        ListOperations<String, String> ops = template.opsForList();
        String key = "test:list";

        Long pushCount = ops.leftPush(key, "first");
        assertEquals(Long.valueOf(1), pushCount);

        pushCount = ops.rightPush(key, "last");
        assertEquals(Long.valueOf(2), pushCount);

        pushCount = ops.leftPushAll(key, "one", "two", "three");
        assertEquals(Long.valueOf(5), pushCount);

        Long size = ops.size(key);
        assertEquals(Long.valueOf(5), size);

        List<String> range = ops.range(key, 0, -1);
        assertEquals(Arrays.asList("three", "two", "one", "first", "last"), range);

        String element = ops.index(key, 2);
        assertEquals("one", element);

        ops.set(key, 1, "modified");
        range = ops.range(key, 0, -1);
        assertEquals(Arrays.asList("three", "modified", "one", "first", "last"), range);

        String popped = ops.leftPop(key);
        assertEquals("three", popped);

        popped = ops.rightPop(key);
        assertEquals("last", popped);

        ops.trim(key, 0, 1);
        range = ops.range(key, 0, -1);
        assertEquals(Arrays.asList("modified", "one"), range);

        ops.rightPushAll(key, "one", "one", "one");
        Long removed = ops.remove(key, 2, "one");
        assertEquals(Long.valueOf(2), removed);
        range = ops.range(key, 0, -1);
        assertEquals(Arrays.asList("modified", "one", "one"), range);
    }

    @Test
    public void testHashOperations() {
        HashOperations<String, String, String> ops = template.opsForHash();
        String key = "test:hash";

        ops.put(key, "field1", "value1");
        ops.put(key, "field2", "value2");

        String value = ops.get(key, "field1");
        assertEquals("value1", value);

        Map<String, String> map = new HashMap<>();
        map.put("field3", "value3");
        map.put("field4", "value4");
        ops.putAll(key, map);

        Long size = ops.size(key);
        assertEquals(Long.valueOf(4), size);

        assertTrue(ops.hasKey(key, "field1"));
        assertFalse(ops.hasKey(key, "nonexistent"));

        Map<String, String> entries = ops.entries(key);
        assertEquals(4, entries.size());
        assertEquals("value1", entries.get("field1"));
        assertEquals("value2", entries.get("field2"));
        assertEquals("value3", entries.get("field3"));
        assertEquals("value4", entries.get("field4"));

        Set<String> keys = ops.keys(key);
        assertEquals(new HashSet<>(Arrays.asList("field1", "field2", "field3", "field4")), keys);

        List<String> values = ops.values(key);
        assertTrue(values.containsAll(Arrays.asList("value1", "value2", "value3", "value4")));

        Long deleted = ops.delete(key, "field1", "field2");
        assertEquals(Long.valueOf(2), deleted);
        assertNull(ops.get(key, "field1"));

        ops.put(key, "counter", "10");
        Long newValue = ops.increment(key, "counter", 5);
        assertEquals(Long.valueOf(15), newValue);

        ops.put(key, "price", "10.5");
        Double newPrice = ops.increment(key, "price", 2.5);
        assertEquals(Double.valueOf(13.0), newPrice);

        assertTrue(ops.putIfAbsent(key, "newfield", "newvalue"));
        assertFalse(ops.putIfAbsent(key, "newfield", "anothervalue"));
        assertEquals("newvalue", ops.get(key, "newfield"));
    }

    @Test
    public void testHashOperationsWithJson() {
        HashOperations<String, String, TestUser> ops = jsonTemplate.opsForHash();
        String key = "test:hash:users";

        TestUser user1 = new TestUser("John", 30);
        TestUser user2 = new TestUser("Jane", 25);

        ops.put(key, "user1", user1);
        ops.put(key, "user2", user2);

        TestUser retrieved1 = ops.get(key, "user1");
        assertEquals(user1, retrieved1);
        assertEquals("John", retrieved1.getName());
        assertEquals(30, retrieved1.getAge());

        Map<String, TestUser> entries = ops.entries(key);
        assertEquals(2, entries.size());
        assertEquals(user1, entries.get("user1"));
        assertEquals(user2, entries.get("user2"));
    }

    @Test
    public void testZSetOperations() {
        ZSetOperations<String, String> ops = template.opsForZSet();
        String key = "test:zset";

        assertTrue(ops.add(key, "a", 1.0));
        assertTrue(ops.add(key, "b", 2.0));
        assertTrue(ops.add(key, "c", 3.0));

        assertEquals(Long.valueOf(3), ops.size(key));

        assertEquals(Double.valueOf(1.0), ops.score(key, "a"));
        assertEquals(Double.valueOf(2.0), ops.score(key, "b"));
        assertEquals(Double.valueOf(3.0), ops.score(key, "c"));

        assertEquals(Double.valueOf(2.0), ops.incrementScore(key, "a", 1.0));

        assertEquals(Long.valueOf(0), ops.rank(key, "a"));
        assertEquals(Long.valueOf(1), ops.rank(key, "b"));
        assertEquals(Long.valueOf(2), ops.rank(key, "c"));

        assertEquals(Long.valueOf(2), ops.reverseRank(key, "a"));
        assertEquals(Long.valueOf(1), ops.reverseRank(key, "b"));
        assertEquals(Long.valueOf(0), ops.reverseRank(key, "c"));

        Set<String> range = ops.range(key, 0, -1);
        assertEquals(new LinkedHashSet<>(Arrays.asList("a", "b", "c")), range);

        Set<String> revRange = ops.reverseRange(key, 0, -1);
        assertEquals(new LinkedHashSet<>(Arrays.asList("c", "b", "a")), revRange);

        Set<String> scoreRange = ops.rangeByScore(key, 1.0, 2.0);
        assertEquals(new LinkedHashSet<>(Arrays.asList("a", "b")), scoreRange);

        Set<String> revScoreRange = ops.reverseRangeByScore(key, 1.0, 2.0);
        assertEquals(new LinkedHashSet<>(Arrays.asList("b", "a")), revScoreRange);

        assertEquals(Long.valueOf(2), ops.count(key, 1.0, 2.0));

        assertEquals(Long.valueOf(2), ops.remove(key, "a", "b"));
        assertEquals(Long.valueOf(1), ops.size(key));

        // Test intersection and union
        String key2 = "test:zset2";
        ops.add(key2, "c", 1.0);
        ops.add(key2, "d", 2.0);

        String destKey = "test:zset:dest";
        assertEquals(Long.valueOf(1), ops.intersectAndStore(key, key2, destKey));
        assertEquals(new LinkedHashSet<>(Collections.singletonList("c")), ops.range(destKey, 0, -1));

        assertEquals(Long.valueOf(2), ops.unionAndStore(key, key2, "test:zset:union"));
        assertEquals(new LinkedHashSet<>(Arrays.asList("c", "d")), ops.range("test:zset:union", 0, -1));
    }

    @Test
    public void testZSetOperationsWithJson() {
        ZSetOperations<String, TestUser> ops = jsonTemplate.opsForZSet();
        String key = "test:zset:users";

        TestUser user1 = new TestUser("John", 30);
        TestUser user2 = new TestUser("Jane", 25);
        TestUser user3 = new TestUser("Bob", 35);

        assertTrue(ops.add(key, user1, 30.0));
        assertTrue(ops.add(key, user2, 25.0));
        assertTrue(ops.add(key, user3, 35.0));

        assertEquals(Double.valueOf(30.0), ops.score(key, user1));
        assertEquals(Long.valueOf(1), ops.rank(key, user1));

        Set<TestUser> users = ops.range(key, 0, -1);
        List<TestUser> userList = new ArrayList<>(users);
        assertEquals(3, userList.size());
        assertEquals("Jane", userList.get(0).getName());
        assertEquals("John", userList.get(1).getName());
        assertEquals("Bob", userList.get(2).getName());

        Set<TestUser> reverseUsers = ops.reverseRange(key, 0, -1);
        List<TestUser> reverseUserList = new ArrayList<>(reverseUsers);
        assertEquals("Bob", reverseUserList.get(0).getName());
        assertEquals("John", reverseUserList.get(1).getName());
        assertEquals("Jane", reverseUserList.get(2).getName());
    }
} 