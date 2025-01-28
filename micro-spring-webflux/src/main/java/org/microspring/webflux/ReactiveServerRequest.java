package org.microspring.webflux;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import reactor.core.publisher.Mono;

import java.net.URI;

/**
 * Represents a server-side HTTP request
 */
public class ReactiveServerRequest {
    private final HttpMethod method;
    private final URI uri;
    private final HttpHeaders headers;
    private final Mono<String> body;

    public ReactiveServerRequest(HttpMethod method, URI uri, HttpHeaders headers, Mono<String> body) {
        this.method = method;
        this.uri = uri;
        this.headers = headers;
        this.body = body;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public URI getUri() {
        return uri;
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

    public Mono<String> getBody() {
        return body;
    }
} 