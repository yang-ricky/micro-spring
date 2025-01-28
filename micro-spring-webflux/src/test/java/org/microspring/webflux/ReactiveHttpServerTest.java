package org.microspring.webflux;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import reactor.core.publisher.Mono;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ReactiveHttpServerTest {
    private ReactiveHttpServer server;
    private static final int PORT = 8080;

    @Before
    public void setUp() {
        server = new ReactiveHttpServer(PORT);
        server.start((request, response) -> {
            System.out.println("Received request: " + request.getMethod() + " " + request.getUri());
            
            // 根据不同的路径返回不同的响应
            String path = request.getUri().getPath();
            switch (path) {
                case "/error":
                    return Mono.just(response)
                            .doOnNext(resp -> {
                                resp.status(io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR);
                                resp.write("Internal Server Error");
                            })
                            .flatMap(ReactiveServerResponse::end);
                case "/echo":
                    return request.getBody()
                            .map(body -> {
                                response.write("Echo: " + body);
                                return response;
                            })
                            .flatMap(ReactiveServerResponse::end);
                default:
                    return Mono.just(response)
                            .doOnNext(resp -> resp.write("Hello from ReactiveHttpServer"))
                            .flatMap(ReactiveServerResponse::end);
            }
        });

        // Give the server a moment to start
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @After
    public void tearDown() {
        server.shutdown();
    }

    @Test
    public void testBasicGetRequest() throws IOException {
        URL url = new URL("http://localhost:" + PORT + "/anything");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        
        int responseCode = connection.getResponseCode();
        assertEquals(200, responseCode);

        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            String response = in.lines().collect(Collectors.joining("\n"));
            assertEquals("Hello from ReactiveHttpServer", response);
        }
    }

    @Test
    public void testPostRequest() throws IOException {
        URL url = new URL("http://localhost:" + PORT + "/echo");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "text/plain");

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
    public void testErrorResponse() throws IOException {
        URL url = new URL("http://localhost:" + PORT + "/error");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        assertEquals(500, connection.getResponseCode());

        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getErrorStream(), StandardCharsets.UTF_8))) {
            String response = in.lines().collect(Collectors.joining("\n"));
            assertEquals("Internal Server Error", response);
        }
    }

    @Test
    public void testConcurrentRequests() throws InterruptedException {
        int numThreads = 2;
        int requestsPerThread = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads * requestsPerThread);
        List<Exception> exceptions = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            executor.submit(() -> {
                for (int j = 0; j < requestsPerThread; j++) {
                    try {
                        URL url = new URL("http://localhost:" + PORT + "/anything");
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("GET");
                        
                        int responseCode = connection.getResponseCode();
                        assertEquals(200, responseCode);

                        try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                            String response = in.lines().collect(Collectors.joining("\n"));
                            assertEquals("Hello from ReactiveHttpServer", response);
                        }
                    } catch (Exception e) {
                        synchronized (exceptions) {
                            exceptions.add(e);
                        }
                    } finally {
                        latch.countDown();
                    }
                }
            });
        }

        assertTrue("Timeout waiting for concurrent requests", 
                  latch.await(30, TimeUnit.SECONDS));
        executor.shutdown();
        assertTrue("Some concurrent requests failed", exceptions.isEmpty());
    }
} 