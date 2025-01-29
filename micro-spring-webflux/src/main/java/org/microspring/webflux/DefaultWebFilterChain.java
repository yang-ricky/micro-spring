package org.microspring.webflux;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Default implementation of WebFilterChain that maintains a list of filters
 */
public class DefaultWebFilterChain implements WebFilterChain {
    private final List<WebFilter> filters;
    private final AtomicInteger index = new AtomicInteger(0);
    private final WebHandler handler;

    public DefaultWebFilterChain(List<WebFilter> filters, WebHandler handler) {
        this.filters = filters;
        this.handler = handler;
    }

    @Override
    public Mono<Void> filter(ReactiveServerRequest request, ReactiveServerResponse response) {
        return Mono.defer(() -> {
            if (index.get() < filters.size()) {
                WebFilter filter = filters.get(index.getAndIncrement());
                return filter.filter(request, response, this);
            } else {
                return handler.handle(request, response);
            }
        });
    }

    /**
     * Create a new chain instance with the same filters but reset index
     */
    public DefaultWebFilterChain clone(WebHandler handler) {
        return new DefaultWebFilterChain(filters, handler);
    }
} 