package org.microspring.webflux;

import reactor.core.publisher.Mono;

/**
 * Chain of responsibility for WebFilter instances
 */
@FunctionalInterface
public interface WebFilterChain {
    /**
     * Continue the filter chain by invoking the next filter
     *
     * @param request The request being processed
     * @param response The response being built
     * @return Mono<Void> completion signal
     */
    Mono<Void> filter(ReactiveServerRequest request, ReactiveServerResponse response);
} 