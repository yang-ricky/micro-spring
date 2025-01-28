package org.microspring.webflux;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;

/**
 * Represents a server-side HTTP response
 */
public class ReactiveServerResponse {
    private HttpResponseStatus status = HttpResponseStatus.OK;
    private final HttpHeaders headers;
    private String body;

    public ReactiveServerResponse() {
        this.headers = new io.netty.handler.codec.http.DefaultHttpHeaders();
    }

    public ReactiveServerResponse status(HttpResponseStatus status) {
        this.status = status;
        return this;
    }

    public ReactiveServerResponse header(String name, String value) {
        headers.set(name, value);
        return this;
    }

    public ReactiveServerResponse write(String data) {
        this.body = data;
        return this;
    }

    public Mono<Void> end() {
        // For now, just return a completed Mono
        // Later we'll implement actual writing to the Netty channel
        return Mono.empty();
    }

    public HttpResponseStatus getStatus() {
        return status;
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

    public String getBody() {
        return body;
    }
} 