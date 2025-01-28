package org.microspring.webflux;

import reactor.core.publisher.Mono;

/**
 * Core interface for handling HTTP requests in a reactive way
 */
public interface ReactiveHttpHandler {
    /**
     * Handle the HTTP request and return a Mono that completes when the response is fully written
     *
     * @param request The incoming HTTP request
     * @param response The response to write to
     * @return A Mono that completes when the response is fully written
     */
    Mono<Void> handle(ReactiveServerRequest request, ReactiveServerResponse response);
} 