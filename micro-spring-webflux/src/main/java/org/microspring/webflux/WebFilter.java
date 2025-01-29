package org.microspring.webflux;

import reactor.core.publisher.Mono;

/**
 * Filter interface for processing web requests.
 * Can be used for logging, authentication, etc.
 */
@FunctionalInterface
public interface WebFilter {
    /**
     * Process the web request and invoke the next filter in the chain
     *
     * @param request The incoming request
     * @param response The server response
     * @param chain The filter chain to delegate to
     * @return Mono<Void> completion signal
     */
    Mono<Void> filter(ReactiveServerRequest request, ReactiveServerResponse response, WebFilterChain chain);
} 