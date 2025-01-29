package org.microspring.webflux;

import io.netty.handler.codec.http.HttpMethod;
import reactor.core.publisher.Mono;

/**
 * DSL for building router functions in a fluent way.
 * Example usage:
 * RouterFunction router = RouterFunctionBuilder.route()
 *     .path("/api")
 *         .GET("/hello", req -> Mono.just("Hello!"))
 *         .POST("/echo", req -> req.getBody().map(body -> "Echo: " + body))
 *     .path("/admin")
 *         .GET("/status", req -> Mono.just("OK"))
 *     .build();
 */
public class RouterFunctionBuilder {
    private final RouterFunction routerFunction = new RouterFunction();
    private String basePath = "";

    private RouterFunctionBuilder() {}

    /**
     * Start building a router function
     */
    public static RouterFunctionBuilder route() {
        return new RouterFunctionBuilder();
    }

    /**
     * Set base path for subsequent route definitions
     */
    public RouterFunctionBuilder path(String path) {
        this.basePath = path;
        return this;
    }

    /**
     * Add a GET route
     */
    public RouterFunctionBuilder GET(String path, HandlerFunction handler) {
        routerFunction.register(HttpMethod.GET, buildPath(path), handler);
        return this;
    }

    /**
     * Add a POST route
     */
    public RouterFunctionBuilder POST(String path, HandlerFunction handler) {
        routerFunction.register(HttpMethod.POST, buildPath(path), handler);
        return this;
    }

    /**
     * Add a PUT route
     */
    public RouterFunctionBuilder PUT(String path, HandlerFunction handler) {
        routerFunction.register(HttpMethod.PUT, buildPath(path), handler);
        return this;
    }

    /**
     * Add a DELETE route
     */
    public RouterFunctionBuilder DELETE(String path, HandlerFunction handler) {
        routerFunction.register(HttpMethod.DELETE, buildPath(path), handler);
        return this;
    }

    /**
     * Add a route with custom HTTP method
     */
    public RouterFunctionBuilder method(HttpMethod method, String path, HandlerFunction handler) {
        routerFunction.register(method, buildPath(path), handler);
        return this;
    }

    /**
     * Build the router function
     */
    public RouterFunction build() {
        return routerFunction;
    }

    private String buildPath(String path) {
        if (basePath.isEmpty()) {
            return path;
        }
        // Handle path joining with proper slashes
        String normalizedBase = basePath.endsWith("/") ? basePath.substring(0, basePath.length() - 1) : basePath;
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return normalizedBase + normalizedPath;
    }
} 