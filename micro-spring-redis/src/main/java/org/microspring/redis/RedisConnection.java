package org.microspring.redis;

import org.microspring.redis.protocol.RedisCommand;
import org.microspring.redis.protocol.RedisResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class RedisConnection implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(RedisConnection.class);

    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
        logger.info("Connected to Redis at {}:{}", host, port);
    }

    @Override
    public void close() throws IOException {
        if (socket != null && !socket.isClosed()) {
            socket.close();
            logger.info("Connection closed");
        }
    }

    public RedisResponse sendCommand(String command, String... args) throws IOException {
        RedisCommand redisCommand = new RedisCommand(command, args);
        String formatted = redisCommand.format();
        
        logger.debug("Sending command: {}", redisCommand);
        logger.debug("Protocol: \n{}", formatted);
        
        writer.write(formatted);
        writer.flush();
        
        RedisResponse response = RedisResponse.read(reader);
        logger.debug("Received response: {}", response);
        
        return response;
    }

    // Basic Redis Commands
    public String ping() throws IOException {
        return sendCommand("PING").toString();
    }

    public String echo(String message) throws IOException {
        return sendCommand("ECHO", message).toString();
    }

    public String set(String key, String value) throws IOException {
        return sendCommand("SET", key, value).toString();
    }

    public String get(String key) throws IOException {
        return sendCommand("GET", key).toString();
    }

    // Main method for testing
    public static void main(String[] args) {
        try (RedisConnection conn = new RedisConnection()) {
            conn.connect("localhost", 6379);
            
            // Test PING
            logger.info("PING response: {}", conn.ping());
            
            // Test ECHO
            logger.info("ECHO response: {}", conn.echo("Hello Redis!"));
            
            // Test SET and GET
            String key = "test:key";
            String value = "Hello from Micro-Spring-Redis!";
            
            logger.info("SET response: {}", conn.set(key, value));
            logger.info("GET response: {}", conn.get(key));
            
        } catch (IOException e) {
            logger.error("Error during Redis operations", e);
        }
    }
} 