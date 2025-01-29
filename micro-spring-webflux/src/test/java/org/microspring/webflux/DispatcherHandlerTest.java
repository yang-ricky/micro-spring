package org.microspring.webflux;

import io.netty.handler.codec.http.HttpResponseStatus;
import org.junit.Test;
import org.microspring.web.annotation.RequestMapping;
import org.microspring.web.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.Assert.assertEquals;

public class DispatcherHandlerTest {

    @RestController
    @RequestMapping("/api")
    static class TestController {
        @RequestMapping("/hello")
        public Mono<String> hello() {
            return Mono.just("Hello from @RestController!");
        }

        @RequestMapping("/shared")
        public Mono<String> shared() {
            return Mono.just("Hello from @RestController shared!");
        }
    }

    @Test
    public void testRoutingPriority_WhenBothHandlersExist() {
        // Set up router function for the same path as controller
        RouterFunction routerFunction = RouterFunctionBuilder.route()
            .GET("/api/shared", request -> 
                Mono.just(ReactiveServerResponse.ok().body("Hello from RouterFunction shared!")))
            .build();

        // Set up handler mapping
        ReactiveHandlerMapping handlerMapping = new ReactiveHandlerMapping();
        handlerMapping.registerController(new TestController());

        // Create dispatcher
        DispatcherHandler dispatcher = new DispatcherHandler(routerFunction, handlerMapping);

        // Create test request
        ReactiveServerRequest request = new ReactiveServerRequest(
            io.netty.handler.codec.http.HttpMethod.GET,
            java.net.URI.create("/api/shared"),
            new io.netty.handler.codec.http.DefaultHttpHeaders(),
            Mono.empty()
        );

        // Test that router function takes priority over controller
        StepVerifier.create(dispatcher.handle(request))
            .assertNext(response -> {
                assertEquals(HttpResponseStatus.OK, response.getStatus());
                assertEquals("Hello from RouterFunction shared!", response.getBody());
            })
            .verifyComplete();
    }

    @Test
    public void testFallbackToAnnotation_WhenRouterDoesNotMatch() {
        // Set up router function with different path
        RouterFunction routerFunction = RouterFunctionBuilder.route()
            .GET("/other/path", request -> 
                Mono.just(ReactiveServerResponse.ok().body("Hello from RouterFunction!")))
            .build();

        // Set up handler mapping
        ReactiveHandlerMapping handlerMapping = new ReactiveHandlerMapping();
        handlerMapping.registerController(new TestController());

        // Create dispatcher
        DispatcherHandler dispatcher = new DispatcherHandler(routerFunction, handlerMapping);

        // Create test request
        ReactiveServerRequest request = new ReactiveServerRequest(
            io.netty.handler.codec.http.HttpMethod.GET,
            java.net.URI.create("/api/hello"),
            new io.netty.handler.codec.http.DefaultHttpHeaders(),
            Mono.empty()
        );

        // Test that annotation handler is used when no router function match
        StepVerifier.create(dispatcher.handle(request))
            .assertNext(response -> {
                assertEquals(HttpResponseStatus.OK, response.getStatus());
                assertEquals("Hello from @RestController!", response.getBody());
            })
            .verifyComplete();
    }

    @Test
    public void test404_WhenNoHandlerFound() {
        // Set up router function with a specific path
        RouterFunction routerFunction = RouterFunctionBuilder.route()
            .GET("/api/exists", request -> 
                Mono.just(ReactiveServerResponse.ok().body("Hello!")))
            .build();

        // Set up handler mapping with our test controller
        ReactiveHandlerMapping handlerMapping = new ReactiveHandlerMapping();
        handlerMapping.registerController(new TestController());

        // Create dispatcher
        DispatcherHandler dispatcher = new DispatcherHandler(routerFunction, handlerMapping);

        // Create test request for non-existent path
        ReactiveServerRequest request = new ReactiveServerRequest(
            io.netty.handler.codec.http.HttpMethod.GET,
            java.net.URI.create("/non/existent/path"),
            new io.netty.handler.codec.http.DefaultHttpHeaders(),
            Mono.empty()
        );

        // Test that 404 is returned when no handler is found
        StepVerifier.create(dispatcher.handle(request))
            .assertNext(response -> {
                assertEquals(HttpResponseStatus.NOT_FOUND, response.getStatus());
                assertEquals("Not Found", response.getBody());
            })
            .verifyComplete();
    }
} 