package org.microspring.webflux;

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
import static org.junit.Assert.assertTrue;

public class ExceptionHandlingTest {
    private ReactiveHttpServer server;
    private static final int PORT = 8080;

    @RestControllerAdvice
    public static class GlobalExceptionHandler {
        @ExceptionHandler(IllegalArgumentException.class)
        public Mono<String> handleIllegalArgument(IllegalArgumentException ex) {
            return Mono.just("Invalid argument: " + ex.getMessage());
        }

        @ExceptionHandler(RuntimeException.class)
        public Mono<ReactiveServerResponse> handleRuntime(RuntimeException ex) {
            return Mono.just(new ReactiveServerResponse()
                .status(io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR)
                .write("Server error: " + ex.getMessage()));
        }
    }

    @Before
    public void setUp() {
        server = new ReactiveHttpServer(PORT);
        server.registerExceptionHandler(new GlobalExceptionHandler());

        server.start((request, response) -> {
            String path = request.getUri().getPath();
            switch (path) {
                case "/error/argument":
                    throw new IllegalArgumentException("Invalid input");
                case "/error/runtime":
                    throw new RuntimeException("Something went wrong");
                default:
                    return Mono.just(response)
                        .doOnNext(resp -> resp.write("OK"))
                        .flatMap(ReactiveServerResponse::end);
            }
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
    public void testIllegalArgumentException() throws IOException {
        URL url = new URL("http://localhost:" + PORT + "/error/argument");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        assertEquals(200, connection.getResponseCode());
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String response = in.lines().collect(Collectors.joining("\n"));
            assertEquals("Invalid argument: Invalid input", response);
        }
    }

    @Test
    public void testRuntimeException() throws IOException {
        URL url = new URL("http://localhost:" + PORT + "/error/runtime");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        assertEquals(500, connection.getResponseCode());
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
            String response = in.lines().collect(Collectors.joining("\n"));
            assertEquals("Server error: Something went wrong", response);
        }
    }

    @Test
    public void testNormalRequest() throws IOException {
        URL url = new URL("http://localhost:" + PORT + "/normal");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        assertEquals(200, connection.getResponseCode());
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String response = in.lines().collect(Collectors.joining("\n"));
            assertEquals("OK", response);
        }
    }
} 