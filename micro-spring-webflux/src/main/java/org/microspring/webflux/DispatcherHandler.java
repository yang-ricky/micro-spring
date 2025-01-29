package org.microspring.webflux;

import reactor.core.publisher.Mono;

/**
 * Central dispatcher for HTTP request handlers/controllers.
 * Dispatches to registered handlers based on priority.
 */
public class DispatcherHandler {
    private final RouterFunction routerFunction;
    private final ReactiveHandlerMapping handlerMapping;

    public DispatcherHandler(RouterFunction routerFunction, ReactiveHandlerMapping handlerMapping) {
        this.routerFunction = routerFunction;
        this.handlerMapping = handlerMapping;
    }

    /**
     * Handle the request by trying router function first, then annotation-based handlers
     */
    public Mono<ReactiveServerResponse> handle(ReactiveServerRequest request) {
        // First try router function
        return routerFunction.route(request)
            .flatMap(handler -> handler.handle(request))
            .switchIfEmpty(
                // If no route found, try annotation-based handlers
                Mono.defer(() -> {
                    try {
                        HandlerMethod handler = handlerMapping.getHandler(
                            request.getUri().getPath(),
                            request.getMethod()
                        );
                        if (handler != null) {
                            return handler.invoke(request)
                                .map(result -> {
                                    if (result instanceof ReactiveServerResponse) {
                                        return (ReactiveServerResponse) result;
                                    } else {
                                        return ReactiveServerResponse.ok().body(result.toString());
                                    }
                                });
                        }
                    } catch (Exception e) {
                        return Mono.error(e);
                    }
                    return Mono.empty();
                })
            )
            .switchIfEmpty(
                // If still no handler found, return 404
                Mono.just(new ReactiveServerResponse()
                    .status(io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND)
                    .body("Not Found"))
            );
    }
} 