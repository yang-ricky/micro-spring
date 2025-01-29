package org.microspring.webflux;

import io.netty.handler.codec.http.HttpMethod;

/**
 * Builder for RouterFunction with a fluent API
 */
public class RouterFunctionBuilder {
    private final RouterFunction routerFunction = new RouterFunction();

    private RouterFunctionBuilder() {}

    public static RouterFunctionBuilder route() {
        return new RouterFunctionBuilder();
    }

    /**
     * Add a GET route
     */
    public RouterFunctionBuilder GET(String path, HandlerFunction handler) {
        routerFunction.register(HttpMethod.GET, path, handler);
        return this;
    }

    /**
     * Add a POST route
     */
    public RouterFunctionBuilder POST(String path, HandlerFunction handler) {
        routerFunction.register(HttpMethod.POST, path, handler);
        return this;
    }

    /**
     * Add a PUT route
     */
    public RouterFunctionBuilder PUT(String path, HandlerFunction handler) {
        routerFunction.register(HttpMethod.PUT, path, handler);
        return this;
    }

    /**
     * Add a DELETE route
     */
    public RouterFunctionBuilder DELETE(String path, HandlerFunction handler) {
        routerFunction.register(HttpMethod.DELETE, path, handler);
        return this;
    }

    /**
     * Build the router function
     */
    public RouterFunction build() {
        return routerFunction;
    }
} 