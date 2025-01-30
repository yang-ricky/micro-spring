package org.microspring.webflux;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.microspring.web.annotation.ExceptionHandler;
import org.microspring.web.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class WebFilterTest {
    private ReactiveHttpServer server;
    private static final int PORT = 8082;

    @RestControllerAdvice
    public static class TestExceptionHandler {
        @ExceptionHandler(IllegalArgumentException.class)
        public Mono<ReactiveServerResponse> handleIllegalArgument(IllegalArgumentException ex) {
            return Mono.just(new ReactiveServerResponse()
                .status(HttpResponseStatus.BAD_REQUEST)
                .write("Bad Request: " + ex.getMessage()));
        }
    }

    @Before
    public void setUp() {
        server = new ReactiveHttpServer(PORT);

        // Register exception handler
        server.registerExceptionHandler(new TestExceptionHandler());

        // Add authentication filter
        server.addFilter((request, response, chain) -> {
            String auth = request.getHeader("Authorization");
            if (auth != null && auth.equals("Bearer valid-token")) {
                return chain.filter(request, response);
            }
            response.status(HttpResponseStatus.UNAUTHORIZED);
            return Mono.empty();
        });

        // Add logging filter
        server.addFilter((request, response, chain) -> {
            return chain.filter(request, response);
        });

        // Start server with a simple handler
        server.start((request, response) -> {
            if (request.getUri().getPath().equals("/error")) {
                throw new IllegalArgumentException("Test error");
            }
            return Mono.just(response.body("Hello, World!"));
        });

        try {
            Thread.sleep(1000); // Wait for server to start
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
    public void testUnauthorizedAccess() throws IOException {
        URL url = new URL("http://localhost:" + PORT + "/api/secure");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        assertEquals(HttpResponseStatus.UNAUTHORIZED.code(), connection.getResponseCode());
    }

    @Test
    public void testAuthorizedAccess() throws IOException {
        URL url = new URL("http://localhost:" + PORT + "/api/secure");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Authorization", "Bearer valid-token");
        
        assertEquals(HttpResponseStatus.OK.code(), connection.getResponseCode());
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String response = in.lines().collect(Collectors.joining("\n"));
            assertEquals("Hello, World!", response);
        }
    }

    @Test
    public void testExceptionHandling() throws IOException {
        URL url = new URL("http://localhost:" + PORT + "/error");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Authorization", "Bearer valid-token");
        
        assertEquals(HttpResponseStatus.BAD_REQUEST.code(), connection.getResponseCode());
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
            String response = in.lines().collect(Collectors.joining("\n"));
            assertEquals("Bad Request: Test error", response);
        }
    }
} 