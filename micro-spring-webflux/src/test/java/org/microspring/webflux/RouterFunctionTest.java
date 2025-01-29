package org.microspring.webflux;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class RouterFunctionTest {
    private ReactiveHttpServer server;
    private static final int PORT = 8081;

    @Before
    public void setUp() {
        RouterFunction router = RouterFunctionBuilder.route()
            .GET("/router/hello", request -> 
                Mono.just(ReactiveServerResponse.ok().body("Hello Router!")))
            .POST("/router/echo", request -> 
                request.getBody().map(body -> 
                    ReactiveServerResponse.ok().body("Router Echo: " + body)))
            .build();

        server = new ReactiveHttpServer(PORT);
        server.start((request, response) -> 
            router.route(request)
                .flatMap(handler -> handler.handle(request))
                .switchIfEmpty(Mono.just(response)
                    .doOnNext(resp -> {
                        resp.status(io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND);
                        resp.write("Not Found");
                    }))
                .flatMap(ReactiveServerResponse::end));

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
    public void testRouterGet() throws IOException {
        URL url = new URL("http://localhost:" + PORT + "/router/hello");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        assertEquals(200, connection.getResponseCode());
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String response = in.lines().collect(Collectors.joining("\n"));
            assertEquals("Hello Router!", response);
        }
    }

    @Test
    public void testRouterNotFound() throws IOException {
        URL url = new URL("http://localhost:" + PORT + "/router/nonexistent");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        assertEquals(404, connection.getResponseCode());
    }

    @Test(expected = IllegalStateException.class)
    public void testRouterDuplicateRoute() {
        RouterFunctionBuilder.route()
            .GET("/router/hello", request -> Mono.just(ReactiveServerResponse.ok().body("Hello")))
            .GET("/router/hello", request -> Mono.just(ReactiveServerResponse.ok().body("Hello Again")))
            .build();
    }
} 