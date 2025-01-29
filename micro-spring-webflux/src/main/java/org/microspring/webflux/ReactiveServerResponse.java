package org.microspring.webflux;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import reactor.core.publisher.Mono;

/**
 * Represents a server-side HTTP response
 */
public class ReactiveServerResponse {
    private static final ObjectMapper objectMapper = new ObjectMapper();
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

    /**
     * Write an object as JSON response
     * @param obj The object to be written as JSON
     * @return this response
     */
    public ReactiveServerResponse writeJson(Object obj) {
        try {
            this.body = objectMapper.writeValueAsString(obj);
            this.headers.set(HttpHeaderNames.CONTENT_TYPE, "application/json");
            return this;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
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