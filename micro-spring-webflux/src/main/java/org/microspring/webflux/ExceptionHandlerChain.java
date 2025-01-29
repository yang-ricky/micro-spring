package org.microspring.webflux;

import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Chain of responsibility for exception handlers
 */
public class ExceptionHandlerChain {
    private final List<WebExceptionHandler> handlers;

    public ExceptionHandlerChain(List<WebExceptionHandler> handlers) {
        this.handlers = handlers;
    }

    /**
     * Handle exception through the chain of handlers
     */
    public Mono<Void> handle(ReactiveServerRequest request, ReactiveServerResponse response, Throwable ex) {
        if (handlers.isEmpty()) {
            return Mono.error(ex);
        }

        // Try each handler in sequence
        Mono<Void> result = Mono.error(ex);
        for (WebExceptionHandler handler : handlers) {
            result = result.onErrorResume(t -> handler.handle(request, response, ex));
        }
        return result;
    }
} 