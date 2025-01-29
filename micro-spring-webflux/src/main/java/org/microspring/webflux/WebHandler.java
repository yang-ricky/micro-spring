package org.microspring.webflux;

import reactor.core.publisher.Mono;

/**
 * Handler interface for processing web requests after filters
 */
@FunctionalInterface
public interface WebHandler {
    /**
     * Handle the web request
     */
    Mono<Void> handle(ReactiveServerRequest request, ReactiveServerResponse response);
} 