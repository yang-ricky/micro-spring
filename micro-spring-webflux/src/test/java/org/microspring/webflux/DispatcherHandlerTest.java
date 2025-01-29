package org.microspring.webflux;

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
    }

    @Test
    public void testRoutingPriority() {
        // Set up router function
        RouterFunction routerFunction = RouterFunctionBuilder.route()
            .GET("/api/hello", request -> 
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

        // Test that router function takes priority
        StepVerifier.create(dispatcher.handle(request))
            .assertNext(response -> {
                assertEquals("Hello from RouterFunction!", response.getBody());
            })
            .verifyComplete();
    }

    @Test
    public void testFallbackToAnnotation() {
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
                assertEquals("Hello from @RestController!", response.getBody());
            })
            .verifyComplete();
    }
} 