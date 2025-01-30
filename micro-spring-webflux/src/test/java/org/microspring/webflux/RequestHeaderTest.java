package org.microspring.webflux;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.microspring.web.annotation.RequestHeader;
import org.microspring.web.annotation.RequestMapping;
import org.microspring.web.annotation.RestController;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class RequestHeaderTest {
    private ReactiveHttpServer server;
    private static final int PORT = 8080;

    @RestController
    public static class HeaderTestController {
        @RequestMapping("/required-header")
        public Mono<String> withRequiredHeader(
                @RequestHeader("X-Required") String requiredHeader) {
            return Mono.just("Header value: " + requiredHeader);
        }

        @RequestMapping("/optional-header")
        public Mono<String> withOptionalHeader(
                @RequestHeader(value = "X-Optional", required = false, defaultValue = "default") String optionalHeader) {
            return Mono.just("Header value: " + optionalHeader);
        }

        @RequestMapping("/multiple-headers")
        public Mono<String> withMultipleHeaders(
                @RequestHeader("X-First") String first,
                @RequestHeader(value = "X-Second", required = false, defaultValue = "second-default") String second) {
            return Mono.just("First: " + first + ", Second: " + second);
        }
    }

    @Before
    public void setUp() {
        server = new ReactiveHttpServer(PORT);
        HeaderTestController controller = new HeaderTestController();
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
                            response.write("Missing required header");
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
    public void testRequiredHeader() throws IOException {
        URL url = new URL("http://localhost:" + PORT + "/required-header");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("X-Required", "test-value");

        assertEquals(200, connection.getResponseCode());
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String response = in.lines().collect(Collectors.joining("\n"));
            assertEquals("Header value: test-value", response);
        }
    }

    @Test
    public void testMissingRequiredHeader() throws IOException {
        URL url = new URL("http://localhost:" + PORT + "/required-header");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        assertEquals(400, connection.getResponseCode());
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
            String response = in.lines().collect(Collectors.joining("\n"));
            assertEquals("Missing required header", response);
        }
    }

    @Test
    public void testOptionalHeaderPresent() throws IOException {
        URL url = new URL("http://localhost:" + PORT + "/optional-header");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("X-Optional", "optional-value");

        assertEquals(200, connection.getResponseCode());
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String response = in.lines().collect(Collectors.joining("\n"));
            assertEquals("Header value: optional-value", response);
        }
    }

    @Test
    public void testOptionalHeaderMissing() throws IOException {
        URL url = new URL("http://localhost:" + PORT + "/optional-header");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        assertEquals(200, connection.getResponseCode());
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String response = in.lines().collect(Collectors.joining("\n"));
            assertEquals("Header value: default", response);
        }
    }

    @Test
    public void testMultipleHeaders() throws IOException {
        URL url = new URL("http://localhost:" + PORT + "/multiple-headers");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("X-First", "first-value");
        connection.setRequestProperty("X-Second", "second-value");

        assertEquals(200, connection.getResponseCode());
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String response = in.lines().collect(Collectors.joining("\n"));
            assertEquals("First: first-value, Second: second-value", response);
        }
    }
} 