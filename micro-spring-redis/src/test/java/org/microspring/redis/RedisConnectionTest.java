package org.microspring.redis;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.embedded.RedisServer;

import java.io.IOException;
import java.net.ServerSocket;

import static org.junit.Assert.assertEquals;

public class RedisConnectionTest {
    private static final Logger logger = LoggerFactory.getLogger(RedisConnectionTest.class);
    private static RedisServer redisServer;
    private static int redisPort;
    private RedisConnection connection;
    
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
            logger.info("Test connection established");
        } catch (Exception e) {
            logger.error("Failed to establish test connection", e);
            throw e;
        }
    }
    
    @After
    public void tearDown() {
        try {
            if (connection != null) {
                connection.close();
                logger.info("Test connection closed");
            }
        } catch (Exception e) {
            logger.error("Error closing test connection", e);
        }
    }
    
    @Test
    public void testPing() throws Exception {
        logger.info("Testing PING command");
        String response = connection.ping();
        assertEquals("PONG", response);
    }
    
    @Test
    public void testEcho() throws Exception {
        String message = "Hello Redis";
        logger.info("Testing ECHO command with message: {}", message);
        String response = connection.echo(message);
        assertEquals(message, response);
    }
    
    @Test
    public void testSetAndGet() throws Exception {
        String key = "test:key";
        String value = "test value";
        
        logger.info("Testing SET command with key: {}, value: {}", key, value);
        String setResponse = connection.set(key, value);
        assertEquals("OK", setResponse);
        
        logger.info("Testing GET command with key: {}", key);
        String getResponse = connection.get(key);
        assertEquals(value, getResponse);
    }
} 