package org.microspring.webflux;

import io.netty.handler.codec.http.HttpMethod;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Router function that routes requests to handler functions
 */
public class RouterFunction {
    private final Map<RouteKey, HandlerFunction> routes = new HashMap<>();

    /**
     * Register a route with method, path and handler
     */
    void register(HttpMethod method, String path, HandlerFunction handler) {
        RouteKey key = new RouteKey(method, path);
        if (routes.containsKey(key)) {
            throw new IllegalStateException(
                String.format("Duplicate route found: %s %s", method, path));
        }
        routes.put(key, handler);
    }

    /**
     * Route a request to its handler
     */
    public Mono<HandlerFunction> route(ReactiveServerRequest request) {
        RouteKey key = new RouteKey(
            request.getMethod(),
            request.getUri().getPath()
        );
        HandlerFunction handler = routes.get(key);
        return handler != null ? Mono.just(handler) : Mono.empty();
    }

    private static class RouteKey {
        private final HttpMethod method;
        private final String path;

        RouteKey(HttpMethod method, String path) {
            this.method = method;
            this.path = path;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RouteKey routeKey = (RouteKey) o;
            return Objects.equals(method.name(), routeKey.method.name()) && 
                   Objects.equals(path, routeKey.path);
        }

        @Override
        public int hashCode() {
            return Objects.hash(method.name(), path);
        }
    }
} 