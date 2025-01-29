package org.microspring.webflux;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.microspring.web.annotation.RestController;
import org.microspring.web.annotation.RequestMapping;
import org.microspring.web.annotation.RequestMethod;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AnnotationRoutingTest {
    private ReactiveHttpServer server;
    private static final int PORT = 8080;
    private ReactiveHandlerMapping handlerMapping;

    @RestController
    @RequestMapping("/api")
    public static class TestController {
        @RequestMapping(value = "/hello", method = RequestMethod.GET)
        public Mono<String> hello() {
            return Mono.just("Hello, WebFlux!");
        }

        @RequestMapping(value = "/echo", method = RequestMethod.POST)
        public Mono<String> echo(ReactiveServerRequest request) {
            return request.getBody().map(body -> "Echo: " + body);
        }
    }

    @RestController
    public static class ConflictController {
        @RequestMapping(value = "/api/hello", method = RequestMethod.GET)
        public Mono<String> conflictingHello() {
            return Mono.just("This should cause conflict!");
        }
    }

    @Before
    public void setUp() {
        handlerMapping = new ReactiveHandlerMapping();
        TestController testController = new TestController();
        handlerMapping.registerController(testController);

        server = new ReactiveHttpServer(PORT);
        server.start((request, response) -> {
            try {
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
                            System.err.println("Error handling request: " + e.getMessage());
                            response.status(io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR);
                            response.write("Internal Server Error: " + e.getMessage());
                            return Mono.just(response);
                        })
                        .flatMap(ReactiveServerResponse::end);
                }

                response.status(io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND);
                return Mono.just(response)
                    .doOnNext(resp -> resp.write("Not Found"))
                    .flatMap(ReactiveServerResponse::end);
            } catch (Exception e) {
                System.err.println("Unexpected error: " + e.getMessage());
                response.status(io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR);
                return Mono.just(response)
                    .doOnNext(resp -> resp.write("Internal Server Error"))
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
    public void testGetMapping() throws IOException {
        URL url = new URL("http://localhost:" + PORT + "/api/hello");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        assertEquals(200, connection.getResponseCode());
        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String response = in.lines().collect(Collectors.joining("\n"));
            assertEquals("Hello, WebFlux!", response);
        }
    }

    @Test
    public void testPostMapping() throws IOException {
        URL url = new URL("http://localhost:" + PORT + "/api/echo");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        
        String postData = "Hello, Server!";
        try (OutputStream os = connection.getOutputStream()) {
            os.write(postData.getBytes(StandardCharsets.UTF_8));
        }

        assertEquals(200, connection.getResponseCode());
        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String response = in.lines().collect(Collectors.joining("\n"));
            assertEquals("Echo: " + postData, response);
        }
    }

    @Test
    public void testNotFound() throws IOException {
        URL url = new URL("http://localhost:" + PORT + "/api/nonexistent");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        assertEquals(404, connection.getResponseCode());
    }

    @Test(expected = IllegalStateException.class)
    public void testRouteConflict() {
        ConflictController conflictController = new ConflictController();
        handlerMapping.registerController(conflictController);
    }
} 