package org.microspring.webflux;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JsonHandlingTest {
    private ReactiveHttpServer server;
    private static final int PORT = 8080;

    // Test data class
    public static class User {
        private String name;
        private int age;

        // Default constructor for Jackson
        public User() {}

        public User(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getAge() { return age; }
        public void setAge(int age) { this.age = age; }
    }

    @Before
    public void setUp() {
        server = new ReactiveHttpServer(PORT);
        server.start((request, response) -> {
            String path = request.getUri().getPath();
            
            if ("/json".equals(path)) {
                // Handle JSON request
                return request.bodyToObject(User.class)
                        .map(user -> {
                            user.setAge(user.getAge() + 1); // Increment age
                            return user;
                        })
                        .map(user -> {
                            response.writeJson(user);
                            return response;
                        })
                        .onErrorResume(e -> {
                            response.status(io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST);
                            response.writeJson(new ErrorResponse("Invalid JSON request: " + e.getMessage()));
                            return Mono.just(response);
                        })
                        .flatMap(ReactiveServerResponse::end);
            }
            
            if ("/largedata".equals(path)) {
                // Generate some "large" data (just for testing)
                StringBuilder data = new StringBuilder();
                for (int i = 0; i < 100; i++) {
                    data.append("Line ").append(i).append("\n");
                }
                return Mono.just(response)
                        .doOnNext(resp -> resp.write(data.toString()))
                        .flatMap(ReactiveServerResponse::end);
            }

            return Mono.just(response)
                    .doOnNext(resp -> resp.write("Unknown path"))
                    .flatMap(ReactiveServerResponse::end);
        });

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // Error response class for JSON errors
    public static class ErrorResponse {
        private final String error;

        public ErrorResponse(String error) {
            this.error = error;
        }

        public String getError() {
            return error;
        }
    }

    @After
    public void tearDown() {
        server.shutdown();
    }

    @Test
    public void testJsonHandling() throws IOException {
        URL url = new URL("http://localhost:" + PORT + "/json");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");

        // Send JSON request
        String jsonInput = "{\"name\":\"Alice\",\"age\":25}";
        try (OutputStream os = connection.getOutputStream()) {
            os.write(jsonInput.getBytes(StandardCharsets.UTF_8));
        }

        // Read response
        assertEquals(200, connection.getResponseCode());
        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String response = in.lines().collect(Collectors.joining("\n"));
            assertTrue(response.contains("\"name\":\"Alice\""));
            assertTrue(response.contains("\"age\":26")); // Age should be incremented
        }
    }

    @Test
    public void testInvalidJsonSyntax() throws IOException {
        URL url = new URL("http://localhost:" + PORT + "/json");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");

        // Send invalid JSON
        String invalidJson = "{\"name\":\"Alice\",age:}"; // Invalid JSON syntax
        try (OutputStream os = connection.getOutputStream()) {
            os.write(invalidJson.getBytes(StandardCharsets.UTF_8));
        }

        assertEquals(400, connection.getResponseCode());
        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
            String response = in.lines().collect(Collectors.joining("\n"));
            assertTrue(response.contains("Invalid JSON request"));
        }
    }

    @Test
    public void testInvalidDataTypes() throws IOException {
        URL url = new URL("http://localhost:" + PORT + "/json");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");

        // Send JSON with wrong data type (string instead of number for age)
        String invalidTypeJson = "{\"name\":\"Alice\",\"age\":\"twenty-five\"}";
        try (OutputStream os = connection.getOutputStream()) {
            os.write(invalidTypeJson.getBytes(StandardCharsets.UTF_8));
        }

        assertEquals(400, connection.getResponseCode());
        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
            String response = in.lines().collect(Collectors.joining("\n"));
            assertTrue(response.contains("Invalid JSON request"));
        }
    }

    @Test
    public void testEmptyRequestBody() throws IOException {
        URL url = new URL("http://localhost:" + PORT + "/json");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");

        // Send empty body
        try (OutputStream os = connection.getOutputStream()) {
            os.write("".getBytes(StandardCharsets.UTF_8));
        }

        assertEquals(400, connection.getResponseCode());
        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
            String response = in.lines().collect(Collectors.joining("\n"));
            assertTrue(response.contains("Invalid JSON request"));
        }
    }

    @Test
    public void testLargeDataHandling() throws IOException {
        URL url = new URL("http://localhost:" + PORT + "/largedata");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        assertEquals(200, connection.getResponseCode());

        // Read response in chunks to simulate backpressure
        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            char[] buffer = new char[100]; // Small buffer to simulate chunked reading
            int read;
            int totalRead = 0;
            while ((read = in.read(buffer)) != -1) {
                response.append(buffer, 0, read);
                totalRead += read;
                // Simulate some processing time for each chunk
                Thread.sleep(10);
            }
            assertTrue(totalRead > 100); // Verify we got all the data
            assertTrue(response.toString().contains("Line 0"));
            assertTrue(response.toString().contains("Line 99"));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while reading response", e);
        }
    }
} 