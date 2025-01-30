package org.microspring.webflux;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.microspring.web.annotation.*;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class RequestMappingTest {
    private ReactiveHttpServer server;
    private static final int PORT = 8080;

    @RestController
    @RequestMapping("/api")
    public static class TestController {
        @RequestMapping("/users/{id}")
        public Mono<String> getUserById(
                @PathVariable("id") String userId,
                @RequestParam(value = "detail", required = false, defaultValue = "false") boolean detail) {
            return Mono.just(detail ? 
                "User " + userId + " details: {name: 'Test User', age: 25}" :
                "User " + userId);
        }

        @RequestMapping("/posts/{postId}/comments/{commentId}")
        public Mono<String> getComment(
                @PathVariable String postId,
                @PathVariable String commentId,
                @RequestParam(value = "format", defaultValue = "text") String format) {
            return Mono.just(String.format("Post %s, Comment %s, Format: %s", postId, commentId, format));
        }

        @RequestMapping("/search")
        public Mono<String> search(
                @RequestParam("q") String query,
                @RequestParam(value = "page", defaultValue = "1") int page,
                @RequestParam(value = "size", required = false, defaultValue = "10") int size) {
            return Mono.just(String.format("Search: %s, Page: %d, Size: %d", query, page, size));
        }
    }

    @Before
    public void setUp() {
        server = new ReactiveHttpServer(PORT);
        TestController controller = new TestController();
        ReactiveHandlerMapping handlerMapping = new ReactiveHandlerMapping();
        handlerMapping.registerController(controller);

        server.start((request, response) -> {
            HandlerMethod handler = handlerMapping.getHandler(
                request.getUri().getPath(),
                request.getMethod()
            );

            if (handler != null) {
                return handler.invoke(request)
                    .map(result -> {
                        response.write(result.toString());
                        return response;
                    })
                    .onErrorResume(e -> {
                        if (e instanceof IllegalArgumentException) {
                            response.status(io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST);
                            response.write("Bad Request: " + e.getMessage());
                        } else {
                            response.status(io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR);
                            response.write("Internal Server Error: " + e.getMessage());
                        }
                        return Mono.just(response);
                    })
                    .flatMap(ReactiveServerResponse::end);
            }

            response.status(io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND);
            return Mono.just(response)
                .doOnNext(resp -> resp.write("Not Found"))
                .flatMap(ReactiveServerResponse::end);
        });

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @After
    public void tearDown() {
        if (server != null) {
            server.shutdown();
        }
    }

    @Test
    public void testPathVariable() throws IOException {
        URL url = new URL("http://localhost:" + PORT + "/api/users/123");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        assertEquals(200, connection.getResponseCode());
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String response = in.lines().collect(Collectors.joining("\n"));
            assertEquals("User 123", response);
        }
    }

    @Test
    public void testPathVariableWithRequestParam() throws IOException {
        URL url = new URL("http://localhost:" + PORT + "/api/users/123?detail=true");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        assertEquals(200, connection.getResponseCode());
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String response = in.lines().collect(Collectors.joining("\n"));
            assertEquals("User 123 details: {name: 'Test User', age: 25}", response);
        }
    }

    @Test
    public void testMultiplePathVariables() throws IOException {
        URL url = new URL("http://localhost:" + PORT + "/api/posts/456/comments/789?format=json");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        assertEquals(200, connection.getResponseCode());
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String response = in.lines().collect(Collectors.joining("\n"));
            assertEquals("Post 456, Comment 789, Format: json", response);
        }
    }

    @Test
    public void testRequiredRequestParam() throws IOException {
        URL url = new URL("http://localhost:" + PORT + "/api/search?q=test&page=2&size=20");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        assertEquals(200, connection.getResponseCode());
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String response = in.lines().collect(Collectors.joining("\n"));
            assertEquals("Search: test, Page: 2, Size: 20", response);
        }
    }

    @Test
    public void testMissingRequiredRequestParam() throws IOException {
        URL url = new URL("http://localhost:" + PORT + "/api/search");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        assertEquals(400, connection.getResponseCode());
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
            String response = in.lines().collect(Collectors.joining("\n"));
            assertEquals("Bad Request: Required parameter 'q' is not present", response);
        }
    }

    @Test
    public void testDefaultValueRequestParam() throws IOException {
        URL url = new URL("http://localhost:" + PORT + "/api/search?q=test");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        assertEquals(200, connection.getResponseCode());
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String response = in.lines().collect(Collectors.joining("\n"));
            assertEquals("Search: test, Page: 1, Size: 10", response);
        }
    }
} 