package org.microspring.webflux;

import reactor.core.publisher.Mono;

/**
 * Functional interface for handling HTTP requests
 */
@FunctionalInterface
public interface HandlerFunction {
    /**
     * Handle the given request.
     *
     * @param request the request to handle
     * @return the response Mono
     */
    Mono<ReactiveServerResponse> handle(ReactiveServerRequest request);
} 