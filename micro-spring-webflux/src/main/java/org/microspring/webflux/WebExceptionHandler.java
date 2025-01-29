package org.microspring.webflux;

import reactor.core.publisher.Mono;

/**
 * Handler for exceptions during request processing
 */
@FunctionalInterface
public interface WebExceptionHandler {
    /**
     * Handle the given exception
     * Return empty Mono to continue with other exception handlers
     *
     * @param request The request being processed
     * @param response The response being built
     * @param ex The exception that occurred
     * @return Mono<Void> completion signal
     */
    Mono<Void> handle(ReactiveServerRequest request, ReactiveServerResponse response, Throwable ex);
} 